package com.isro.itantra.transport.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class LengthPrefixedIoTest {
    @Test
    fun writeThenReadFrame() {
        val payload = byteArrayOf(1, 2, 3, 4)
        val out = ByteArrayOutputStream()
        LengthPrefixedIo.writeFrame(out, payload)
        val read = LengthPrefixedIo.readFrame(ByteArrayInputStream(out.toByteArray()))
        assertThat(read).isEqualTo(payload)
    }

    @Test
    fun decodeAllLeavesRemainder() {
        val first = PacketCodec.encode(Packet(PacketType.Data, 1, 0, 1, "en", "a"))
        val second = PacketCodec.encode(Packet(PacketType.Ack, 0, 1, 0, "", ""))
        val stream = ByteArrayOutputStream()
        LengthPrefixedIo.writeFrame(stream, first)
        LengthPrefixedIo.writeFrame(stream, second)
        val bytes = stream.toByteArray()
        val partial = bytes.copyOf(bytes.size - 3)
        val (frames, rest) = LengthPrefixedIo.decodeAll(partial)
        assertThat(frames).hasSize(1)
        assertThat(PacketCodec.decode(frames[0]).packet!!.seq).isEqualTo(1)
        assertThat(rest).isNotEmpty()
    }
}
