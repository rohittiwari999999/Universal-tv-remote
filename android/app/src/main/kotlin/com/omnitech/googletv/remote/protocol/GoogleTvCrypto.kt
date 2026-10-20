package com.omnitech.googletv.remote.protocol

import android.content.Context
import android.util.Log
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.math.BigInteger
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

/**
 * Handles persistent TLS X.509 client certificate management and official
 * Google TV / Android TV Remote Protocol v2 pairing cryptography.
 */
object GoogleTvCrypto {

    private const val TAG = "GoogleTvCrypto"
    private const val KEY_ALIAS = "google_tv_remote_client_key"
    private const val KEY_FILE = "google_tv_client_key.pk8"
    private const val CERT_FILE = "google_tv_client_cert.der"

    init {
        try {
            // Android has an internal restricted BouncyCastle.
            // Insert full BouncyCastle provider cleanly to avoid NoSuchAlgorithmException.
            Security.removeProvider("BC")
            Security.insertProviderAt(BouncyCastleProvider(), 1)
        } catch (e: Exception) {
            try {
                Security.addProvider(BouncyCastleProvider())
            } catch (ignored: Exception) {}
        }
    }

    @Volatile
    private var cachedKeyPair: KeyPair? = null

    @Volatile
    private var cachedCertificate: X509Certificate? = null

    /**
     * Retrieves or creates a persistent SSLContext with a self-signed X.509 client certificate.
     * The certificate is saved to internal storage so subsequent app launches reuse the same identity.
     */
    @Synchronized
    fun getOrCreateSslContext(context: Context): SSLContext {
        val (keyPair, cert) = getOrCreateCredentials(context)

        val keyStore = try {
            KeyStore.getInstance("PKCS12").apply {
                load(null, null)
                setKeyEntry(KEY_ALIAS, keyPair.private, "remote_secret".toCharArray(), arrayOf(cert))
            }
        } catch (e: Exception) {
            KeyStore.getInstance(KeyStore.getDefaultType()).apply {
                load(null, null)
                setKeyEntry(KEY_ALIAS, keyPair.private, "remote_secret".toCharArray(), arrayOf(cert))
            }
        }

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, "remote_secret".toCharArray())
        }

        // Trust manager that accepts the Google TV's self-signed TLS certificate
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(kmf.keyManagers, trustAllCerts, SecureRandom())
        return sslContext
    }

    @Synchronized
    fun getClientCertificate(context: Context): X509Certificate {
        return getOrCreateCredentials(context).second
    }

    /**
     * Wipes stored identity and forces generation of a clean new certificate.
     */
    @Synchronized
    fun resetCredentials(context: Context) {
        cachedKeyPair = null
        cachedCertificate = null
        try {
            File(context.filesDir, KEY_FILE).delete()
            File(context.filesDir, CERT_FILE).delete()
            Log.d(TAG, "Reset client TLS credentials successfully")
        } catch (e: Exception) {
            Log.w(TAG, "Failed to delete credential files: ${e.message}")
        }
    }

    @Synchronized
    private fun getOrCreateCredentials(context: Context): Pair<KeyPair, X509Certificate> {
        val memPair = cachedKeyPair
        val memCert = cachedCertificate
        if (memPair != null && memCert != null) {
            return Pair(memPair, memCert)
        }

        val keyFile = File(context.filesDir, KEY_FILE)
        val certFile = File(context.filesDir, CERT_FILE)

        if (keyFile.exists() && certFile.exists()) {
            try {
                val keyBytes = keyFile.readBytes()
                val certBytes = certFile.readBytes()

                val keyFactory = KeyFactory.getInstance("RSA")
                val privKey = keyFactory.generatePrivate(PKCS8EncodedKeySpec(keyBytes)) as RSAPrivateKey

                val certFactory = CertificateFactory.getInstance("X.509")
                val cert = certFactory.generateCertificate(ByteArrayInputStream(certBytes)) as X509Certificate

                val keyPair = KeyPair(cert.publicKey, privKey)
                cachedKeyPair = keyPair
                cachedCertificate = cert
                Log.d(TAG, "Loaded persistent client TLS certificate and key from storage")
                return Pair(keyPair, cert)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load saved credentials, regenerating: ${e.message}")
            }
        }

        // Generate brand new RSA 2048 KeyPair and X.509 Certificate
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        val keyPair = kpg.generateKeyPair()

        val cert = generateCertificate(keyPair)

        // Save persistently to app internal files
        try {
            FileOutputStream(keyFile).use { it.write(keyPair.private.encoded) }
            FileOutputStream(certFile).use { it.write(cert.encoded) }
            Log.d(TAG, "Persisted new client TLS certificate and private key to storage")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save client certificate: ${e.message}", e)
        }

        cachedKeyPair = keyPair
        cachedCertificate = cert
        return Pair(keyPair, cert)
    }

    private fun generateCertificate(keyPair: KeyPair): X509Certificate {
        val now = System.currentTimeMillis()
        val startDate = Date(now - 24 * 60 * 60 * 1000L) // yesterday
        val endDate = Date(now + 10L * 365 * 24 * 60 * 60 * 1000L) // 10 years

        // Official CN used by Google TV Remote
        val subject = X500Name("CN=atvremote")
        val serial = BigInteger.valueOf(1000L)

        val builder = JcaX509v3CertificateBuilder(
            subject,
            serial,
            startDate,
            endDate,
            subject,
            keyPair.public
        )

        try {
            builder.addExtension(
                org.bouncycastle.asn1.x509.Extension.basicConstraints,
                false,
                org.bouncycastle.asn1.x509.BasicConstraints(true)
            )
            builder.addExtension(
                org.bouncycastle.asn1.x509.Extension.subjectAlternativeName,
                false,
                org.bouncycastle.asn1.x509.GeneralNames(
                    org.bouncycastle.asn1.x509.GeneralName(
                        org.bouncycastle.asn1.x509.GeneralName.dNSName,
                        "atvremote"
                    )
                )
            )
        } catch (e: Exception) {
            Log.w(TAG, "Notice adding cert extensions: ${e.message}")
        }

        val signer = JcaContentSignerBuilder("SHA256withRSA")
            .setProvider(BouncyCastleProvider())
            .build(keyPair.private)

        val certHolder = builder.build(signer)
        return JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider())
            .getCertificate(certHolder)
    }

    /**
     * Computes the official Google TV Polo Pairing SHA-256 secret.
     * TV displays a 6-character hex PIN (e.g., "7D8A2C").
     *
     * Protocol Hash Input:
     * SHA256( ClientModulusHex + "0" + ClientExponentHex + ServerModulusHex + "0" + ServerExponentHex + PinHexTail )
     *
     * Verification:
     * digest[0] must match the first byte of the PIN (pinCode.substring(0, 2)).
     */
    fun computePairingSecretHash(
        clientCert: X509Certificate,
        serverCert: X509Certificate,
        pinCode: String
    ): ByteArray {
        val cleanPin = pinCode.trim().lowercase()
        require(cleanPin.length == 6) { "Pairing PIN code must be exactly 6 hex characters" }

        val clientKey = clientCert.publicKey as? RSAPublicKey
            ?: throw IllegalArgumentException("Client certificate does not contain an RSA public key")
        val serverKey = serverCert.publicKey as? RSAPublicKey
            ?: throw IllegalArgumentException("Server certificate does not contain an RSA public key")

        var clientModHex = clientKey.modulus.toString(16)
        if (clientModHex.length % 2 != 0) clientModHex = "0$clientModHex"

        var clientExpHex = "0" + clientKey.publicExponent.toString(16)
        if (clientExpHex.length % 2 != 0) clientExpHex = "0$clientExpHex"

        var serverModHex = serverKey.modulus.toString(16)
        if (serverModHex.length % 2 != 0) serverModHex = "0$serverModHex"

        var serverExpHex = "0" + serverKey.publicExponent.toString(16)
        if (serverExpHex.length % 2 != 0) serverExpHex = "0$serverExpHex"

        val pinTailHex = cleanPin.substring(2) // characters 2..5 (4 hex chars = 2 bytes)

        val md = MessageDigest.getInstance("SHA-256")
        md.update(hexToBytes(clientModHex))
        md.update(hexToBytes(clientExpHex))
        md.update(hexToBytes(serverModHex))
        md.update(hexToBytes(serverExpHex))
        md.update(hexToBytes(pinTailHex))

        val digest = md.digest()

        // Verify checksum with first 2 characters of PIN
        val expectedFirstByte = cleanPin.substring(0, 2).toInt(16).toByte()
        if (digest[0] != expectedFirstByte) {
            Log.w(TAG, "PIN checksum mismatch: expected ${cleanPin.substring(0, 2)} (${expectedFirstByte.toInt() and 0xFF}), got ${digest[0].toInt() and 0xFF}")
            throw IllegalArgumentException("Invalid PIN code. The code entered does not match TV signature.")
        }

        return digest
    }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
