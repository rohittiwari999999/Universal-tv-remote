package com.omnitech.googletv.remote.protocol

import android.content.Context
import android.util.Log
import com.omnitech.googletv.remote.model.DiscoveredGoogleTv
import com.omnitech.googletv.remote.model.RemoteConnectionState
import com.omnitech.googletv.remote.model.TvKey
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import javax.net.ssl.SSLSocket

/**
 * Communicates with Google TV over the authentic Remote Control Command Channel (Port 6466).
 * Sends real KeyEvents, Ping responses, and App deep link launches using Google TV Protobuf framing.
 */
class GoogleTvRemoteEngine(private val context: Context) {

    private val TAG = "GoogleTvRemote"

    private val _connectionState = MutableStateFlow<RemoteConnectionState>(RemoteConnectionState.Disconnected)
    val connectionState: StateFlow<RemoteConnectionState> = _connectionState.asStateFlow()

    private var sslSocket: SSLSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var connectedDevice: DiscoveredGoogleTv? = null
    private var receiveJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var imeCounter = 0
    private var imeFieldCounter = 0

    suspend fun connect(device: DiscoveredGoogleTv) = withContext(Dispatchers.IO) {
        disconnect()
        connectedDevice = device
        _connectionState.value = RemoteConnectionState.Connecting(device)

        try {
            Log.d(TAG, "Connecting to Google TV remote command channel at ${device.hostAddress}:${device.remotePort}")
            val sslContext = GoogleTvCrypto.getOrCreateSslContext(context)
            val socket = sslContext.socketFactory.createSocket() as SSLSocket
            sslSocket = socket

            socket.connect(InetSocketAddress(device.hostAddress, device.remotePort), 6000)
            socket.soTimeout = 0 // Don't timeout continuous stream; keepalive ping manages it
            socket.startHandshake()

            val inStream = socket.inputStream
            val outStream = socket.outputStream
            inputStream = inStream
            outputStream = outStream

            // Phase 1: Handshake with Google TV Remote Service
            // Read TV's initial remote_configure message
            val tvConfRaw = GoogleTvProtobuf.readFramedMessage(inStream)
            val tvConf = GoogleTvProtobuf.parseRemoteMessage(tvConfRaw)
            Log.d(TAG, "TV remote_configure received (hasConfigure=${tvConf.hasRemoteConfigure})")

            // Send Client remote_configure (features = 622)
            val clientConf = GoogleTvProtobuf.buildRemoteConfigure(622)
            GoogleTvProtobuf.writeFramedMessage(outStream, clientConf)

            // Read TV's remote_set_active
            val tvActiveRaw = GoogleTvProtobuf.readFramedMessage(inStream)
            val tvActive = GoogleTvProtobuf.parseRemoteMessage(tvActiveRaw)
            Log.d(TAG, "TV remote_set_active received (hasSetActive=${tvActive.hasRemoteSetActive})")

            // Send Client remote_set_active
            val clientActive = GoogleTvProtobuf.buildRemoteSetActive(622)
            GoogleTvProtobuf.writeFramedMessage(outStream, clientActive)

            // Read TV's remote_start
            val tvStartRaw = GoogleTvProtobuf.readFramedMessage(inStream)
            val tvStart = GoogleTvProtobuf.parseRemoteMessage(tvStartRaw)
            Log.d(TAG, "TV remote_start received (hasRemoteStart=${tvStart.hasRemoteStart})")

            _connectionState.value = RemoteConnectionState.Connected(device)
            Log.i(TAG, "CONNECTED TO GOOGLE TV! Ready to transmit remote control keys.")

            // Start continuous listener for pings and state updates
            startContinuousListener(inStream, outStream, device)

        } catch (e: Exception) {
            Log.e(TAG, "Connection failed: ${e.message}", e)
            val errorText = when {
                e.message?.contains("handshake_failure", true) == true || e.message?.contains("SSL", true) == true ->
                    "Authentication rejected by TV. The TV may need to be paired again."
                e.message?.contains("ECONNREFUSED", true) == true ->
                    "Connection refused on port 6466. Please verify TV is turned ON."
                else -> e.localizedMessage ?: "Connection failed"
            }
            _connectionState.value = RemoteConnectionState.Error(errorText)
            disconnect()
        }
    }

    private fun startContinuousListener(inStream: InputStream, outStream: OutputStream, device: DiscoveredGoogleTv) {
        receiveJob?.cancel()
        receiveJob = scope.launch {
            try {
                while (isActive) {
                    val rawMsg = GoogleTvProtobuf.readFramedMessage(inStream)
                    val incoming = GoogleTvProtobuf.parseRemoteMessage(rawMsg)

                    // Respond immediately to TV keepalive pings
                    if (incoming.pingVal1 != null) {
                        val pingResp = GoogleTvProtobuf.buildRemotePingResponse(incoming.pingVal1)
                        synchronized(outStream) {
                            GoogleTvProtobuf.writeFramedMessage(outStream, pingResp)
                        }
                    }

                    if (incoming.imeCounter != null) imeCounter = incoming.imeCounter
                    if (incoming.imeFieldCounter != null) imeFieldCounter = incoming.imeFieldCounter
                }
            } catch (e: Exception) {
                if (isActive) {
                    Log.w(TAG, "Remote connection stream ended: ${e.message}")
                    _connectionState.value = RemoteConnectionState.Disconnected
                }
            }
        }
    }

    /**
     * Sends a Key event (DPad, Home, Back, Volume, Power, etc.) to the Google TV.
     */
    suspend fun sendKey(key: TvKey) = withContext(Dispatchers.IO) {
        val out = outputStream ?: return@withContext
        try {
            // Direction 3 = SHORT press
            val keyBytes = GoogleTvProtobuf.buildRemoteKeyInject(key.code, direction = 3)
            synchronized(out) {
                GoogleTvProtobuf.writeFramedMessage(out, keyBytes)
            }
            Log.d(TAG, "Sent key ${key.label} (code=${key.code}) to Google TV")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send key ${key.name}: ${e.message}")
        }
    }

    /**
     * Sends text string to the Google TV input field.
     */
    suspend fun sendText(text: String) = withContext(Dispatchers.IO) {
        val out = outputStream ?: return@withContext
        try {
            val textBytes = GoogleTvProtobuf.buildRemoteImeBatchEdit(text, imeCounter, imeFieldCounter)
            synchronized(out) {
                GoogleTvProtobuf.writeFramedMessage(out, textBytes)
            }
            Log.d(TAG, "Sent text input to Google TV: $text")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send text: ${e.message}")
        }
    }

    /**
     * Deep-links directly to popular streaming apps on Google TV.
     */
    suspend fun launchApp(packageName: String) = withContext(Dispatchers.IO) {
        val out = outputStream ?: return@withContext
        try {
            val appUrl = when (packageName) {
                "com.google.android.youtube.tv", "youtube" -> "https://www.youtube.com"
                "com.netflix.ninja", "netflix" -> "https://www.netflix.com/title"
                "com.amazon.amazonvideo.livingroom", "prime" -> "https://app.primevideo.com"
                "in.startv.hotstar", "hotstar" -> "https://www.hotstar.com"
                else -> packageName
            }
            val appBytes = GoogleTvProtobuf.buildRemoteAppLink(appUrl)
            synchronized(out) {
                GoogleTvProtobuf.writeFramedMessage(out, appBytes)
            }
            Log.d(TAG, "Launched app via link $appUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch app $packageName: ${e.message}")
        }
    }

    fun disconnect() {
        receiveJob?.cancel()
        receiveJob = null
        try {
            inputStream?.close()
            outputStream?.close()
            sslSocket?.close()
        } catch (ignored: Exception) {}
        inputStream = null
        outputStream = null
        sslSocket = null
        if (_connectionState.value !is RemoteConnectionState.Disconnected) {
            _connectionState.value = RemoteConnectionState.Disconnected
        }
    }
}
