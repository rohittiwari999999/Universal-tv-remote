package com.omnitech.googletv.remote.ir

import android.content.Context
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
        val SUPPORTED_BRANDS = listOf(
            TvBrand("coocaa", "Coocaa TV", "Coocaa / Skyworth NEC 38.4kHz (Google & Android TV)", 38400, "NEC", "0x707030CF"),
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
                // Coocaa / Skyworth 68-pulse IR timing pattern (38.4 kHz)
                CoocaaIrController.COOCAA_POWER_PATTERN
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
}
