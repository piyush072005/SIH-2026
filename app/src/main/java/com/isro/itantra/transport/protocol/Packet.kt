package com.isro.itantra.transport.protocol

enum class PacketType(val code: Byte) {
    Data(1),
    Ack(2),
    Heartbeat(3),
    Alert(4),
    Hello(5),
    Resume(6);

    companion object {
        fun fromCode(code: Byte): PacketType? = entries.firstOrNull { it.code == code }
    }
}

object PacketFlags {
    const val RELIABLE: Int = 0x01
    const val LAST: Int = 0x02
}

data class Packet(
    val type: PacketType,
    val seq: Int,
    val ackSeq: Int,
    val flags: Int,
    val languageCode: String,
    val payloadUtf8: String,
    val version: Int = PacketCodec.VERSION,
)

data class DecodeResult(
    val packet: Packet? = null,
    val error: String? = null,
)
