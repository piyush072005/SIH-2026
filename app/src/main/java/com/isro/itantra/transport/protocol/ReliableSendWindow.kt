package com.isro.itantra.transport.protocol

data class RetryPolicy(
    val maxAttempts: Int = 5,
    val initialDelayMs: Long = 200L,
    val multiplier: Double = 2.0,
    val maxDelayMs: Long = 2_000L,
) {
    fun delayMsForAttempt(attempt: Int): Long {
        require(attempt >= 1)
        var delay = initialDelayMs.toDouble()
        repeat(attempt - 1) { delay *= multiplier }
        return minOf(delay.toLong(), maxDelayMs)
    }
}

data class InFlightFrame(
    val packet: Packet,
    val encoded: ByteArray,
    val attempts: Int,
    val nextRetryAtMs: Long,
    val createdAtMs: Long,
)

class ReliableSendWindow(
    private val policy: RetryPolicy = RetryPolicy(),
) {
    private val inFlight = LinkedHashMap<Int, InFlightFrame>()
    private var nextSeq: Int = 1
    private var lastAcked: Int = 0

    fun peekNextSeq(): Int = nextSeq

    fun allocateSeq(): Int {
        val seq = nextSeq
        nextSeq += 1
        return seq
    }

    fun track(packet: Packet, encoded: ByteArray, nowMs: Long): InFlightFrame {
        val frame = InFlightFrame(
            packet = packet,
            encoded = encoded,
            attempts = 1,
            nextRetryAtMs = nowMs + policy.delayMsForAttempt(1),
            createdAtMs = nowMs,
        )
        inFlight[packet.seq] = frame
        return frame
    }

    fun onAck(ackSeq: Int): Boolean {
        val removed = inFlight.remove(ackSeq) != null
        if (removed && ackSeq > lastAcked) lastAcked = ackSeq
        return removed
    }

    fun dueForRetry(nowMs: Long): List<InFlightFrame> {
        val due = inFlight.values.filter { nowMs >= it.nextRetryAtMs }
        val updated = mutableListOf<InFlightFrame>()
        for (frame in due) {
            val nextAttempt = frame.attempts + 1
            if (nextAttempt > policy.maxAttempts) continue
            val retry = frame.copy(
                attempts = nextAttempt,
                nextRetryAtMs = nowMs + policy.delayMsForAttempt(nextAttempt),
            )
            inFlight[frame.packet.seq] = retry
            updated += retry
        }
        return updated
    }

    fun expired(nowMs: Long): List<InFlightFrame> {
        val dead = inFlight.values.filter { it.attempts >= policy.maxAttempts && nowMs >= it.nextRetryAtMs }
        dead.forEach { inFlight.remove(it.packet.seq) }
        return dead
    }

    fun snapshotUnacked(): List<InFlightFrame> = inFlight.values.toList()

    fun restore(frames: List<InFlightFrame>, nextSeqValue: Int) {
        inFlight.clear()
        frames.forEach { inFlight[it.packet.seq] = it }
        nextSeq = nextSeqValue
        lastAcked = inFlight.keys.minOrNull()?.minus(1) ?: 0
    }

    fun inFlightCount(): Int = inFlight.size

    fun lastAckedSeq(): Int = lastAcked
}
