package com.omnitech.googletv.remote.ir

/**
 * Represents carrier frequency ranges supported by phone's IR blaster hardware.
 */
data class IrCarrierRange(
    val minFrequencyHz: Int,
    val maxFrequencyHz: Int
) {
    override fun toString(): String = "${minFrequencyHz / 1000} kHz - ${maxFrequencyHz / 1000} kHz"
}

/**
 * Hardware diagnostic statuses for ConsumerIR on Android.
 */
enum class IrHardwareStatus {
    SUPPORTED_AND_READY,
    NO_SYSTEM_FEATURE,
    SERVICE_UNAVAILABLE,
    EMITTER_NOT_DETECTED
}

/**
 * Complete hardware diagnostics report for the device.
 */
data class IrHardwareDiagnostic(
    val status: IrHardwareStatus,
    val hasSystemFeature: Boolean,
    val isServiceAvailable: Boolean,
    val hasIrEmitter: Boolean,
    val supportedCarrierRanges: List<IrCarrierRange>,
    val diagnosticSummary: String
)

/**
 * Execution result of a native IR transmission call.
 * Strictly separates successful API invocation from confirmed physical TV response.
 */
data class IrExecutionResult(
    val apiExecutedSuccessfully: Boolean,
    val status: String,
    val message: String,
    val carrierFreqHz: Int,
    val pulseCount: Int,
    val durationMicros: Long,
    val hexCode: String? = null,
    val errorDetails: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
