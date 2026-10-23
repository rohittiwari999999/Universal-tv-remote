package com.omnitech.googletv.remote.ir

import android.content.Context
import android.hardware.ConsumerIrManager
import android.util.Log
import kotlinx.coroutines.delay

/**
 * Metadata descriptor for each testable Coocaa IR frequency & code variant.
 * Declared at top-level to guarantee zero forward-reference or unresolved symbol issues.
 */
data class CoocaaVariantItem(
    val index: Int,
    val name: String,
    val frequencyHz: Int,
    val hexCode: String,
    val protocol: String,
    val description: String,
    val pattern: IntArray,
    val isRecommended: Boolean = false
)

/**
 * Advanced Coocaa & Skyworth TV IR Blaster Controller.
 * Tuned specifically for iQOO Neo 10 / Vivo Smart Remote HAL and standby wake-up.
 *
 * Why iQOO Neo 10 custom remote worked:
 * 1. Skyworth Preset #1 OEM Factory Code is NEC 0x40BF12ED (Address 0x40, Inverted 0xBF, Command 0x12).
 * 2. Secondary Vivo / Skyworth code is 0x40BF02FD and 0x47EF827D.
 * 3. Most remotes and TV firmware respond reliably to a clean 67-pulse single-frame or 2-frame standby burst.
 * 4. Arrays must end with a 560µs stop mark (odd length) as required by Vivo/iQOO IR HAL.
 * 5. Inter-command delay between different test codes must be >= 400ms so TV TSOP AGC resets.
 */
class CoocaaIrController(private val context: Context) {

    private val TAG = "CoocaaIrController"
    private val PREFS_NAME = "coocaa_ir_prefs"
    private val KEY_SELECTED_VARIANT = "selected_coocaa_variant_index"

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

    /**
     * Gets the currently active / saved Coocaa variant index (Default: 1 = iQOO Neo 10 Match 0x40BF12ED 67p).
     */
    fun getSelectedVariantIndex(): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getInt(KEY_SELECTED_VARIANT, 1)
    }

    /**
     * Saves the chosen working variant index as the persistent default for POWER ON button.
     */
    fun setSelectedVariantIndex(index: Int) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putInt(KEY_SELECTED_VARIANT, index).apply()
        Log.i(TAG, "Saved active Coocaa variant index: $index")
    }

    fun getSelectedVariant(): CoocaaVariantItem {
        val idx = getSelectedVariantIndex()
        return ALL_VARIANTS.find { it.index == idx } ?: ALL_VARIANTS.first()
    }

    companion object {
        const val FREQ_38000 = 38000
        const val FREQ_38400 = 38400
        const val FREQ_36000 = 36000
        const val FREQ_40000 = 40000

        /**
         * Builds an authentic standard NEC 32-bit timing pattern (LSB first per NEC standard).
         * Generates [repeatCount] complete 32-bit NEC frames separated by 40ms inter-frame gap.
         * Odd-length array (Mark -> Space -> Mark) strictly compliant with Vivo/iQOO IR HAL.
         */
        fun buildStandardNec(b0: Int, b1: Int, b2: Int, b3: Int, repeatCount: Int = 1): IntArray {
            val bytes = intArrayOf(b0 and 0xFF, b1 and 0xFF, b2 and 0xFF, b3 and 0xFF)
            val list = ArrayList<Int>(68 * repeatCount)

            for (frame in 0 until repeatCount) {
                // 9ms AGC burst mark, 4.5ms space
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
                // Stop bit mark (560µs)
                list.add(560)

                // Add 40ms inter-frame gap only BETWEEN frames (never at the very end)
                if (frame < repeatCount - 1) {
                    list.add(40000)
                }
            }
            return list.toIntArray()
        }

        /**
         * Builds an NEC 32-bit timing pattern using MSB-first bit order.
         * For remotes/devices whose databases store reversed bit streams.
         */
        fun buildNecMsb(b0: Int, b1: Int, b2: Int, b3: Int, repeatCount: Int = 1): IntArray {
            val bytes = intArrayOf(b0 and 0xFF, b1 and 0xFF, b2 and 0xFF, b3 and 0xFF)
            val list = ArrayList<Int>(68 * repeatCount)

            for (frame in 0 until repeatCount) {
                list.add(9000)
                list.add(4500)

                for (b in bytes) {
                    for (i in 7 downTo 0) {
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
                if (frame < repeatCount - 1) {
                    list.add(40000)
                }
            }
            return list.toIntArray()
        }

        // =========================================================================
        // CANDIDATE CODES FOR COOCAA / SKYWORTH (Starting with iQOO Neo 10 Presets)
        // =========================================================================

        // 1. iQOO Neo 10 / Vivo Remote Skyworth Preset #1 (OEM Standard: 0x40BF12ED) - Single Clean Frame (67 pulses)
        val PATTERN_40BF_12ED_SINGLE = buildStandardNec(0x40, 0xBF, 0x12, 0xED, 1)

        // 2. iQOO Neo 10 / Vivo Remote Skyworth Preset #1 - 2-Frame Standby Wake Burst (135 pulses)
        val PATTERN_40BF_12ED_DUAL = buildStandardNec(0x40, 0xBF, 0x12, 0xED, 2)

        // 3. iQOO Neo 10 / Vivo Remote Skyworth Preset #2 (0x40BF02FD) - Single Clean Frame (67 pulses)
        val PATTERN_40BF_02FD_SINGLE = buildStandardNec(0x40, 0xBF, 0x02, 0xFD, 1)

        // 4. iQOO Neo 10 / Vivo Remote Skyworth Preset #2 - 2-Frame Standby Wake Burst (135 pulses)
        val PATTERN_40BF_02FD_DUAL = buildStandardNec(0x40, 0xBF, 0x02, 0xFD, 2)

        // 5. iQOO Neo 10 Match #3: Skyworth Extended (0x47EF827D) - Single Frame (67 pulses)
        val PATTERN_47EF_827D_SINGLE = buildStandardNec(0x47, 0xEF, 0x82, 0x7D, 1)

        // 6. iQOO Neo 10 Match #3: Skyworth Extended - 2-Frame Burst
        val PATTERN_47EF_827D_DUAL = buildStandardNec(0x47, 0xEF, 0x82, 0x7D, 2)

        // 7. iQOO Neo 10 / Vivo Remote Skyworth Preset #4 (0x40BF48B7)
        val PATTERN_40BF_48B7 = buildStandardNec(0x40, 0xBF, 0x48, 0xB7, 1)

        // 8. Official Coocaa Google TV & Android TV (Standard 0x00FF12ED) - Single Frame
        val PATTERN_GOOGLE_TV_SINGLE = buildStandardNec(0x00, 0xFF, 0x12, 0xED, 1)

        // 9. Official Coocaa Google TV & Android TV - 2-Frame Standby Burst
        val PATTERN_GOOGLE_TV_DUAL = buildStandardNec(0x00, 0xFF, 0x12, 0xED, 2)

        // 10. Skyworth Android TV Alt (0x00FF02FD)
        val PATTERN_SKYWORTH_ALT_00FF02 = buildStandardNec(0x00, 0xFF, 0x02, 0xFD, 1)

        // 11. Coocaa Coolita / 32S3U / 40S3U (0x01FE48B7)
        val PATTERN_COOLITA = buildStandardNec(0x01, 0xFE, 0x48, 0xB7, 1)

        // 12. Coocaa Coolita Alt (0x00FE48B7)
        val PATTERN_COOLITA_00FE = buildStandardNec(0x00, 0xFE, 0x48, 0xB7, 1)

        // 13. Coocaa 4K S6G / Y72 Series (0x707030CF, 38.4kHz)
        val PATTERN_S6G_Y72 = buildStandardNec(0x70, 0x70, 0x30, 0xCF, 1)

        // 14. Coocaa Indian 36kHz Carrier Variant (0x40BF12ED)
        val PATTERN_INDIAN_36K = buildStandardNec(0x40, 0xBF, 0x12, 0xED, 1)

        // 15. Coocaa 40kHz High Carrier Variant (0x40BF12ED)
        val PATTERN_HIGH_40K = buildStandardNec(0x40, 0xBF, 0x12, 0xED, 1)

        // 16. Skyworth Discrete Power ON (0x40BF13EC)
        val PATTERN_DISCRETE_ON = buildStandardNec(0x40, 0xBF, 0x13, 0xEC, 1)

        // 17. Skyworth 4K Android 11/12 (0x00FF827D)
        val PATTERN_SKYWORTH_00FF82 = buildStandardNec(0x00, 0xFF, 0x82, 0x7D, 1)

        // 18. MSB-First Bitstream: Skyworth 0x40BF12ED
        val PATTERN_MSB_40BF = buildNecMsb(0x40, 0xBF, 0x12, 0xED, 1)

        // 19. MSB-First Bitstream: Skyworth 0x47EF827D
        val PATTERN_MSB_47EF = buildNecMsb(0x47, 0xEF, 0x82, 0x7D, 1)

        // 20. Coocaa Universal Smart Power (0x04FB01FE)
        val PATTERN_UNIVERSAL = buildStandardNec(0x04, 0xFB, 0x01, 0xFE, 1)

        /**
         * Primary Coocaa TV Power Pattern.
         * Default: Skyworth 0x40BF12ED Single Clean Frame (67 pulses, 38.0 kHz).
         */
        @JvmField
        val COOCAA_POWER_PATTERN: IntArray = PATTERN_40BF_12ED_SINGLE

        /**
         * List of all testable Coocaa TV code and frequency variants.
         * Allows testing one-by-one ("Ek ek karke test karein") directly on iQOO Neo 10.
         */
        val ALL_VARIANTS: List<CoocaaVariantItem> = listOf(
            CoocaaVariantItem(
                index = 1,
                name = "iQOO Neo 10 Model 1: Skyworth 0x40BF12ED (Single Frame)",
                frequencyHz = 38000,
                hexCode = "0x40BF12ED (67p)",
                protocol = "Standard NEC Single Frame (38.0 kHz)",
                description = "Standard 1-tap single frame sent by iQOO / Vivo Smart Remote for Coocaa (Address 0x40, Cmd 0x12)",
                pattern = PATTERN_40BF_12ED_SINGLE,
                isRecommended = true
            ),
            CoocaaVariantItem(
                index = 2,
                name = "iQOO Neo 10 Model 1: Skyworth 0x40BF12ED (Standby Wake Burst)",
                frequencyHz = 38000,
                hexCode = "0x40BF12ED (2-Frame)",
                protocol = "NEC 32-bit 2-Frame Burst (38.0 kHz)",
                description = "Dual frame with 40ms gap to wake TV MCU from deep eco standby mode",
                pattern = PATTERN_40BF_12ED_DUAL,
                isRecommended = true
            ),
            CoocaaVariantItem(
                index = 3,
                name = "iQOO Neo 10 Model 2: Skyworth 0x40BF02FD (Single Frame)",
                frequencyHz = 38000,
                hexCode = "0x40BF02FD (67p)",
                protocol = "Standard NEC Single Frame (38.0 kHz)",
                description = "Alternate standard Skyworth / Coocaa code in iQOO / Vivo database (Cmd 0x02)",
                pattern = PATTERN_40BF_02FD_SINGLE,
                isRecommended = true
            ),
            CoocaaVariantItem(
                index = 4,
                name = "iQOO Neo 10 Model 2: Skyworth 0x40BF02FD (Standby Burst)",
                frequencyHz = 38000,
                hexCode = "0x40BF02FD (2-Frame)",
                protocol = "NEC 32-bit 2-Frame Burst (38.0 kHz)",
                description = "Dual frame standby burst for Cmd 0x02",
                pattern = PATTERN_40BF_02FD_DUAL
            ),
            CoocaaVariantItem(
                index = 5,
                name = "iQOO Neo 10 Match 3: Skyworth 0x47EF827D (Single Frame)",
                frequencyHz = 38000,
                hexCode = "0x47EF827D (67p)",
                protocol = "Standard NEC Single Frame (38.0 kHz)",
                description = "Skyworth 4K & Smart Android TV variant (Customer 0x47EF, Power 0x82)",
                pattern = PATTERN_47EF_827D_SINGLE,
                isRecommended = true
            ),
            CoocaaVariantItem(
                index = 6,
                name = "iQOO Neo 10 Match 3: Skyworth 0x47EF827D (Standby Burst)",
                frequencyHz = 38000,
                hexCode = "0x47EF827D (2-Frame)",
                protocol = "NEC 32-bit 2-Frame Burst (38.0 kHz)",
                description = "Dual frame standby burst for 0x47EF827D",
                pattern = PATTERN_47EF_827D_DUAL
            ),
            CoocaaVariantItem(
                index = 7,
                name = "iQOO Neo 10 Model 4: Skyworth 0x40BF48B7",
                frequencyHz = 38000,
                hexCode = "0x40BF48B7",
                protocol = "NEC 32-bit (38.0 kHz)",
                description = "Skyworth chassis variant with Power keycode 0x48",
                pattern = PATTERN_40BF_48B7
            ),
            CoocaaVariantItem(
                index = 8,
                name = "Coocaa Google TV Standard: 0x00FF12ED (Single Frame)",
                frequencyHz = 38000,
                hexCode = "0x00FF12ED (67p)",
                protocol = "NEC 32-bit (38.0 kHz)",
                description = "Official Skyworth/Coocaa Google TV standard power code",
                pattern = PATTERN_GOOGLE_TV_SINGLE
            ),
            CoocaaVariantItem(
                index = 9,
                name = "Coocaa Google TV Standard: 0x00FF12ED (Standby Burst)",
                frequencyHz = 38000,
                hexCode = "0x00FF12ED (2-Frame)",
                protocol = "NEC 2-Frame Burst (38.0 kHz)",
                description = "Dual-frame burst for Google TV standard",
                pattern = PATTERN_GOOGLE_TV_DUAL
            ),
            CoocaaVariantItem(
                index = 10,
                name = "Skyworth Android TV Alt: 0x00FF02FD",
                frequencyHz = 38000,
                hexCode = "0x00FF02FD",
                protocol = "NEC 32-bit (38.0 kHz)",
                description = "Skyworth / Coocaa alternate power toggle code",
                pattern = PATTERN_SKYWORTH_ALT_00FF02
            ),
            CoocaaVariantItem(
                index = 11,
                name = "Coocaa Coolita / 32S3U: 0x01FE48B7",
                frequencyHz = 38000,
                hexCode = "0x01FE48B7",
                protocol = "NEC 32-bit (38.0 kHz)",
                description = "Coocaa Coolita OS and budget 32/40-inch Smart TVs",
                pattern = PATTERN_COOLITA
            ),
            CoocaaVariantItem(
                index = 12,
                name = "Coocaa Coolita Alt: 0x00FE48B7",
                frequencyHz = 38000,
                hexCode = "0x00FE48B7",
                protocol = "NEC 32-bit (38.0 kHz)",
                description = "Coocaa Coolita alternate address code",
                pattern = PATTERN_COOLITA_00FE
            ),
            CoocaaVariantItem(
                index = 13,
                name = "Coocaa 4K S6G / Y72: 0x707030CF (38.4kHz)",
                frequencyHz = 38400,
                hexCode = "0x707030CF",
                protocol = "NEC 32-bit (38.4 kHz Modulation)",
                description = "Coocaa 43/50/55 inch 4K Smart TV models with 38.4kHz carrier",
                pattern = PATTERN_S6G_Y72
            ),
            CoocaaVariantItem(
                index = 14,
                name = "Coocaa Indian 36kHz Model: 0x40BF12ED",
                frequencyHz = 36000,
                hexCode = "0x40BF12ED (36kHz)",
                protocol = "NEC 32-bit (36.0 kHz Modulation)",
                description = "Older Indian & Southeast Asian Coocaa models using 36kHz carrier",
                pattern = PATTERN_INDIAN_36K
            ),
            CoocaaVariantItem(
                index = 15,
                name = "Coocaa High Frequency 40kHz: 0x40BF12ED",
                frequencyHz = 40000,
                hexCode = "0x40BF12ED (40kHz)",
                protocol = "NEC 32-bit (40.0 kHz Modulation)",
                description = "Coocaa variant tuned to 40.0 kHz carrier frequency",
                pattern = PATTERN_HIGH_40K
            ),
            CoocaaVariantItem(
                index = 16,
                name = "Skyworth Discrete Power ON: 0x40BF13EC",
                frequencyHz = 38000,
                hexCode = "0x40BF13EC",
                protocol = "NEC 32-bit Discrete ON (38.0 kHz)",
                description = "Discrete ON code (only powers ON the TV, will not accidentally toggle OFF)",
                pattern = PATTERN_DISCRETE_ON
            ),
            CoocaaVariantItem(
                index = 17,
                name = "Skyworth 4K Android 11/12: 0x00FF827D",
                frequencyHz = 38000,
                hexCode = "0x00FF827D",
                protocol = "NEC 32-bit (38.0 kHz)",
                description = "Coocaa 4K Android 11/12 TV power key",
                pattern = PATTERN_SKYWORTH_00FF82
            ),
            CoocaaVariantItem(
                index = 18,
                name = "MSB-First Bitstream: Skyworth 0x40BF12ED",
                frequencyHz = 38000,
                hexCode = "0x40BF12ED (MSB)",
                protocol = "NEC 32-bit MSB-First (38.0 kHz)",
                description = "Alternative MSB-first bit order for 0x40BF12ED",
                pattern = PATTERN_MSB_40BF
            ),
            CoocaaVariantItem(
                index = 19,
                name = "MSB-First Bitstream: Skyworth 0x47EF827D",
                frequencyHz = 38000,
                hexCode = "0x47EF827D (MSB)",
                protocol = "NEC 32-bit MSB-First (38.0 kHz)",
                description = "Alternative MSB-first bit order for 0x47EF827D",
                pattern = PATTERN_MSB_47EF
            ),
            CoocaaVariantItem(
                index = 20,
                name = "Coocaa Universal Smart: 0x04FB01FE",
                frequencyHz = 38000,
                hexCode = "0x04FB01FE",
                protocol = "NEC 32-bit (38.0 kHz)",
                description = "Coocaa Universal Smart TV power toggle code",
                pattern = PATTERN_UNIVERSAL
            )
        )
    }

    /**
     * Transmits the currently saved / chosen Coocaa variant.
     * Guaranteed clean single transmission with no collisions.
     */
    suspend fun sendSavedPowerSignal(): Boolean {
        if (!hasIrBlaster()) return false
        val variant = getSelectedVariant()
        return transmitSinglePattern(variant.pattern, variant.frequencyHz, variant.name)
    }

    /**
     * Transmits a specific Coocaa code index (1 to 20).
     */
    suspend fun sendPowerOnSignal(codeIndex: Int): Boolean {
        if (!hasIrBlaster()) return false
        val variant = ALL_VARIANTS.find { it.index == codeIndex } ?: getSelectedVariant()
        return transmitSinglePattern(variant.pattern, variant.frequencyHz, variant.name)
    }

    /**
     * Synchronous power signal transmission (can be invoked directly in standard OnClickListener).
     */
    fun sendPowerOnSignalSync(codeIndex: Int = 0): Boolean {
        if (!hasIrBlaster()) return false
        val variant = if (codeIndex > 0) {
            ALL_VARIANTS.find { it.index == codeIndex } ?: getSelectedVariant()
        } else {
            getSelectedVariant()
        }
        val pattern = variant.pattern
        val freq = variant.frequencyHz

        return try {
            irManager?.transmit(freq, pattern)
            Log.i(TAG, "Transmitted sync IR signal (${variant.name}) at ${freq}Hz")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error transmitting sync IR: ${e.message}", e)
            false
        }
    }

    /**
     * Transmits a specific variant with a customized carrier frequency.
     */
    suspend fun transmitWithFrequency(codeIndex: Int, frequencyHz: Int): Boolean {
        if (!hasIrBlaster()) return false
        val variant = ALL_VARIANTS.find { it.index == codeIndex } ?: getSelectedVariant()
        return transmitSinglePattern(variant.pattern, frequencyHz, "${variant.name} @ ${frequencyHz}Hz")
    }

    /**
     * Transmits a single pattern buffer without collision.
     */
    private suspend fun transmitSinglePattern(pattern: IntArray, freq: Int, label: String): Boolean {
        return try {
            irManager?.transmit(freq, pattern)
            Log.i(TAG, "Successfully transmitted $label at ${freq}Hz")
            delay(150)
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error transmitting $label: ${e.message}", e)
            false
        }
    }

    /**
     * Sequential Sweep Test across top 5 candidate codes with clean 450ms gaps.
     * Prevents signal truncation and gives TV TSOP demodulator time to reset AGC.
     */
    suspend fun sendCleanSequenceBlast(): Boolean {
        if (!hasIrBlaster()) return false

        // Top 5 priority candidates (Single-frame and standby burst)
        val priorityIndices = listOf(1, 2, 3, 5, 8)
        for (index in priorityIndices) {
            val variant = ALL_VARIANTS.find { it.index == index } ?: continue
            try {
                irManager?.transmit(variant.frequencyHz, variant.pattern)
                Log.i(TAG, "Sequence step fired: ${variant.name} (${variant.frequencyHz}Hz)")
                // 450ms gap is essential: 70ms transmission + 380ms quiet time for TV IR sensor AGC reset
                delay(450)
            } catch (e: Exception) {
                Log.w(TAG, "Sequence step failed for ${variant.name}: ${e.message}")
            }
        }
        Log.i(TAG, "Completed Clean Sequence Blast across top Coocaa codes!")
        return true
    }
}
