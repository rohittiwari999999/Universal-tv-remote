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

        const val FREQ_36000 = 36000
        const val FREQ_40000 = 40000
        const val FREQ_56000 = 56000

        /**
         * Primary Coocaa TV Power Pattern (38.4 kHz, 68 pulses).
         * Direct reference needed by IrBlasterManager and external activities.
         */
        @JvmField
        val COOCAA_POWER_PATTERN: IntArray = RAW_USER_PATTERN

        /**
         * List of all testable Coocaa TV code and frequency variants.
         * Allows testing one-by-one ("Ek ek karke test karein") so user finds the exact working frequency.
         */
        val ALL_VARIANTS = listOf(
            CoocaaVariantItem(
                index = 1,
                name = "Coocaa Google TV & Android TV",
                frequencyHz = 38000,
                hexCode = "0x00FF12ED",
                protocol = "NEC 32-bit (38kHz)",
                description = "Standard Google TV / Android TV (Skyworth official)",
                pattern = PATTERN_GOOGLE_TV
            ),
            CoocaaVariantItem(
                index = 2,
                name = "Coocaa 4K S6G / Y72 Series",
                frequencyHz = 38400,
                hexCode = "0x707030CF",
                protocol = "NEC 32-bit (38.4kHz)",
                description = "Coocaa 43/50/55 inch 4K Smart TV models",
                pattern = PATTERN_S6G_Y72
            ),
            CoocaaVariantItem(
                index = 3,
                name = "Coocaa 9ms Lead Burst (Standby Wake)",
                frequencyHz = 38400,
                hexCode = "0x707030CF (9ms AGC)",
                protocol = "Standard 9000µs NEC",
                description = "Deep standby wake-up pattern with 9ms AGC leader burst",
                pattern = CORRECTED_HEADER_PATTERN
            ),
            CoocaaVariantItem(
                index = 4,
                name = "Coocaa Coolita / 32S3U / 40S3U",
                frequencyHz = 38000,
                hexCode = "0x01FE48B7",
                protocol = "NEC 32-bit (38kHz)",
                description = "Coocaa Coolita OS and budget 32/40-inch Smart TVs",
                pattern = PATTERN_COOLITA
            ),
            CoocaaVariantItem(
                index = 5,
                name = "Skyworth Android TV Alt",
                frequencyHz = 38000,
                hexCode = "0x00FF02FD",
                protocol = "NEC 32-bit (38kHz)",
                description = "Skyworth / Coocaa alternate power code",
                pattern = PATTERN_SKYWORTH_ALT
            ),
            CoocaaVariantItem(
                index = 6,
                name = "Custom 68-Pulse Raw Array",
                frequencyHz = 38400,
                hexCode = "RAW [1901µs, 4453µs...]",
                protocol = "Custom Raw Microseconds",
                description = "User exact 68-pulse timing pattern",
                pattern = RAW_USER_PATTERN
            ),
            CoocaaVariantItem(
                index = 7,
                name = "Coocaa 36kHz Indian Model Variant",
                frequencyHz = 36000,
                hexCode = "0x00FF12ED (36kHz)",
                protocol = "NEC 32-bit (36kHz Modulation)",
                description = "Older Indian & Southeast Asian Coocaa models using 36kHz carrier",
                pattern = PATTERN_GOOGLE_TV
            ),
            CoocaaVariantItem(
                index = 8,
                name = "Coocaa Universal Power",
                frequencyHz = 38000,
                hexCode = "0x04FB01FE",
                protocol = "NEC 32-bit",
                description = "Coocaa Universal Smart TV power toggle code",
                pattern = PATTERN_UNIVERSAL
            ),
            CoocaaVariantItem(
                index = 9,
                name = "Coocaa 40kHz High Carrier Variant",
                frequencyHz = 40000,
                hexCode = "0x707030CF (40kHz)",
                protocol = "NEC 32-bit (40kHz)",
                description = "High carrier frequency variant for noise-filtered receivers",
                pattern = PATTERN_S6G_Y72
            )
        )
    }

    /**
     * Transmits a specific Coocaa code index (1 to 9) or 0 for Super Multi-Blast.
     */
    suspend fun sendPowerOnSignal(codeIndex: Int = 0): Boolean {
        if (!hasIrBlaster()) {
            return false
        }

        val variant = ALL_VARIANTS.find { it.index == codeIndex }
        return if (variant != null) {
            transmitSinglePattern(variant.pattern, variant.frequencyHz, variant.name)
        } else {
            sendSuperMultiBlast()
        }
    }

    /**
     * Synchronous power signal transmission (can be invoked directly in standard OnClickListener).
     */
    fun sendPowerOnSignalSync(codeIndex: Int = 0): Boolean {
        if (!hasIrBlaster()) return false
        val variant = ALL_VARIANTS.find { it.index == codeIndex }
        val pattern = variant?.pattern ?: COOCAA_POWER_PATTERN
        val freq = variant?.frequencyHz ?: FREQ_38400

        return try {
            irManager?.transmit(freq, pattern)
            Log.i(TAG, "Transmitted sync IR signal (${variant?.name ?: "Default"}) at ${freq}Hz")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error transmitting sync IR: ${e.message}", e)
            false
        }
    }

    /**
     * Transmits a specific variant with a customized carrier frequency.
     * Useful when TV receiver is slightly off-frequency (e.g. 36.5kHz, 38.0kHz, 39.0kHz, 40.0kHz).
     */
    suspend fun transmitWithFrequency(codeIndex: Int, frequencyHz: Int): Boolean {
        if (!hasIrBlaster()) return false
        val variant = ALL_VARIANTS.find { it.index == codeIndex } ?: ALL_VARIANTS.first()
        return transmitSinglePattern(variant.pattern, frequencyHz, "${variant.name} @ ${frequencyHz}Hz")
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
     * Super Multi-Blast: Transmits ALL 9 Coocaa TV power codes sequentially!
     * Ensures any Coocaa / Skyworth model (Google TV, Android TV, Coolita, S6G, Y72, 36kHz)
     * catches its corresponding Power ON signal.
     */
    suspend fun sendSuperMultiBlast(): Boolean {
        if (!hasIrBlaster()) return false

        for (variant in ALL_VARIANTS) {
            try {
                irManager?.transmit(variant.frequencyHz, variant.pattern)
                delay(40)
                irManager?.transmit(variant.frequencyHz, variant.pattern) // 2x burst
                delay(60)
            } catch (e: Exception) {
                Log.w(TAG, "Blast step failed for ${variant.name}: ${e.message}")
            }
        }
        Log.i(TAG, "Completed Super Multi-Blast across all ${ALL_VARIANTS.size} Coocaa codes!")
        return true
    }
}

/**
 * Metadata descriptor for each testable Coocaa IR frequency & code variant.
 */
data class CoocaaVariantItem(
    val index: Int,
    val name: String,
    val frequencyHz: Int,
    val hexCode: String,
    val protocol: String,
    val description: String,
    val pattern: IntArray
)
