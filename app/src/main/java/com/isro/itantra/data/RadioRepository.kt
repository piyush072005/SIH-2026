package com.isro.itantra.data

import com.isro.itantra.data.db.ITantraDatabase
import com.isro.itantra.data.db.MessageLogEntity
import com.isro.itantra.data.db.OutboxEntity
import com.isro.itantra.data.db.PeerEntity
import com.isro.itantra.domain.contracts.models.DiscoveredPeer
import com.isro.itantra.domain.contracts.models.IncomingMessage
import com.isro.itantra.domain.model.LinkKind
import com.isro.itantra.domain.model.Peer
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RadioRepository @Inject constructor(
    db: ITantraDatabase,
) {
    private val peers = db.peerDao()
    private val outbox = db.outboxDao()
    private val log = db.messageLogDao()

    suspend fun rememberPeer(peer: Peer) {
        peers.upsert(
            PeerEntity(
                id = peer.id,
                displayName = peer.displayName,
                kind = peer.kind.name,
                addressHint = peer.addressHint,
                lastSeenEpochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun loadPeers(): List<DiscoveredPeer> = peers.all().map {
        DiscoveredPeer(id = it.id, name = it.displayName)
    }

    suspend fun saveOutbox(seq: Int, encoded: ByteArray, attempts: Int) {
        outbox.upsert(OutboxEntity(seq, encoded, attempts, System.currentTimeMillis()))
    }

    suspend fun dropOutbox(seq: Int) = outbox.delete(seq)

    suspend fun logOutbound(text: String, isAlert: Boolean, languageCode: String, seq: Int) {
        log.insert(
            MessageLogEntity(
                seq = seq,
                inbound = false,
                isAlert = isAlert,
                languageCode = languageCode,
                text = text,
                epochMs = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun logInbound(message: IncomingMessage) {
        log.insert(
            MessageLogEntity(
                seq = message.id.toIntOrNull() ?: 0,
                inbound = true,
                isAlert = message.isAlert,
                languageCode = message.languageCode,
                text = message.text,
                epochMs = message.timestamp,
            ),
        )
    }
}
