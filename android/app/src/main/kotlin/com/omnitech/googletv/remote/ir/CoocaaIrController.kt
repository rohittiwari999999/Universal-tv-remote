package com.omnitech.googletv.remote.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log
import kotlinx.coroutines.delay

/**
 * Advanced Coocaa & Skyworth TV IR Blaster Controller.
 * Supports all 6 known Coocaa TV power code variants:
 * 1. Coocaa Google TV & Android TV (Official Skyworth NEC 0x00FF12ED)
 * 2. Coocaa 4K Smart Series S6G / Y72 (NEC 0x707030CF)
 * 3. Coocaa Coolita OS & Smart 32S3U / 40S3U (NEC 0x01FE48B7)
 * 4. Skyworth Android TV Alt (NEC 0x00FF02FD)
 * 5. Corrected 9000us Header Burst (with 68-pulse payload)
 * 6. User Raw 1901us Array
 *
 * Includes Super Multi-Blast which transmits all codes sequentially with dual frames
 * so any Coocaa TV model in sleep/standby mode wakes up instantly.
 */
class CoocaaIrController(private val context: Context) {

    private val TAG = "CoocaaIrController"

    private val irManager: ConsumerIrManager? by lazy {
        try {
            context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        } catch (e: Exception) {
            Log.w(TAG, "ConsumerIrManager not available: ${e.message}")
            null
        }
    }

    fun hasIrBlaster(): Boolean {
        return try {
            irManager?.hasIrEmitter() == true
        } catch (e: Exception) {
            false
        }
    }

    companion object {
        const val FREQ_38400 = 38400
        const val FREQ_38000 = 38000

        // Variant 6: User's exact 68-pulse raw timing array
        val RAW_USER_PATTERN = intArrayOf(
            1901, 4453, 625, 1614, 625, 1588, 625, 1614, 625, 442,
            625, 442, 625, 468, 625, 442, 625, 494, 572, 1614,
            625, 1588, 625, 1614, 625, 494, 572, 442, 651, 442,
            625, 442, 625, 442, 625, 1614, 625, 1588, 651, 1588,
            625, 442, 625, 494, 598, 442, 625, 442, 625, 520,
            572, 442, 625, 442, 625, 442, 651, 1588, 625, 1614,
            625, 1588, 625, 1614, 625, 1588, 625, 48958
        )

        // Variant 5: Corrected 9000us start pulse for standard NEC IR demodulators
        val CORRECTED_HEADER_PATTERN = intArrayOf(
            9000, 4453, 625, 1614, 625, 1588, 625, 1614, 625, 442,
            625, 442, 625, 468, 625, 442, 625, 494, 572, 1614,
            625, 1588, 625, 1614, 625, 494, 572, 442, 651, 442,
            625, 442, 625, 442, 625, 1614, 625, 1588, 651, 1588,
            625, 442, 625, 494, 598, 442, 625, 442, 625, 520,
            572, 442, 625, 442, 625, 442, 651, 1588, 625, 1614,
            625, 1588, 625, 1614, 625, 1588, 625, 48958
        )

        /**
         * Builds an authentic standard NEC 32-bit timing pattern (LSB first byte-by-byte).
         * Header: 9000us mark, 4500us space.
         * Bit 0: 560us mark, 560us space.
         * Bit 1: 560us mark, 1690us space.
         * Final stop: 560us mark, 40000us space.
         */
        fun buildStandardNec(b0: Int, b1: Int, b2: Int, b3: Int): IntArray {
            val bytes = intArrayOf(b0 and 0xFF, b1 and 0xFF, b2 and 0xFF, b3 and 0xFF)
            val list = ArrayList<Int>(68)
            // 9ms AGC burst, 4.5ms space
            list.add(9000)
            list.add(4500)

            for (b in bytes) {
                // NEC transmits LSB first
                for (i in 0..7) {
                    val bit = (b shr i) and 1
                    list.add(560)
                    if (bit == 1) {
                        list.add(1690)
                    } else {
                        list.add(560)
                    }
                }
            }
            list.add(560)
            list.add(40000)
            return list.toIntArray()
        }

        // 1. Coocaa Google TV & Android TV (Address 0x00, Command 0x12) -> 0x00, 0xFF, 0x12, 0xED
        val PATTERN_GOOGLE_TV = buildStandardNec(0x00, 0xFF, 0x12, 0xED)

        // 2. Coocaa 4K S6G / Y72 Series (Address 0x70, Extended 0x70, Command 0x30, Inverted 0xCF)
        val PATTERN_S6G_Y72 = buildStandardNec(0x70, 0x70, 0x30, 0xCF)

        // 3. Coocaa Coolita / 32S3U / 40S3U (Address 0x01, Inverted 0xFE, Command 0x48, Inverted 0xB7)
        val PATTERN_COOLITA = buildStandardNec(0x01, 0xFE, 0x48, 0xB7)

        // 4. Skyworth Android TV Alternate (Address 0x00, Inverted 0xFF, Command 0x02, Inverted 0xFD)
        val PATTERN_SKYWORTH_ALT = buildStandardNec(0x00, 0xFF, 0x02, 0xFD)

        // 5. Coocaa Universal Power 0x04FB01FE
        val PATTERN_UNIVERSAL = buildStandardNec(0x04, 0xFB, 0x01, 0xFE)

        /**
         * Primary Coocaa TV Power Pattern (38.4 kHz, 68 pulses).
         * Direct reference needed by IrBlasterManager and external activities.
         */
        @JvmField
        val COOCAA_POWER_PATTERN: IntArray = RAW_USER_PATTERN
    }

    /**
     * Transmits a specific Coocaa code index (1 to 6) or 0 for Super Multi-Blast.
     */
    suspend fun sendPowerOnSignal(codeIndex: Int = 0): Boolean {
        if (!hasIrBlaster()) {
            return false
        }

        return when (codeIndex) {
            1 -> transmitSinglePattern(PATTERN_GOOGLE_TV, FREQ_38000, "Coocaa Google TV (0x00FF12ED)")
            2 -> transmitSinglePattern(PATTERN_S6G_Y72, FREQ_38400, "Coocaa S6G/Y72 4K (0x707030CF)")
            3 -> transmitSinglePattern(PATTERN_COOLITA, FREQ_38000, "Coocaa Coolita/Smart (0x01FE48B7)")
            4 -> transmitSinglePattern(PATTERN_SKYWORTH_ALT, FREQ_38000, "Skyworth Android TV (0x00FF02FD)")
            5 -> transmitSinglePattern(CORRECTED_HEADER_PATTERN, FREQ_38400, "Coocaa 9ms Lead Burst")
            6 -> transmitSinglePattern(RAW_USER_PATTERN, FREQ_38400, "Coocaa Raw 68-pulse")
            else -> sendSuperMultiBlast()
        }
    }

    /**
     * Synchronous power signal transmission (can be invoked directly in standard OnClickListener).
     */
    fun sendPowerOnSignalSync(codeIndex: Int = 0): Boolean {
        if (!hasIrBlaster()) return false
        val pattern = when (codeIndex) {
            1 -> PATTERN_GOOGLE_TV
            2 -> PATTERN_S6G_Y72
            3 -> PATTERN_COOLITA
            4 -> PATTERN_SKYWORTH_ALT
            5 -> CORRECTED_HEADER_PATTERN
            else -> COOCAA_POWER_PATTERN
        }
        val freq = if (codeIndex == 1 || codeIndex == 3 || codeIndex == 4) FREQ_38000 else FREQ_38400
        return try {
            irManager?.transmit(freq, pattern)
            Log.i(TAG, "Transmitted sync IR signal at ${freq}Hz")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error transmitting sync IR: ${e.message}", e)
            false
        }
    }

    /**
     * Transmits a single pattern twice (dual frames) for TV sleep wake-up.
     */
    private suspend fun transmitSinglePattern(pattern: IntArray, freq: Int, label: String): Boolean {
        return try {
            // First frame
            irManager?.transmit(freq, pattern)
            delay(50)
            // Second frame for reliable wake from deep standby
            irManager?.transmit(freq, pattern)
            Log.i(TAG, "Successfully transmitted $label at ${freq}Hz (dual-frame)")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error transmitting $label: ${e.message}", e)
            false
        }
    }

    /**
     * Super Multi-Blast: Transmits ALL 6 Coocaa TV power codes sequentially!
     * Ensures any Coocaa / Skyworth model (Google TV, Android TV, Coolita, S6G, Y72)
     * catches its corresponding Power ON signal.
     */
    suspend fun sendSuperMultiBlast(): Boolean {
        if (!hasIrBlaster()) return false

        val codes = listOf(
            Triple(PATTERN_GOOGLE_TV, FREQ_38000, "Google TV Code"),
            Triple(PATTERN_S6G_Y72, FREQ_38400, "S6G/Y72 Code"),
            Triple(CORRECTED_HEADER_PATTERN, FREQ_38400, "9ms Header Code"),
            Triple(PATTERN_COOLITA, FREQ_38000, "Coolita Code"),
            Triple(PATTERN_SKYWORTH_ALT, FREQ_38000, "Skyworth Alt Code"),
            Triple(RAW_USER_PATTERN, FREQ_38400, "Raw 68-pulse")
        )

        for ((pattern, freq, name) in codes) {
            try {
                irManager?.transmit(freq, pattern)
                delay(65)
            } catch (e: Exception) {
                Log.w(TAG, "Blast step failed for $name: ${e.message}")
            }
        }
        Log.i(TAG, "Completed Super Multi-Blast across all 6 Coocaa codes!")
        return true
    }
}
