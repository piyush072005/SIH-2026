package com.isro.itantra.transport.protocol

import com.google.common.truth.Truth.assertThat
import org.junit.Test

class ReliableSendWindowTest {
    private val policy = RetryPolicy(maxAttempts = 3, initialDelayMs = 100, multiplier = 2.0, maxDelayMs = 1_000)

    @Test
    fun delayGrowsThenCaps() {
        assertThat(policy.delayMsForAttempt(1)).isEqualTo(100)
        assertThat(policy.delayMsForAttempt(2)).isEqualTo(200)
        assertThat(policy.delayMsForAttempt(3)).isEqualTo(400)
        val longPolicy = policy.copy(maxDelayMs = 250)
        assertThat(longPolicy.delayMsForAttempt(5)).isEqualTo(250)
    }

    @Test
    fun ackRemovesInFlight() {
        val window = ReliableSendWindow(policy)
        val seq = window.allocateSeq()
        val packet = Packet(PacketType.Data, seq, 0, PacketFlags.RELIABLE, "en", "ping")
        window.track(packet, PacketCodec.encode(packet), nowMs = 0)
        assertThat(window.inFlightCount()).isEqualTo(1)
        assertThat(window.onAck(seq)).isTrue()
        assertThat(window.inFlightCount()).isEqualTo(0)
        assertThat(window.lastAckedSeq()).isEqualTo(seq)
    }

    @Test
    fun retriesThenExpiresAfterMaxAttempts() {
        val window = ReliableSendWindow(policy)
        val seq = window.allocateSeq()
        val packet = Packet(PacketType.Data, seq, 0, PacketFlags.RELIABLE, "en", "drop-me")
        window.track(packet, PacketCodec.encode(packet), nowMs = 0)

        val first = window.dueForRetry(nowMs = 100)
        assertThat(first).hasSize(1)
        assertThat(first[0].attempts).isEqualTo(2)

        val second = window.dueForRetry(nowMs = 300)
        assertThat(second).hasSize(1)
        assertThat(second[0].attempts).isEqualTo(3)

        assertThat(window.dueForRetry(nowMs = 700)).isEmpty()
        val dead = window.expired(nowMs = 700)
        assertThat(dead).hasSize(1)
        assertThat(window.inFlightCount()).isEqualTo(0)
    }

    @Test
    fun restoreReplaysUnackedOnReconnect() {
        val window = ReliableSendWindow(policy)
        val seq = window.allocateSeq()
        val packet = Packet(PacketType.Data, seq, 0, PacketFlags.RELIABLE, "hi", "नमस्ते")
        val encoded = PacketCodec.encode(packet)
        window.track(packet, encoded, nowMs = 10)
        val snapshot = window.snapshotUnacked()
        val restored = ReliableSendWindow(policy)
        restored.restore(snapshot, nextSeqValue = seq + 1)
        assertThat(restored.snapshotUnacked()).hasSize(1)
        assertThat(restored.peekNextSeq()).isEqualTo(seq + 1)
    }
}
