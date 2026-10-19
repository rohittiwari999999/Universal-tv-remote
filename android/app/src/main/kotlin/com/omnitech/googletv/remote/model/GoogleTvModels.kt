package com.omnitech.googletv.remote.model

/**
 * Represents a Google TV or Android TV device discovered on the local network.
 */
data class DiscoveredGoogleTv(
    val id: String,
    val name: String,
    val hostAddress: String,
    val pairingPort: Int = 6467,
    val remotePort: Int = 6466,
    val model: String = "Google TV",
    val manufacturer: String = "Google",
    val isCastEnabled: Boolean = true,
    val signalStrengthDbm: Int = -50,
    val isPaired: Boolean = false,
    val lastSeenTimestamp: Long = System.currentTimeMillis()
)

/**
 * State of the pairing process with Google TV.
 */
sealed class PairingState {
    object Idle : PairingState()
    object Scanning : PairingState()
    data class Connecting(val device: DiscoveredGoogleTv) : PairingState()
    data class AwaitingPin(val device: DiscoveredGoogleTv) : PairingState()
    data class VerifyingPin(val device: DiscoveredGoogleTv) : PairingState()
    data class Paired(val device: DiscoveredGoogleTv) : PairingState()
    data class Error(val message: String) : PairingState()
}

/**
 * State of the active remote control session over port 6466.
 */
sealed class RemoteConnectionState {
    object Disconnected : RemoteConnectionState()
    data class Connecting(val device: DiscoveredGoogleTv) : RemoteConnectionState()
    data class Connected(
        val device: DiscoveredGoogleTv,
        val latencyMs: Long = 12,
        val isMuted: Boolean = false,
        val currentVolume: Int = 24
    ) : RemoteConnectionState()
    data class Error(val message: String) : RemoteConnectionState()
}

/**
 * Android KeyCodes and Google TV command keys
 */
enum class TvKey(val code: Int, val label: String) {
    POWER(26, "Power"),
    HOME(3, "Home"),
    BACK(4, "Back"),
    DPAD_UP(19, "Up"),
    DPAD_DOWN(20, "Down"),
    DPAD_LEFT(21, "Left"),
    DPAD_RIGHT(22, "Right"),
    DPAD_CENTER(23, "Select"),
    VOLUME_UP(24, "Vol +"),
    VOLUME_DOWN(25, "Vol -"),
    VOLUME_MUTE(164, "Mute"),
    PLAY_PAUSE(85, "Play/Pause"),
    REWIND(89, "Rewind"),
    FAST_FORWARD(90, "Fast Forward"),
    ASSISTANT(231, "Assistant"),
    SETTINGS(176, "Settings"),
    APP_YOUTUBE(1001, "YouTube"),
    APP_NETFLIX(1002, "Netflix"),
    APP_PRIME(1003, "Prime Video"),
    APP_HOTSTAR(1004, "Disney+ Hotstar")
}
