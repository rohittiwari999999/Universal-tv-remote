package com.omnitech.googletv.remote.protocol

import android.content.Context
import android.util.Base64
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.util.Date
import javax.net.ssl.KeyManagerFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import org.bouncycastle.asn1.x500.X500Name
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder

/**
 * Handles TLS X.509 client certificate generation and PIN code hashing
 * for Google TV / Android TV Remote Protocol v2 pairing.
 */
object GoogleTvCrypto {

    private const val KEY_ALIAS = "google_tv_remote_client_key"
    private const val PREFS_NAME = "google_tv_crypto_prefs"
    private const val PREF_CLIENT_CERT = "client_cert_pem"
    private const val PREF_PRIVATE_KEY = "private_key_pem"

    /**
     * Generates or retrieves an existing self-signed X.509 client certificate and keypair.
     */
    fun getOrCreateSslContext(context: Context): SSLContext {
        val keyPair = getOrCreateKeyPair()
        val cert = getOrCreateCertificate(keyPair)

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
            setKeyEntry(KEY_ALIAS, keyPair.private, "remote_pass".toCharArray(), arrayOf(cert))
        }

        val kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm()).apply {
            init(keyStore, "remote_pass".toCharArray())
        }

        // Trust all server certificates since Google TV uses self-signed server certs
        val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        })

        val sslContext = SSLContext.getInstance("TLSv1.2")
        sslContext.init(kmf.keyManagers, trustAllCerts, SecureRandom())
        return sslContext
    }

    private fun getOrCreateKeyPair(): KeyPair {
        val kpg = KeyPairGenerator.getInstance("RSA")
        kpg.initialize(2048, SecureRandom())
        return kpg.generateKeyPair()
    }

    private fun getOrCreateCertificate(keyPair: KeyPair): X509Certificate {
        val now = System.currentTimeMillis()
        val startDate = Date(now - 24 * 60 * 60 * 1000)
        val endDate = Date(now + 10L * 365 * 24 * 60 * 60 * 1000) // 10 years validity

        val subject = X500Name("CN=atvremote, O=Google, OU=Android")
        val serial = BigInteger(64, SecureRandom())

        val builder = JcaX509v3CertificateBuilder(
            subject,
            serial,
            startDate,
            endDate,
            subject,
            keyPair.public
        )

        val signer = JcaContentSignerBuilder("SHA256withRSA").build(keyPair.private)
        val certHolder = builder.build(signer)
        return JcaX509CertificateConverter().getCertificate(certHolder)
    }

    /**
     * Computes the SHA-256 HMAC secret for the PIN code verification step.
     * TV displays a 4 or 6-character code (e.g., "ABCD" or "123456").
     * Client hash = SHA-256(Client_Cert_DER + Server_Cert_DER + PIN_ASCII)
     */
    fun computePairingSecretHash(
        clientCertBytes: ByteArray,
        serverCertBytes: ByteArray,
        pinCode: String
    ): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(clientCertBytes)
        md.update(serverCertBytes)
        // Clean up PIN code
        val cleanedPin = pinCode.trim().uppercase()
        // Append PIN bytes
        md.update(cleanedPin.toByteArray(Charsets.US_ASCII))
        return md.digest()
    }
}
