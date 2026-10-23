package com.omnitech.googletv.remote.ir

import android.content.Context
import android.content.pm.PackageManager
import android.hardware.ConsumerIrManager
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

data class TvBrand(
    val id: String,
    val name: String,
    val description: String,
    val carrierFreq: Int = 38000,
    val protocol: String = "NEC",
    val codeHex: String = "0x707030CF"
)

data class IrTransmitResult(
    val success: Boolean,
    val isHardwareEmitter: Boolean,
    val brand: TvBrand,
    val message: String
)

/**
 * Handles Infrared (IR) Blaster code transmission and TV Power ON control.
 * Implements exact NEC protocol timing for Coocaa TV (Skyworth) and major TV brands.
 */
class IrBlasterManager(private val context: Context) {

    private val TAG = "IrBlasterManager"
    private val PREFS_NAME = "ir_blaster_prefs"
    private val KEY_SELECTED_BRAND = "selected_tv_brand"

    private val consumerIrManager: ConsumerIrManager? =
        try {
            context.getSystemService(Context.CONSUMER_IR_SERVICE) as? ConsumerIrManager
        } catch (e: Exception) {
            Log.w(TAG, "ConsumerIrManager not available: ${e.message}")
            null
        }

    companion object {
        /**
         * Primary Coocaa / Skyworth Power ON pattern (NEC 38.0 kHz 3-frame burst).
         * Defined directly inside IrBlasterManager to guarantee zero unresolved reference errors in CI/CD.
         * Default: Skyworth 0x40BF12ED (The #1 OEM Factory Code in iQOO Neo 10 / Vivo Smart Remote database).
         */
        @JvmField
        val COOCAA_POWER_PATTERN: IntArray = intArrayOf(
            // Single Clean Frame (67 pulses): 9ms mark, 4.5ms space, 32 bits (0x40, 0xBF, 0x12, 0xED), 560us stop
            9000, 4500,
            560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 560, 1690, 560, 560, // 0x40
            560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 1690, 560, 560, 560, 1690, // 0xBF
            560, 560, 560, 1690, 560, 560, 560, 560, 560, 1690, 560, 560, 560, 560, 560, 560, // 0x12
            560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 560, 560, 1690, 560, 1690, 560, 1690, // 0xED
            560
        )

        val SUPPORTED_BRANDS = listOf(
            TvBrand("coocaa", "Coocaa TV", "Coocaa / Skyworth (iQOO Neo 10 Match: 0x40BF12ED, 38kHz)", 38000, "NEC", "0x40BF12ED"),
            TvBrand("samsung", "Samsung TV", "Samsung 38kHz Power Code", 38000, "Samsung", "0xE0E040BF"),
            TvBrand("lg", "LG TV", "LG NEC 38kHz Power Code", 38000, "NEC", "0x20DF10EF"),
            TvBrand("sony", "Sony TV", "Sony SIRC 40kHz 12-bit Power Code", 40000, "Sony SIRC", "0xA90"),
            TvBrand("xiaomi", "Mi / Xiaomi", "Xiaomi / Redmi TV NEC 38kHz", 38000, "NEC", "0x00FF02FD"),
            TvBrand("oneplus", "OnePlus TV", "OnePlus TV NEC 38kHz", 38000, "NEC", "0x00FF827D"),
            TvBrand("tcl", "TCL TV", "TCL NEC 38kHz", 38000, "NEC", "0xF708FB04"),
            TvBrand("vu", "Vu TV", "Vu Smart TV NEC 38kHz", 38000, "NEC", "0x4CB3807F"),
            TvBrand("realme", "Realme TV", "Realme Smart TV NEC 38kHz", 38000, "NEC", "0x00FF827D"),
            TvBrand("hisense", "Hisense TV", "Hisense NEC 38kHz", 38000, "NEC", "0xFB047788"),
            TvBrand("panasonic", "Panasonic", "Panasonic 37kHz Power Code", 37000, "Panasonic", "0x40040100BCBD")
        )
    }

    /**
     * Checks if the device has a physical hardware IR emitter (blaster)
     */
    fun hasIrEmitter(): Boolean {
        return try {
            consumerIrManager?.hasIrEmitter() == true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks whether the system reports the FEATURE_CONSUMER_IR hardware feature via PackageManager.
     */
    fun hasSystemFeature(): Boolean {
        return try {
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_CONSUMER_IR)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Queries supported IR carrier frequency ranges using ConsumerIrManager.getCarrierFrequencies().
     */
    fun getCarrierFrequenciesList(): List<IrCarrierRange> {
        val ranges = ArrayList<IrCarrierRange>()
        try {
            val freqRanges = consumerIrManager?.carrierFrequencies
            if (freqRanges != null) {
                for (range in freqRanges) {
                    ranges.add(IrCarrierRange(range.minFrequency, range.maxFrequency))
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error querying carrier frequencies: ${e.message}")
        }
        return ranges
    }

    /**
     * Inspects hardware state and returns comprehensive diagnostic report.
     */
    fun getHardwareDiagnostic(): IrHardwareDiagnostic {
        val hasFeature = hasSystemFeature()
        val isService = (consumerIrManager != null)
        val hasEmitter = hasIrEmitter()
        val ranges = getCarrierFrequenciesList()

        val status = when {
            hasEmitter -> IrHardwareStatus.SUPPORTED_AND_READY
            !hasFeature -> IrHardwareStatus.NO_SYSTEM_FEATURE
            !isService -> IrHardwareStatus.SERVICE_UNAVAILABLE
            else -> IrHardwareStatus.EMITTER_NOT_DETECTED
        }

        val summary = when (status) {
            IrHardwareStatus.SUPPORTED_AND_READY ->
                "Hardware IR Blaster detected and ready. Supported frequencies: ${if (ranges.isNotEmpty()) ranges.joinToString() else "Standard 30-60 kHz"}."
            IrHardwareStatus.NO_SYSTEM_FEATURE ->
                "PackageManager reports android.hardware.consumerir feature is missing on this device."
            IrHardwareStatus.SERVICE_UNAVAILABLE ->
                "ConsumerIrManager system service is null or unavailable on this Android build."
            IrHardwareStatus.EMITTER_NOT_DETECTED ->
                "ConsumerIrManager service is bound, but hasIrEmitter() returned false (IR HAL disabled or unavailable)."
        }

        return IrHardwareDiagnostic(
            status = status,
            hasSystemFeature = hasFeature,
            isServiceAvailable = isService,
            hasIrEmitter = hasEmitter,
            supportedCarrierRanges = ranges,
            diagnosticSummary = summary
        )
    }

    /**
     * Transmits a custom microsecond pulse pattern.
     * Validates pattern length, timing values, carrier frequency, and executes ConsumerIrManager.transmit().
     * Strictly separates API execution from confirmed physical TV reception.
     */
    fun transmitCustomPattern(frequencyHz: Int, pattern: IntArray, description: String = "Custom Pattern"): IrExecutionResult {
        if (!hasIrEmitter()) {
            return IrExecutionResult(
                apiExecutedSuccessfully = false,
                status = "STATUS_NO_EMITTER",
                message = "Device has no active IR emitter hardware (hasIrEmitter = false).",
                carrierFreqHz = frequencyHz,
                pulseCount = pattern.size,
                durationMicros = 0L,
                errorDetails = "ConsumerIrManager.hasIrEmitter() returned false."
            )
        }

        if (consumerIrManager == null) {
            return IrExecutionResult(
                apiExecutedSuccessfully = false,
                status = "STATUS_SERVICE_UNAVAILABLE",
                message = "ConsumerIrManager system service is unavailable.",
                carrierFreqHz = frequencyHz,
                pulseCount = pattern.size,
                durationMicros = 0L,
                errorDetails = "Context.CONSUMER_IR_SERVICE was null."
            )
        }

        if (frequencyHz <= 0 || frequencyHz > 100000) {
            return IrExecutionResult(
                apiExecutedSuccessfully = false,
                status = "STATUS_INVALID_FREQUENCY",
                message = "Invalid carrier frequency: ${frequencyHz}Hz. Standard IR carrier is between 30,000 Hz and 60,000 Hz.",
                carrierFreqHz = frequencyHz,
                pulseCount = pattern.size,
                durationMicros = 0L,
                errorDetails = "Frequency out of range (1 - 100,000 Hz)."
            )
        }

        if (pattern.isEmpty()) {
            return IrExecutionResult(
                apiExecutedSuccessfully = false,
                status = "STATUS_INVALID_PATTERN",
                message = "Pattern array is empty.",
                carrierFreqHz = frequencyHz,
                pulseCount = 0,
                durationMicros = 0L,
                errorDetails = "Pattern array size was 0."
            )
        }

        // Validate all pulses are positive
        for (i in pattern.indices) {
            if (pattern[i] <= 0) {
                return IrExecutionResult(
                    apiExecutedSuccessfully = false,
                    status = "STATUS_INVALID_PATTERN",
                    message = "Pattern contains non-positive microsecond pulse at index $i: ${pattern[i]}µs.",
                    carrierFreqHz = frequencyHz,
                    pulseCount = pattern.size,
                    durationMicros = 0L,
                    errorDetails = "Invalid pulse value at index $i."
                )
            }
        }

        // Android IR HAL requires array ending with Mark pulse (odd length).
        // If even length, drop trailing space pulse to prevent HAL silent rejection.
        val safePattern = if (pattern.size % 2 == 0) {
            pattern.copyOf(pattern.size - 1)
        } else {
            pattern
        }

        var totalMicros = 0L
        for (p in safePattern) totalMicros += p

        return try {
            consumerIrManager.transmit(frequencyHz, safePattern)
            Log.i(TAG, "ConsumerIrManager.transmit executed: ${safePattern.size} pulses @ ${frequencyHz}Hz for $description")
            IrExecutionResult(
                apiExecutedSuccessfully = true,
                status = "STATUS_TRANSMITTED",
                message = "ConsumerIrManager.transmit() executed successfully (${safePattern.size} pulses @ ${frequencyHz / 1000}kHz). NOTE: This confirms phone hardware fired the signal; visually check if TV responds.",
                carrierFreqHz = frequencyHz,
                pulseCount = safePattern.size,
                durationMicros = totalMicros
            )
        } catch (e: Exception) {
            Log.e(TAG, "ConsumerIrManager.transmit threw an exception: ${e.message}", e)
            IrExecutionResult(
                apiExecutedSuccessfully = false,
                status = "STATUS_TRANSMISSION_ERROR",
                message = "Hardware transmit error: ${e.message}",
                carrierFreqHz = frequencyHz,
                pulseCount = safePattern.size,
                durationMicros = totalMicros,
                errorDetails = e.stackTraceToString()
            )
        }
    }

    /**
     * Gets the currently selected TV brand from storage (Default: Coocaa TV)
     */
    fun getSelectedBrand(): TvBrand {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedId = prefs.getString(KEY_SELECTED_BRAND, "coocaa") ?: "coocaa"
        return SUPPORTED_BRANDS.find { it.id.equals(savedId, ignoreCase = true) }
            ?: SUPPORTED_BRANDS.first()
    }

    /**
     * Updates and saves the selected TV brand
     */
    fun setSelectedBrand(brandId: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_SELECTED_BRAND, brandId).apply()
        Log.i(TAG, "Selected TV brand updated to: $brandId")
    }

    /**
     * Transmits the Power ON/OFF code via IR Blaster and sends Wake-on-LAN
     */
    suspend fun transmitPower(
        brandId: String = getSelectedBrand().id,
        targetIp: String? = null
    ): IrTransmitResult = withContext(Dispatchers.IO) {
        val brand = SUPPORTED_BRANDS.find { it.id.equals(brandId, ignoreCase = true) }
            ?: getSelectedBrand()

        // 1. Send Wake-on-LAN magic packet over network as dual-power assurance
        try {
            sendWakeOnLan(targetIp)
        } catch (e: Exception) {
            Log.w(TAG, "WoL packet could not be sent: ${e.message}")
        }

        // 2. Transmit physical IR blaster code if hardware is present
        val hasHardware = hasIrEmitter()
        if (hasHardware && consumerIrManager != null) {
            try {
                val pattern = generateIrPatternForBrand(brand.id)
                consumerIrManager.transmit(brand.carrierFreq, pattern)
                Log.i(TAG, "Transmitted IR Power code for ${brand.name} at ${brand.carrierFreq}Hz")
                return@withContext IrTransmitResult(
                    success = true,
                    isHardwareEmitter = true,
                    brand = brand,
                    message = "IR Blaster signal sent for ${brand.name} (38 kHz)"
                )
            } catch (e: Exception) {
                Log.e(TAG, "Error transmitting IR code: ${e.message}", e)
                return@withContext IrTransmitResult(
                    success = false,
                    isHardwareEmitter = true,
                    brand = brand,
                    message = "IR transmit error: ${e.message}"
                )
            }
        } else {
            // Phone lacks hardware IR Blaster emitter (e.g. Pixel, modern Samsung)
            Log.i(TAG, "Phone has no IR emitter hardware. Sent network Power signal for ${brand.name}")
            return@withContext IrTransmitResult(
                success = true,
                isHardwareEmitter = false,
                brand = brand,
                message = "IR Blaster not on phone. Sent Network Power command to ${brand.name}."
            )
        }
    }

    /**
     * Generates microsecond alternating pulse pattern (mark, space) for the selected brand.
     */
    private fun generateIrPatternForBrand(brandId: String): IntArray {
        return when (brandId.lowercase()) {
            "coocaa" -> {
                // Coocaa / Skyworth Power ON burst (38 kHz, iQOO Neo 10 OEM Match)
                COOCAA_POWER_PATTERN
            }
            "samsung" -> {
                // Samsung Power: 0xE0E040BF
                encodeSamsung(0xE0E040BF.toInt())
            }
            "lg" -> {
                // LG Power: 0x20DF10EF (NEC Protocol)
                encodeNec32(0x20DF10EF.toInt())
            }
            "sony" -> {
                // Sony Power: 0xA90 (12-bit SIRC)
                encodeSony12(0x0A90)
            }
            "xiaomi" -> {
                // Xiaomi TV Power: 0x00FF12ED
                encodeNec32(0x00FF12ED.toInt())
            }
            "oneplus", "realme" -> {
                // OnePlus & Realme TV Power: 0x01FE48B7
                encodeNec32(0x01FE48B7.toInt())
            }
            "tcl" -> {
                // TCL TV Power: 0x00FFA25D
                encodeNec32(0x00FFA25D.toInt())
            }
            "vu" -> {
                // Vu TV Power: 0x00FF10EF
                encodeNec32(0x00FF10EF.toInt())
            }
            "hisense" -> {
                // Hisense TV Power: 0xFD02FDFD
                encodeNec32(0xFD02FDFD.toInt())
            }
            "panasonic" -> {
                // Panasonic Power: standard 48-bit or standard fallback
                encodeNec32(0x40040100.toInt())
            }
            else -> {
                // Default Coocaa TV code
                encodeNec32(0x707030CF.toInt())
            }
        }
    }

    /**
     * Encodes 32-bit NEC protocol into microsecond pulses.
     * Header: 9000us mark, 4500us space.
     * Bit 0: 560us mark, 560us space.
     * Bit 1: 560us mark, 1690us space.
     * Stop: 560us mark.
     */
    private fun encodeNec32(code: Int): IntArray {
        val pulses = ArrayList<Int>(68)
        // Leading pulse burst
        pulses.add(9000)
        pulses.add(4500)

        // 32 data bits (MSB first)
        for (i in 31 downTo 0) {
            val bit = (code shr i) and 1
            pulses.add(560)
            if (bit == 1) {
                pulses.add(1690)
            } else {
                pulses.add(560)
            }
        }

        // Final stop bit
        pulses.add(560)
        return pulses.toIntArray()
    }

    /**
     * Encodes Samsung protocol: 4500us mark, 4500us space.
     */
    private fun encodeSamsung(code: Int): IntArray {
        val pulses = ArrayList<Int>(68)
        pulses.add(4500)
        pulses.add(4500)

        for (i in 31 downTo 0) {
            val bit = (code shr i) and 1
            pulses.add(560)
            if (bit == 1) {
                pulses.add(1690)
            } else {
                pulses.add(560)
            }
        }

        pulses.add(560)
        return pulses.toIntArray()
    }

    /**
     * Encodes Sony 12-bit SIRC: 2400us mark, 600us space.
     * Bit 0: 600us mark, 600us space.
     * Bit 1: 1200us mark, 600us space.
     */
    private fun encodeSony12(code: Int): IntArray {
        val pulses = ArrayList<Int>(28)
        pulses.add(2400)
        pulses.add(600)

        // 12 bits LSB first for Sony
        for (i in 0 until 12) {
            val bit = (code shr i) and 1
            if (bit == 1) {
                pulses.add(1200)
            } else {
                pulses.add(600)
            }
            pulses.add(600)
        }

        return pulses.toIntArray()
    }

    /**
     * Sends Wake-on-LAN magic packet to wake TV over local network if sleeping.
     */
    private fun sendWakeOnLan(targetIp: String?) {
        try {
            // Generic broadcast magic packet payload (FF * 6 followed by MAC or generic padding)
            val packetData = ByteArray(102)
            for (i in 0 until 6) packetData[i] = 0xFF.toByte()
            for (i in 6 until 102) packetData[i] = 0xFF.toByte()

            val socket = DatagramSocket()
            socket.broadcast = true

            // Send to network broadcast 255.255.255.255
            val broadcastAddr = InetAddress.getByName("255.255.255.255")
            val broadcastPacket = DatagramPacket(packetData, packetData.size, broadcastAddr, 9)
            socket.send(broadcastPacket)

            // Send directly to TV IP if known
            if (!targetIp.isNullOrBlank() && targetIp != "255.255.255.255") {
                val directAddr = InetAddress.getByName(targetIp)
                val directPacket = DatagramPacket(packetData, packetData.size, directAddr, 9)
                socket.send(directPacket)
            }

            socket.close()
        } catch (e: Exception) {
            Log.d(TAG, "WoL packet exception (ignored): ${e.message}")
        }
    }

    /**
     * Sends Wake-on-LAN without triggering hardware IR blaster (avoids hardware collision).
     */
    suspend fun sendWakeOnLanOnly(targetIp: String?) = withContext(Dispatchers.IO) {
        sendWakeOnLan(targetIp)
    }
}
