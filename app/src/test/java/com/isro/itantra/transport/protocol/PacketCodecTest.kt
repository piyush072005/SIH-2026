package com.isro.itantra.transport.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class PacketCodecTest {
    @Test
    fun roundTripHindiAlert() {
        val original = Packet(
            type = PacketType.Alert,
            seq = 42,
            ackSeq = 7,
            flags = PacketFlags.RELIABLE or PacketFlags.LAST,
            languageCode = "hi",
            payloadUtf8 = "आपातकालीन संदेश",
        )
        val encoded = PacketCodec.encode(original)
        val decoded = PacketCodec.decode(encoded).packet
        assertThat(decoded).isNotNull()
        assertThat(decoded!!.type).isEqualTo(PacketType.Alert)
        assertThat(decoded.seq).isEqualTo(42)
        assertThat(decoded.ackSeq).isEqualTo(7)
        assertThat(decoded.flags).isEqualTo(PacketFlags.RELIABLE or PacketFlags.LAST)
        assertThat(decoded.languageCode).isEqualTo("hi")
        assertThat(decoded.payloadUtf8).isEqualTo("आपातकालीन संदेश")
    }

    @Test
    fun rejectsBadMagic() {
        val encoded = PacketCodec.encode(
            Packet(PacketType.Data, 1, 0, 0, "en", "hello"),
        )
        encoded[0] = 0
        assertThat(PacketCodec.decode(encoded).error).isEqualTo("bad_magic")
    }

    @Test
    fun rejectsBadCrc() {
        val encoded = PacketCodec.encode(
            Packet(PacketType.Data, 1, 0, 0, "en", "hello"),
        )
        encoded[encoded.lastIndex] = (encoded.last() + 1).toByte()
        assertThat(PacketCodec.decode(encoded).error).isEqualTo("bad_crc")
    }

    @Test
    fun rejectsTruncated() {
        assertThat(PacketCodec.decode(ByteArray(8)).error).isEqualTo("truncated")
    }
}
