package com.omnitech.googletv.remote.protocol

import android.content.Context
import android.util.Log
import com.omnitech.googletv.remote.model.DiscoveredGoogleTv
import com.omnitech.googletv.remote.model.PairingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.OutputStream
import java.net.InetSocketAddress
import java.security.cert.X509Certificate
import javax.net.ssl.SSLSocket

/**
 * Executes the authentic, full Google TV / Android TV Remote v2 Polo Pairing Protocol (Port 6467).
 *
 * Sequence:
 * 1. Wake-up retry ladder to handle TV Wi-Fi power-save sleep.
 * 2. TLS handshake with persistent X.509 client certificate on port 6467.
 * 3. Send OuterMessage(PairingRequest) -> TV answers with PairingRequestAck.
 * 4. Send OuterMessage(PairingOption) -> TV answers with PairingOption.
 * 5. Send OuterMessage(PairingConfiguration) -> TV answers with PairingConfigurationAck.
 *    ==> TV SCREEN IMMEDIATELY POPS UP THE 6-DIGIT HEX PAIRING CODE!
 * 6. User enters the 6-character code into the app.
 * 7. App computes SHA-256(ClientModulus + ClientExponent + ServerModulus + ServerExponent + PinTail).
 * 8. Send OuterMessage(PairingSecret: sha256_hash) -> TV verifies and answers with SecretAck.
 * 9. SUCCESS! TV is securely paired forever with this client certificate.
 */
class GoogleTvPairingEngine(private val context: Context) {

    private val TAG = "GoogleTvPairing"

    private val _pairingState = MutableStateFlow<PairingState>(PairingState.Idle)
    val pairingState: StateFlow<PairingState> = _pairingState.asStateFlow()

    private var sslSocket: SSLSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private var activeDevice: DiscoveredGoogleTv? = null
    private var serverCertificate: X509Certificate? = null
    private var clientCertificate: X509Certificate? = null

    /**
     * Connects to Google TV on port 6467 and triggers the on-screen 6-digit PIN code display.
     */
    suspend fun startPairing(device: DiscoveredGoogleTv) = withContext(Dispatchers.IO) {
        activeDevice = device
        _pairingState.value = PairingState.Connecting(device)

        try {
            closeExisting()

            val sslContext = GoogleTvCrypto.getOrCreateSslContext(context)

            // Retry ladder: TVs in Wi-Fi power-save need 1-2 SYN attempts to wake the Wi-Fi chip
            var connectedSocket: SSLSocket? = null
            var lastEx: Exception? = null

            for (attempt in 1..3) {
                try {
                    val socket = sslContext.socketFactory.createSocket() as SSLSocket
                    sslSocket = socket
                    Log.d(TAG, "Connecting to ${device.hostAddress}:${device.pairingPort} (attempt $attempt/3)...")
                    socket.connect(InetSocketAddress(device.hostAddress, device.pairingPort), 5000)
                    socket.soTimeout = 15000
                    socket.startHandshake()
                    connectedSocket = socket
                    break
                } catch (e: Exception) {
                    lastEx = e
                    closeExisting()
                    Log.w(TAG, "Pairing connect attempt $attempt failed: ${e.message}")
                    if (attempt < 3) {
                        delay(1200)
                    }
                }
            }

            if (connectedSocket == null) {
                throw lastEx ?: IllegalStateException("Could not connect to Google TV on port ${device.pairingPort}")
            }

            // Extract peer and local X.509 certificates for the crypto secret
            val peerCerts = connectedSocket.session.peerCertificates
            if (peerCerts.isNotEmpty() && peerCerts[0] is X509Certificate) {
                serverCertificate = peerCerts[0] as X509Certificate
            } else {
                throw IllegalStateException("Google TV did not present an X.509 server certificate")
            }

            clientCertificate = GoogleTvCrypto.getClientCertificate(context)

            val inStream = connectedSocket.inputStream
            val outStream = connectedSocket.outputStream
            inputStream = inStream
            outputStream = outStream

            // Phase 1: Send PairingRequest
            Log.d(TAG, "Step 1: Sending PairingRequest...")
            val reqBytes = GoogleTvProtobuf.buildPairingRequest(serviceName = "atvremote", clientName = "atvremote")
            GoogleTvProtobuf.writeFramedMessage(outStream, reqBytes)

            // Read response: PairingRequestAck
            val ack1Raw = GoogleTvProtobuf.readFramedMessage(inStream)
            val ack1 = GoogleTvProtobuf.parsePoloMessage(ack1Raw)
            Log.d(TAG, "Step 1 Response: status=${ack1.status}, reqAck=${ack1.hasPairingRequestAck}")
            if (ack1.status != 200 || !ack1.hasPairingRequestAck) {
                throw IllegalStateException("TV rejected PairingRequest with status ${ack1.status}")
            }

            // Phase 2: Send PairingOption
            Log.d(TAG, "Step 2: Sending PairingOption...")
            val optBytes = GoogleTvProtobuf.buildPairingOption()
            GoogleTvProtobuf.writeFramedMessage(outStream, optBytes)

            val optRaw = GoogleTvProtobuf.readFramedMessage(inStream)
            val optResp = GoogleTvProtobuf.parsePoloMessage(optRaw)
            Log.d(TAG, "Step 2 Response: status=${optResp.status}, opt=${optResp.hasPairingOption}")
            if (optResp.status != 200 || !optResp.hasPairingOption) {
                throw IllegalStateException("TV rejected PairingOption with status ${optResp.status}")
            }

            // Phase 3: Send PairingConfiguration
            Log.d(TAG, "Step 3: Sending PairingConfiguration...")
            val configBytes = GoogleTvProtobuf.buildPairingConfiguration()
            GoogleTvProtobuf.writeFramedMessage(outStream, configBytes)

            val configRaw = GoogleTvProtobuf.readFramedMessage(inStream)
            val configResp = GoogleTvProtobuf.parsePoloMessage(configRaw)
            Log.d(TAG, "Step 3 Response: status=${configResp.status}, configAck=${configResp.hasConfigurationAck}")
            if (configResp.status != 200 || !configResp.hasConfigurationAck) {
                throw IllegalStateException("TV rejected PairingConfiguration with status ${configResp.status}")
            }

            // At this exact moment, Google TV has displayed the 6-character code on the screen!
            Log.i(TAG, "SUCCESS: Google TV screen is now displaying the 6-character pairing code!")
            _pairingState.value = PairingState.AwaitingPin(device)

        } catch (e: Exception) {
            Log.e(TAG, "Pairing sequence failed: ${e.message}", e)
            val msg = when {
                e.message?.contains("failed to connect", true) == true || e.message?.contains("ETIMEDOUT", true) == true ->
                    "TV connection timed out on port ${device.pairingPort}.\nMake sure TV screen is ON (not in Standby) and on same Wi-Fi."
                e.message?.contains("ECONNREFUSED", true) == true ->
                    "TV refused pairing connection on port 6467. Ensure TV screen is awake."
                else -> e.localizedMessage ?: "Pairing handshake failed"
            }
            _pairingState.value = PairingState.Error(msg)
            closeExisting()
        }
    }

    /**
     * Submits the 6-character hexadecimal PIN code displayed on the Google TV screen.
     */
    suspend fun submitPin(pinCode: String) = withContext(Dispatchers.IO) {
        val device = activeDevice ?: return@withContext
        val inStream = inputStream
        val outStream = outputStream
        val sCert = serverCertificate
        val cCert = clientCertificate

        if (inStream == null || outStream == null || sCert == null || cCert == null) {
            _pairingState.value = PairingState.Error("Pairing session expired. Please tap 'Retry Pairing'.")
            return@withContext
        }

        _pairingState.value = PairingState.VerifyingPin(device)

        try {
            // Compute official Polo SHA-256 hash
            Log.d(TAG, "Computing SHA-256 verification hash for PIN $pinCode...")
            val secretHash = GoogleTvCrypto.computePairingSecretHash(cCert, sCert, pinCode)

            // Send PairingSecret message
            Log.d(TAG, "Step 4: Sending PairingSecret...")
            val secretMsg = GoogleTvProtobuf.buildPairingSecret(secretHash)
            GoogleTvProtobuf.writeFramedMessage(outStream, secretMsg)

            // Read SecretAck
            val secAckRaw = GoogleTvProtobuf.readFramedMessage(inStream)
            val secAck = GoogleTvProtobuf.parsePoloMessage(secAckRaw)
            Log.d(TAG, "Step 4 Response: status=${secAck.status}, secAck=${secAck.hasSecretAck}")

            if (secAck.status == 200 && secAck.hasSecretAck) {
                Log.i(TAG, "PAIRING 100% COMPLETE AND AUTHENTICATED BY GOOGLE TV!")
                _pairingState.value = PairingState.Paired(device)
            } else {
                _pairingState.value = PairingState.Error("Incorrect PIN code or authentication rejected by TV.")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Secret verification failed: ${e.message}", e)
            _pairingState.value = PairingState.Error(e.localizedMessage ?: "PIN verification failed")
        } finally {
            closeExisting()
        }
    }

    fun reset() {
        closeExisting()
        _pairingState.value = PairingState.Idle
        activeDevice = null
    }

    private fun closeExisting() {
        try {
            inputStream?.close()
            outputStream?.close()
            sslSocket?.close()
        } catch (ignored: Exception) {}
        inputStream = null
        outputStream = null
        sslSocket = null
    }
}
