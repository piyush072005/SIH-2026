package com.isro.itantra.transport.protocol

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32

/**
 * Binary frame:
 * magic(4) version(1) type(1) flags(1) reserved(1)
 * seq(4 BE) ackSeq(4 BE) langLen(1) lang(utf8) payloadLen(2 BE) payload(utf8) crc32(4 BE)
 *
 * CRC covers all bytes before the CRC field.
 */
object PacketCodec {
    const val MAGIC: Int = 0x49544E54 // "ITNT"
    const val VERSION: Int = 1
    /** Bytes before variable-length lang and payload: magic..ackSeq + langLen + payloadLen. */
    const val HEADER_FIXED: Int = 19
    const val MAX_PAYLOAD: Int = 8 * 1024

    fun encode(packet: Packet): ByteArray {
        val lang = packet.languageCode.encodeToByteArray()
        require(lang.size <= 16) { "languageCode too long" }
        val payload = packet.payloadUtf8.encodeToByteArray()
        require(payload.size <= MAX_PAYLOAD) { "payload too large" }

        val withoutCrc = HEADER_FIXED + lang.size + payload.size
        val buffer = ByteBuffer.allocate(withoutCrc + 4).order(ByteOrder.BIG_ENDIAN)
        buffer.putInt(MAGIC)
        buffer.put(VERSION.toByte())
        buffer.put(packet.type.code)
        buffer.put(packet.flags.toByte())
        buffer.put(0)
        buffer.putInt(packet.seq)
        buffer.putInt(packet.ackSeq)
        buffer.put(lang.size.toByte())
        buffer.put(lang)
        buffer.putShort(payload.size.toShort())
        buffer.put(payload)
        val crc = crc32(buffer.array(), 0, withoutCrc)
        buffer.putInt(crc)
        return buffer.array()
    }

    fun decode(bytes: ByteArray, offset: Int = 0, length: Int = bytes.size): DecodeResult {
        if (length < HEADER_FIXED + 4) {
            return DecodeResult(error = "truncated")
        }
        val buffer = ByteBuffer.wrap(bytes, offset, length).order(ByteOrder.BIG_ENDIAN)
        val magic = buffer.int
        if (magic != MAGIC) return DecodeResult(error = "bad_magic")
        val version = buffer.get().toInt() and 0xFF
        if (version != VERSION) return DecodeResult(error = "bad_version")
        val type = PacketType.fromCode(buffer.get()) ?: return DecodeResult(error = "bad_type")
        val flags = buffer.get().toInt() and 0xFF
        buffer.get() // reserved
        val seq = buffer.int
        val ackSeq = buffer.int
        val langLen = buffer.get().toInt() and 0xFF
        if (buffer.remaining() < langLen + 2 + 4) return DecodeResult(error = "truncated_lang")
        val langBytes = ByteArray(langLen)
        buffer.get(langBytes)
        val payloadLen = buffer.short.toInt() and 0xFFFF
        if (payloadLen > MAX_PAYLOAD) return DecodeResult(error = "payload_too_large")
        if (buffer.remaining() < payloadLen + 4) return DecodeResult(error = "truncated_payload")
        val payloadBytes = ByteArray(payloadLen)
        buffer.get(payloadBytes)
        val crcOffset = buffer.position()
        val expected = buffer.int
        val actual = crc32(bytes, offset, crcOffset - offset)
        if (expected != actual) return DecodeResult(error = "bad_crc")
        return DecodeResult(
            packet = Packet(
                type = type,
                seq = seq,
                ackSeq = ackSeq,
                flags = flags,
                languageCode = langBytes.decodeToString(),
                payloadUtf8 = payloadBytes.decodeToString(),
                version = version,
            ),
        )
    }

    private fun crc32(data: ByteArray, offset: Int, length: Int): Int {
        val crc = CRC32()
        crc.update(data, offset, length)
        return crc.value.toInt()
    }
}
