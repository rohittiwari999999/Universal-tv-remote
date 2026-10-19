package com.omnitech.googletv.remote.protocol

import java.io.ByteArrayOutputStream
import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * High-performance, zero-dependency Protobuf wire protocol encoder & decoder
 * for Google TV / Android TV Remote Protocol v2 & Polo Pairing Protocol.
 */
object GoogleTvProtobuf {

    // --- Message Framing over TLS Socket (Varint length prefix) ---

    fun writeFramedMessage(out: OutputStream, payload: ByteArray) {
        writeVarint(out, payload.size.toLong())
        out.write(payload)
        out.flush()
    }

    fun readFramedMessage(input: InputStream): ByteArray {
        var length = 0L
        var shift = 0
        while (shift < 64) {
            val b = input.read()
            if (b == -1) throw EOFException("Socket closed by Google TV")
            length = length or ((b and 0x7F).toLong() shl shift)
            if ((b and 0x80) == 0) break
            shift += 7
        }
        if (length < 0 || length > 2 * 1024 * 1024) {
            throw IOException("Invalid framed protobuf message length: $length")
        }
        val buffer = ByteArray(length.toInt())
        var totalRead = 0
        while (totalRead < buffer.size) {
            val bytesRead = input.read(buffer, totalRead, buffer.size - totalRead)
            if (bytesRead == -1) throw EOFException("Premature EOF reading protobuf message ($totalRead/${buffer.size})")
            totalRead += bytesRead
        }
        return buffer
    }

    fun writeVarint(out: OutputStream, value: Long) {
        var v = value
        while ((v and 0x7FL.inv()) != 0L) {
            out.write(((v and 0x7F) or 0x80).toInt())
            v = v ushr 7
        }
        out.write((v and 0x7F).toInt())
    }

    // --- Polo Pairing Protocol Messages (Port 6467) ---

    /**
     * Step 1: Send PairingRequest
     */
    fun buildPairingRequest(serviceName: String = "atvremote", clientName: String = "Google TV Remote"): ByteArray {
        val req = ProtoWriter().apply {
            writeString(1, serviceName)
            writeString(2, clientName)
        }.toByteArray()

        return ProtoWriter().apply {
            writeInt32(1, 2) // protocol_version = 2
            writeInt32(2, 200) // status = 200 (STATUS_OK)
            writeMessage(10, req) // field 10: pairing_request
        }.toByteArray()
    }

    /**
     * Step 2: Send PairingOption (Hexadecimal encoding, 6 symbols, ROLE_TYPE_INPUT)
     */
    fun buildPairingOption(): ByteArray {
        val enc = ProtoWriter().apply {
            writeInt32(1, 3) // type: ENCODING_TYPE_HEXADECIMAL (3)
            writeInt32(2, 6) // symbol_length: 6
        }.toByteArray()

        val opt = ProtoWriter().apply {
            writeMessage(1, enc) // input_encodings
            writeInt32(3, 1) // preferred_role: ROLE_TYPE_INPUT (1)
        }.toByteArray()

        return ProtoWriter().apply {
            writeInt32(1, 2) // protocol_version: 2
            writeInt32(2, 200) // status: 200
            writeMessage(20, opt) // field 20: pairing_option
        }.toByteArray()
    }

    /**
     * Step 3: Send PairingConfiguration
     */
    fun buildPairingConfiguration(): ByteArray {
        val enc = ProtoWriter().apply {
            writeInt32(1, 3) // type: ENCODING_TYPE_HEXADECIMAL (3)
            writeInt32(2, 6) // symbol_length: 6
        }.toByteArray()

        val config = ProtoWriter().apply {
            writeMessage(1, enc) // encoding
            writeInt32(2, 1) // client_role: ROLE_TYPE_INPUT (1)
        }.toByteArray()

        return ProtoWriter().apply {
            writeInt32(1, 2)
            writeInt32(2, 200)
            writeMessage(30, config) // field 30: pairing_configuration
        }.toByteArray()
    }

    /**
     * Step 4: Send PairingSecret (SHA-256 hash calculated with user PIN)
     */
    fun buildPairingSecret(secretHash: ByteArray): ByteArray {
        val sec = ProtoWriter().apply {
            writeBytes(1, secretHash)
        }.toByteArray()

        return ProtoWriter().apply {
            writeInt32(1, 2)
            writeInt32(2, 200)
            writeMessage(40, sec) // field 40: pairing_secret
        }.toByteArray()
    }

    /**
     * Polo Response info holder
     */
    data class PoloMessage(
        val status: Int = 200,
        val hasPairingRequestAck: Boolean = false,
        val hasPairingOption: Boolean = false,
        val hasConfigurationAck: Boolean = false,
        val hasSecretAck: Boolean = false
    )

    fun parsePoloMessage(rawBytes: ByteArray): PoloMessage {
        val reader = ProtoReader(rawBytes)
        var status = 200
        var hasReqAck = false
        var hasOpt = false
        var hasConfigAck = false
        var hasSecAck = false

        while (reader.hasMore()) {
            val (fieldNum, wireType) = reader.readTag() ?: break
            when (fieldNum) {
                2 -> status = reader.readVarint().toInt()
                11 -> {
                    hasReqAck = true
                    reader.skipField(wireType)
                }
                20 -> {
                    hasOpt = true
                    reader.skipField(wireType)
                }
                31 -> {
                    hasConfigAck = true
                    reader.skipField(wireType)
                }
                41 -> {
                    hasSecAck = true
                    reader.skipField(wireType)
                }
                else -> reader.skipField(wireType)
            }
        }

        return PoloMessage(
            status = status,
            hasPairingRequestAck = hasReqAck,
            hasPairingOption = hasOpt,
            hasConfigurationAck = hasConfigAck,
            hasSecretAck = hasSecAck
        )
    }

    // --- Remote Control Messages (Port 6466) ---

    fun buildRemoteConfigure(features: Int = 622): ByteArray {
        val devInfo = ProtoWriter().apply {
            writeInt32(3, 1)
            writeString(4, "1")
            writeString(5, "atvremote")
            writeString(6, "1.0.0")
        }.toByteArray()

        val conf = ProtoWriter().apply {
            writeInt32(1, features)
            writeMessage(2, devInfo)
        }.toByteArray()

        return ProtoWriter().apply {
            writeMessage(1, conf) // field 1: remote_configure
        }.toByteArray()
    }

    fun buildRemoteSetActive(features: Int = 622): ByteArray {
        val act = ProtoWriter().apply {
            writeInt32(1, features)
        }.toByteArray()

        return ProtoWriter().apply {
            writeMessage(2, act) // field 2: remote_set_active
        }.toByteArray()
    }

    fun buildRemotePingResponse(val1: Int): ByteArray {
        val ping = ProtoWriter().apply {
            writeInt32(1, val1)
        }.toByteArray()

        return ProtoWriter().apply {
            writeMessage(9, ping) // field 9: remote_ping_response
        }.toByteArray()
    }

    fun buildRemoteKeyInject(keyCode: Int, direction: Int = 3): ByteArray {
        val keyInj = ProtoWriter().apply {
            writeInt32(1, keyCode)
            writeInt32(2, direction) // 3 = SHORT
        }.toByteArray()

        return ProtoWriter().apply {
            writeMessage(10, keyInj) // field 10: remote_key_inject
        }.toByteArray()
    }

    fun buildRemoteAppLink(url: String): ByteArray {
        val app = ProtoWriter().apply {
            writeString(1, url)
        }.toByteArray()

        return ProtoWriter().apply {
            writeMessage(90, app) // field 90: remote_app_link_launch_request
        }.toByteArray()
    }

    fun buildRemoteImeBatchEdit(text: String, imeCounter: Int, fieldCounter: Int): ByteArray {
        val paramVal = (text.length - 1).coerceAtLeast(0)
        val imeObj = ProtoWriter().apply {
            writeInt32(1, paramVal)
            writeInt32(2, paramVal)
            writeString(3, text)
        }.toByteArray()

        val editInfo = ProtoWriter().apply {
            writeInt32(1, 1)
            writeMessage(2, imeObj)
        }.toByteArray()

        val batch = ProtoWriter().apply {
            writeInt32(1, imeCounter)
            writeInt32(2, fieldCounter)
            writeMessage(3, editInfo)
        }.toByteArray()

        return ProtoWriter().apply {
            writeMessage(21, batch) // field 21: remote_ime_batch_edit
        }.toByteArray()
    }

    /**
     * Remote Message info holder
     */
    data class RemoteIncomingMessage(
        val hasRemoteConfigure: Boolean = false,
        val hasRemoteSetActive: Boolean = false,
        val hasRemoteStart: Boolean = false,
        val pingVal1: Int? = null,
        val currentAppPackage: String? = null,
        val imeCounter: Int? = null,
        val imeFieldCounter: Int? = null
    )

    fun parseRemoteMessage(rawBytes: ByteArray): RemoteIncomingMessage {
        val reader = ProtoReader(rawBytes)
        var hasConfigure = false
        var hasSetActive = false
        var hasStart = false
        var pingVal1: Int? = null
        var currentApp: String? = null
        var imeCounter: Int? = null
        var imeFieldCounter: Int? = null

        while (reader.hasMore()) {
            val (fieldNum, wireType) = reader.readTag() ?: break
            when (fieldNum) {
                1 -> {
                    hasConfigure = true
                    reader.skipField(wireType)
                }
                2 -> {
                    hasSetActive = true
                    reader.skipField(wireType)
                }
                8 -> {
                    // remote_ping_request
                    val pingBytes = reader.readBytes()
                    val pReader = ProtoReader(pingBytes)
                    while (pReader.hasMore()) {
                        val (pField, pWire) = pReader.readTag() ?: break
                        if (pField == 1) {
                            pingVal1 = pReader.readVarint().toInt()
                        } else {
                            pReader.skipField(pWire)
                        }
                    }
                }
                20 -> {
                    // remote_ime_key_inject (contains app package)
                    val imeBytes = reader.readBytes()
                    try {
                        val subReader = ProtoReader(imeBytes)
                        while (subReader.hasMore()) {
                            val (subField, subWire) = subReader.readTag() ?: break
                            if (subField == 1) { // app_info
                                val appInfoBytes = subReader.readBytes()
                                val appReader = ProtoReader(appInfoBytes)
                                while (appReader.hasMore()) {
                                    val (aField, aWire) = appReader.readTag() ?: break
                                    if (aField == 12) {
                                        currentApp = appReader.readString()
                                    } else {
                                        appReader.skipField(aWire)
                                    }
                                }
                            } else {
                                subReader.skipField(subWire)
                            }
                        }
                    } catch (ignored: Exception) {}
                }
                21 -> {
                    // remote_ime_batch_edit
                    val bBytes = reader.readBytes()
                    try {
                        val bReader = ProtoReader(bBytes)
                        while (bReader.hasMore()) {
                            val (bField, bWire) = bReader.readTag() ?: break
                            if (bField == 1) imeCounter = bReader.readVarint().toInt()
                            else if (bField == 2) imeFieldCounter = bReader.readVarint().toInt()
                            else bReader.skipField(bWire)
                        }
                    } catch (ignored: Exception) {}
                }
                40 -> {
                    // remote_start
                    hasStart = true
                    reader.skipField(wireType)
                }
                else -> reader.skipField(wireType)
            }
        }

        return RemoteIncomingMessage(
            hasRemoteConfigure = hasConfigure,
            hasRemoteSetActive = hasSetActive,
            hasRemoteStart = hasStart,
            pingVal1 = pingVal1,
            currentAppPackage = currentApp,
            imeCounter = imeCounter,
            imeFieldCounter = imeFieldCounter
        )
    }

    // --- Helper Proto Writer & Reader ---

    class ProtoWriter {
        private val bos = ByteArrayOutputStream()

        fun writeVarint(value: Long) {
            var v = value
            while ((v and 0x7FL.inv()) != 0L) {
                bos.write(((v and 0x7F) or 0x80).toInt())
                v = v ushr 7
            }
            bos.write((v and 0x7F).toInt())
        }

        fun writeTag(fieldNumber: Int, wireType: Int) {
            writeVarint(((fieldNumber shl 3) or wireType).toLong())
        }

        fun writeInt32(fieldNumber: Int, value: Int) {
            writeTag(fieldNumber, 0)
            writeVarint(value.toLong())
        }

        fun writeString(fieldNumber: Int, value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            writeTag(fieldNumber, 2)
            writeVarint(bytes.size.toLong())
            bos.write(bytes)
        }

        fun writeBytes(fieldNumber: Int, bytes: ByteArray) {
            writeTag(fieldNumber, 2)
            writeVarint(bytes.size.toLong())
            bos.write(bytes)
        }

        fun writeMessage(fieldNumber: Int, messageBytes: ByteArray) {
            writeTag(fieldNumber, 2)
            writeVarint(messageBytes.size.toLong())
            bos.write(messageBytes)
        }

        fun toByteArray(): ByteArray = bos.toByteArray()
    }

    class ProtoReader(private val bytes: ByteArray) {
        private var pos = 0

        fun hasMore(): Boolean = pos < bytes.size

        fun readVarint(): Long {
            var result = 0L
            var shift = 0
            while (shift < 64) {
                if (pos >= bytes.size) throw EOFException("Unexpected end of proto buffer")
                val b = bytes[pos++].toInt() and 0xFF
                result = result or ((b and 0x7F).toLong() shl shift)
                if ((b and 0x80) == 0) return result
                shift += 7
            }
            return result
        }

        fun readTag(): Pair<Int, Int>? {
            if (!hasMore()) return null
            val tag = readVarint().toInt()
            val fieldNumber = tag ushr 3
            val wireType = tag and 0x07
            return Pair(fieldNumber, wireType)
        }

        fun readBytes(): ByteArray {
            val len = readVarint().toInt()
            if (pos + len > bytes.size) throw EOFException("Buffer underflow reading bytes")
            val slice = bytes.copyOfRange(pos, pos + len)
            pos += len
            return slice
        }

        fun readString(): String = String(readBytes(), Charsets.UTF_8)

        fun skipField(wireType: Int) {
            when (wireType) {
                0 -> readVarint()
                1 -> pos += 8
                2 -> {
                    val len = readVarint().toInt()
                    pos += len
                }
                5 -> pos += 4
                else -> throw IOException("Unsupported wire type: $wireType")
            }
        }
    }
}
