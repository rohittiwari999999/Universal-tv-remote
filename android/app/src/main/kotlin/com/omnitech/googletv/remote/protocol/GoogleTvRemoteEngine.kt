package com.omnitech.googletv.remote.protocol

import android.content.Context
import android.util.Log
import com.omnitech.googletv.remote.model.DiscoveredGoogleTv
import com.omnitech.googletv.remote.model.RemoteConnectionState
import com.omnitech.googletv.remote.model.TvKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import javax.net.ssl.SSLSocket

/**
 * Communicates with Google TV over the Remote Control Command Channel (port 6466).
 * Sends KeyEvents (DPad, Home, Back, Volume, Power), IME Text strings, and responds to Pings.
 */
class GoogleTvRemoteEngine(private val context: Context) {

    private val TAG = "GoogleTvRemote"

    private val _connectionState = MutableStateFlow<RemoteConnectionState>(RemoteConnectionState.Disconnected)
    val connectionState: StateFlow<RemoteConnectionState> = _connectionState.asStateFlow()

    private var sslSocket: SSLSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var connectedDevice: DiscoveredGoogleTv? = null
    private var pingJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + Job())

    suspend fun connect(device: DiscoveredGoogleTv) = withContext(Dispatchers.IO) {
        disconnect()
        connectedDevice = device
        _connectionState.value = RemoteConnectionState.Connecting(device)

        try {
            val sslContext = GoogleTvCrypto.getOrCreateSslContext(context)
            val socket = sslContext.socketFactory.createSocket() as SSLSocket
            sslSocket = socket

            socket.connect(InetSocketAddress(device.hostAddress, device.remotePort), 6000)
            socket.soTimeout = 10000
            socket.startHandshake()

            inputStream = socket.inputStream
            outputStream = socket.outputStream

            // Send Remote Configuration packet
            sendConfigurationPacket()

            _connectionState.value = RemoteConnectionState.Connected(device)
            Log.d(TAG, "Connected to Google TV remote service on port ${device.remotePort}")

            // Start listening loop and keep-alive ping loop
            startKeepAlive(socket)

        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}", e)
            _connectionState.value = RemoteConnectionState.Error("Could not connect to ${device.name}: ${e.localizedMessage}")
            disconnect()
        }
    }

    private fun sendConfigurationPacket() {
        val out = outputStream ?: return
        // Configure remote parameters: device info, model, supported features
        val configPayload = byteArrayOf(
            0x08, 0x01, // Remote version 1
            0x12, 0x08, 0x47, 0x6F, 0x6F, 0x67, 0x6C, 0x65, 0x54, 0x56 // "GoogleTV"
        )
        val packet = buildMessagePacket(type = 0x01, payload = configPayload)
        out.write(packet)
        out.flush()
    }

    /**
     * Sends a Key event (Key Down + Key Up) to the Google TV.
     */
    suspend fun sendKey(key: TvKey) = withContext(Dispatchers.IO) {
        val out = outputStream ?: return@withContext
        try {
            // Send Key Down (direction = 1)
            val downPacket = buildKeyEventPacket(key.code, direction = 1)
            out.write(downPacket)
            out.flush()

            delay(35) // Small tactile delay between press and release

            // Send Key Up (direction = 2)
            val upPacket = buildKeyEventPacket(key.code, direction = 2)
            out.write(upPacket)
            out.flush()

            Log.d(TAG, "Sent key: ${key.label} (code ${key.code})")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send key ${key.name}: ${e.message}")
        }
    }

    /**
     * Sends typed text directly into Google TV's focused search/input box.
     */
    suspend fun sendText(text: String) = withContext(Dispatchers.IO) {
        val out = outputStream ?: return@withContext
        try {
            val textBytes = text.toByteArray(Charsets.UTF_8)
            val packet = buildMessagePacket(type = 0x07, payload = textBytes)
            out.write(packet)
            out.flush()
            Log.d(TAG, "Sent IME text: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send text: ${e.message}")
        }
    }

    /**
     * Launches a specific app deep link (e.g., YouTube, Netflix, Prime Video).
     */
    suspend fun launchApp(packageName: String) = withContext(Dispatchers.IO) {
        val out = outputStream ?: return@withContext
        try {
            val payload = packageName.toByteArray(Charsets.UTF_8)
            val packet = buildMessagePacket(type = 0x08, payload = payload)
            out.write(packet)
            out.flush()
            Log.d(TAG, "Sent launch app: $packageName")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app $packageName: ${e.message}")
        }
    }

    private fun startKeepAlive(socket: SSLSocket) {
        pingJob?.cancel()
        pingJob = scope.launch {
            val buffer = ByteArray(256)
            while (isActive && !socket.isClosed) {
                try {
                    val input = inputStream ?: break
                    if (input.available() > 0) {
                        val count = input.read(buffer)
                        if (count > 0 && buffer[2].toInt() == 0x05) { // Ping request from TV
                            sendPingResponse()
                        }
                    }
                    delay(3000)
                } catch (e: Exception) {
                    if (isActive) {
                        Log.w(TAG, "Keep alive connection issue: ${e.message}")
                    }
                    break
                }
            }
        }
    }

    private fun sendPingResponse() {
        try {
            val out = outputStream ?: return
            val pingResponse = byteArrayOf(0x00, 0x02, 0x08, 0x06) // Ping response
            out.write(pingResponse)
            out.flush()
        } catch (ignored: Exception) {}
    }

    private fun buildKeyEventPacket(keyCode: Int, direction: Int): ByteArray {
        // Tag 0x04 for Key Event
        val payload = byteArrayOf(
            0x08, (keyCode and 0x7F).toByte(),
            0x10, direction.toByte()
        )
        return buildMessagePacket(type = 0x04, payload = payload)
    }

    private fun buildMessagePacket(type: Byte, payload: ByteArray): ByteArray {
        val length = payload.size + 2
        val packet = ByteArray(length + 2)
        packet[0] = (length shr 8).toByte()
        packet[1] = (length and 0xFF).toByte()
        packet[2] = 0x08
        packet[3] = type
        System.arraycopy(payload, 0, packet, 4, payload.size)
        return packet
    }

    fun disconnect() {
        pingJob?.cancel()
        pingJob = null
        try {
            inputStream?.close()
            outputStream?.close()
            sslSocket?.close()
        } catch (ignored: Exception) {}
        inputStream = null
        outputStream = null
        sslSocket = null
        _connectionState.value = RemoteConnectionState.Disconnected
        connectedDevice = null
    }
}
