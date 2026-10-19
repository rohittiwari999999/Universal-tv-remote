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
 * Reactive, non-blocking TLS implementation ensuring instant UI transition to RemoteScreen.
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
            Log.d(TAG, "Connecting TLS socket to ${device.hostAddress}:${device.remotePort} (${device.name})")
            val sslContext = GoogleTvCrypto.getOrCreateSslContext(context)
            val socket = sslContext.socketFactory.createSocket() as SSLSocket
            sslSocket = socket

            // 5-second socket connection timeout
            socket.connect(InetSocketAddress(device.hostAddress, device.remotePort), 5000)
            socket.soTimeout = 0 // Don't block forever with soTimeout; receive loop will listen
            socket.startHandshake()

            val inStream = socket.inputStream
            val outStream = socket.outputStream
            inputStream = inStream
            outputStream = outStream

            // TLS handshake is successful! Immediately transition UI to Connected RemoteScreen!
            Log.i(TAG, "CONNECTED TO GOOGLE TV on port ${device.remotePort}! RemoteScreen ready.")
            _connectionState.value = RemoteConnectionState.Connected(device)

            // Send initial client configure message to Google TV
            try {
                val clientConf = GoogleTvProtobuf.buildRemoteConfigure(622)
                synchronized(outStream) {
                    GoogleTvProtobuf.writeFramedMessage(outStream, clientConf)
                }
                Log.d(TAG, "Sent client remoteConfigure(622)")
            } catch (e: Exception) {
                Log.w(TAG, "Notice sending remoteConfigure: ${e.message}")
            }

            // Start asynchronous continuous listener for keepalives and TV messages
            startContinuousListener(inStream, outStream, device)

        } catch (e: Exception) {
            Log.e(TAG, "Connection to ${device.hostAddress}:${device.remotePort} failed: ${e.message}", e)
            val errorText = when {
                e.message?.contains("handshake_failure", true) == true || e.message?.contains("SSL", true) == true ->
                    "Authentication rejected by TV. Tap 'Re-pair' to generate a fresh PIN."
                e.message?.contains("ECONNREFUSED", true) == true ->
                    "Connection refused on port ${device.remotePort}. Ensure TV is ON and connected to Wi-Fi."
                e.message?.contains("ETIMEDOUT", true) == true ->
                    "Connection timed out. Check your TV's IP address (${device.hostAddress})."
                else -> e.localizedMessage ?: "Could not connect to ${device.name}"
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

                    // If TV sends remote_configure, respond with our client configuration
                    if (incoming.hasRemoteConfigure) {
                        Log.d(TAG, "TV sent remote_configure -> replying with client configure")
                        val conf = GoogleTvProtobuf.buildRemoteConfigure(622)
                        synchronized(outStream) {
                            GoogleTvProtobuf.writeFramedMessage(outStream, conf)
                        }
                    }

                    // If TV sends remote_set_active, respond with active=622
                    if (incoming.hasRemoteSetActive) {
                        Log.d(TAG, "TV sent remote_set_active -> replying with active=622")
                        val act = GoogleTvProtobuf.buildRemoteSetActive(622)
                        synchronized(outStream) {
                            GoogleTvProtobuf.writeFramedMessage(outStream, act)
                        }
                    }

                    // Respond immediately to TV keepalive pings
                    if (incoming.pingVal1 != null) {
                        val pingResp = GoogleTvProtobuf.buildRemotePingResponse(incoming.pingVal1)
                        synchronized(outStream) {
                            GoogleTvProtobuf.writeFramedMessage(outStream, pingResp)
                        }
                    }

                    if (incoming.hasRemoteStart) {
                        Log.i(TAG, "TV confirmed remote_start: ready for all commands")
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
