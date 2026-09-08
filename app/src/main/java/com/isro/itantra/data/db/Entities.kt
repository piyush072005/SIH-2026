package com.isro.itantra.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "peers")
data class PeerEntity(
    @PrimaryKey val id: String,
    val displayName: String,
    val kind: String,
    val addressHint: String?,
    val lastSeenEpochMs: Long,
)

@Entity(tableName = "outbox")
data class OutboxEntity(
    @PrimaryKey val seq: Int,
    val encoded: ByteArray,
    val attempts: Int,
    val createdAtMs: Long,
)

@Entity(tableName = "message_log")
data class MessageLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val seq: Int,
    val inbound: Boolean,
    val isAlert: Boolean,
    val languageCode: String,
    val text: String,
    val epochMs: Long,
)
