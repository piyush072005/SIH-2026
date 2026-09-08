package com.isro.itantra.transport.protocol

import java.io.EOFException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

object LengthPrefixedIo {
    fun writeFrame(out: OutputStream, payload: ByteArray) {
        val header = ByteBuffer.allocate(4).order(ByteOrder.BIG_ENDIAN).putInt(payload.size).array()
        out.write(header)
        out.write(payload)
        out.flush()
    }

    fun readFrame(input: InputStream, maxSize: Int = PacketCodec.MAX_PAYLOAD + 64): ByteArray {
        val header = readFully(input, 4)
        val size = ByteBuffer.wrap(header).order(ByteOrder.BIG_ENDIAN).int
        if (size < 0 || size > maxSize) {
            throw IllegalArgumentException("illegal frame size $size")
        }
        return readFully(input, size)
    }

    fun decodeAll(buffer: ByteArray): Pair<List<ByteArray>, ByteArray> {
        val frames = mutableListOf<ByteArray>()
        var offset = 0
        while (offset + 4 <= buffer.size) {
            val size = ByteBuffer.wrap(buffer, offset, 4).order(ByteOrder.BIG_ENDIAN).int
            if (size < 0 || offset + 4 + size > buffer.size) break
            frames += buffer.copyOfRange(offset + 4, offset + 4 + size)
            offset += 4 + size
        }
        return frames to buffer.copyOfRange(offset, buffer.size)
    }

    private fun readFully(input: InputStream, count: Int): ByteArray {
        val data = ByteArray(count)
        var read = 0
        while (read < count) {
            val n = input.read(data, read, count - read)
            if (n < 0) throw EOFException("stream closed")
            read += n
        }
        return data
    }
}
