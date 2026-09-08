package com.isro.itantra.transport

import com.isro.itantra.di.IoDispatcher
import com.isro.itantra.transport.protocol.LengthPrefixedIo
import com.isro.itantra.transport.protocol.Packet
import com.isro.itantra.transport.protocol.PacketCodec
import com.isro.itantra.transport.protocol.PacketFlags
import com.isro.itantra.transport.protocol.PacketType
import com.isro.itantra.transport.protocol.ReliableSendWindow
import com.isro.itantra.transport.protocol.RetryPolicy
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReliableSocketSession @Inject constructor(
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val window = ReliableSendWindow(RetryPolicy())
    private val writeMutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + io)
    private var retryJob: Job? = null
    private var readerJob: Job? = null
    private var link: ByteLink? = null
    var listener: LinkListener? = null

    fun attach(newLink: ByteLink) {
        detach()
        link = newLink
        readerJob = scope.launch { readLoop(newLink) }
        retryJob = scope.launch { retryLoop() }
        replayUnacked()
    }

    fun detach() {
        retryJob?.cancel()
        readerJob?.cancel()
        retryJob = null
        readerJob = null
        link?.close()
        link = null
    }

    suspend fun sendReliable(
        type: PacketType,
        languageCode: String,
        text: String,
        isAlert: Boolean,
    ): Int = withContext(io) {
        val seq = window.allocateSeq()
        val flags = PacketFlags.RELIABLE or PacketFlags.LAST
        val packet = Packet(
            type = type,
            seq = seq,
            ackSeq = window.lastAckedSeq(),
            flags = flags,
            languageCode = languageCode,
            payloadUtf8 = text,
        )
        val encoded = PacketCodec.encode(packet)
        window.track(packet, encoded, nowMs())
        writeRaw(encoded)
        seq
    }

    fun snapshotUnacked() = window.snapshotUnacked()

    private fun replayUnacked() {
        scope.launch {
            window.snapshotUnacked().forEach { writeRaw(it.encoded) }
        }
    }

    private suspend fun readLoop(active: ByteLink) {
        try {
            val input = active.input()
            while (scope.isActive && active.isOpen) {
                val frame = LengthPrefixedIo.readFrame(input)
                val decoded = PacketCodec.decode(frame)
                val packet = decoded.packet ?: continue
                if (packet.type == PacketType.Ack) {
                    window.onAck(packet.ackSeq)
                    continue
                }
                if (packet.flags and PacketFlags.RELIABLE != 0) {
                    writeAck(packet.seq)
                }
                listener?.onPacket(packet)
            }
        } catch (t: Throwable) {
            listener?.onDisconnected(t.message ?: "link_closed")
        }
    }

    private suspend fun retryLoop() {
        while (scope.isActive) {
            delay(50)
            val now = nowMs()
            window.dueForRetry(now).forEach { writeRaw(it.encoded) }
            val dead = window.expired(now)
            if (dead.isNotEmpty()) {
                listener?.onDisconnected("retry_exhausted seq=${dead.joinToString { it.packet.seq.toString() }}")
            }
        }
    }

    private suspend fun writeAck(seq: Int) {
        val ack = Packet(
            type = PacketType.Ack,
            seq = 0,
            ackSeq = seq,
            flags = 0,
            languageCode = "",
            payloadUtf8 = "",
        )
        writeRaw(PacketCodec.encode(ack))
    }

    private suspend fun writeRaw(bytes: ByteArray) {
        val current = link ?: return
        writeMutex.withLock {
            runCatching { LengthPrefixedIo.writeFrame(current.output(), bytes) }
        }
    }

    private fun nowMs(): Long = System.currentTimeMillis()
}
