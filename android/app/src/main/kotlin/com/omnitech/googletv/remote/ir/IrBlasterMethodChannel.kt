package com.omnitech.googletv.remote.ir

import android.content.Context
import android.util.Log

/**
 * Flutter MethodChannel specification and handler for IR Blaster.
 * Connects Dart MethodChannel ("com.omnitech.googletv.remote/ir_blaster")
 * to Android native ConsumerIrManager.
 *
 * Implements strict error handling:
 * - Unsupported hardware (NO_SYSTEM_FEATURE)
 * - Unavailable service (SERVICE_UNAVAILABLE)
 * - Disabled emitter (NO_EMITTER)
 * - Invalid frequency & invalid pulse pattern
 * - Transmission exceptions
 */
class IrBlasterMethodChannel(private val context: Context) {

    private val TAG = "IrMethodChannel"
    val channelName = "com.omnitech.googletv.remote/ir_blaster"

    private val irBlasterManager = IrBlasterManager(context)
    private val coocaaController = CoocaaIrController(context)

    /**
     * Handles method calls invoked from Dart / Flutter layer.
     */
    fun handleMethodCall(method: String, arguments: Map<String, Any?>?): Map<String, Any?> {
        Log.i(TAG, "MethodChannel call received: $method with args: $arguments")
        return when (method) {
            "checkHardwareSupport" -> {
                val diag = irBlasterManager.getHardwareDiagnostic()
                mapOf(
                    "hasSystemFeature" to diag.hasSystemFeature,
                    "isServiceAvailable" to diag.isServiceAvailable,
                    "hasIrEmitter" to diag.hasIrEmitter,
                    "status" to diag.status.name,
                    "diagnosticSummary" to diag.diagnosticSummary,
                    "supportedCarrierRanges" to diag.supportedCarrierRanges.map {
                        mapOf("min" to it.minFrequencyHz, "max" to it.maxFrequencyHz)
                    }
                )
            }
            "getCarrierFrequencies" -> {
                val ranges = irBlasterManager.getCarrierFrequenciesList()
                mapOf(
                    "ranges" to ranges.map {
                        mapOf("min" to it.minFrequencyHz, "max" to it.maxFrequencyHz, "label" to it.toString())
                    }
                )
            }
            "transmitHex" -> {
                val hexCode = arguments?.get("hex") as? String ?: "0x40BF12ED"
                val frequency = (arguments?.get("frequency") as? Number)?.toInt() ?: 38000
                val repeatCount = (arguments?.get("repeatCount") as? Number)?.toInt() ?: 1

                val result = coocaaController.transmitConfiguredCode(hexCode, frequency, repeatCount)
                mapOf(
                    "success" to result.apiExecutedSuccessfully,
                    "apiExecutedSuccessfully" to result.apiExecutedSuccessfully,
                    "status" to result.status,
                    "message" to result.message,
                    "pulseCount" to result.pulseCount,
                    "durationMicros" to result.durationMicros,
                    "carrierFreqHz" to result.carrierFreqHz,
                    "errorDetails" to (result.errorDetails ?: "")
                )
            }
            "transmitRaw" -> {
                val rawList = arguments?.get("pattern") as? List<*>
                val frequency = (arguments?.get("frequency") as? Number)?.toInt() ?: 38000

                if (rawList == null || rawList.isEmpty()) {
                    mapOf(
                        "success" to false,
                        "apiExecutedSuccessfully" to false,
                        "status" to "STATUS_INVALID_PATTERN",
                        "message" to "Pattern list is null or empty"
                    )
                } else {
                    val pattern = rawList.mapNotNull { (it as? Number)?.toInt() }.toIntArray()
                    val result = irBlasterManager.transmitCustomPattern(frequency, pattern, "MethodChannel Raw")
                    mapOf(
                        "success" to result.apiExecutedSuccessfully,
                        "apiExecutedSuccessfully" to result.apiExecutedSuccessfully,
                        "status" to result.status,
                        "message" to result.message,
                        "pulseCount" to result.pulseCount,
                        "durationMicros" to result.durationMicros,
                        "carrierFreqHz" to result.carrierFreqHz,
                        "errorDetails" to (result.errorDetails ?: "")
                    )
                }
            }
            "transmitCoocaaPreset" -> {
                val index = (arguments?.get("index") as? Number)?.toInt() ?: 1
                val variant = CoocaaIrController.ALL_VARIANTS.find { it.index == index }
                    ?: CoocaaIrController.ALL_VARIANTS.first()
                val result = irBlasterManager.transmitCustomPattern(variant.frequencyHz, variant.pattern, variant.name)
                mapOf(
                    "success" to result.apiExecutedSuccessfully,
                    "apiExecutedSuccessfully" to result.apiExecutedSuccessfully,
                    "status" to result.status,
                    "message" to result.message,
                    "variantName" to variant.name,
                    "hexCode" to variant.hexCode,
                    "pulseCount" to result.pulseCount,
                    "durationMicros" to result.durationMicros,
                    "carrierFreqHz" to result.carrierFreqHz
                )
            }
            else -> {
                mapOf(
                    "success" to false,
                    "status" to "METHOD_NOT_IMPLEMENTED",
                    "message" to "Method '$method' not implemented on channel $channelName"
                )
            }
        }
    }
}
