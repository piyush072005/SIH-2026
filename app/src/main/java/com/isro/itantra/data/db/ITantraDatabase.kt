package com.isro.itantra.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [PeerEntity::class, OutboxEntity::class, MessageLogEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class ITantraDatabase : RoomDatabase() {
    abstract fun peerDao(): PeerDao
    abstract fun outboxDao(): OutboxDao
    abstract fun messageLogDao(): MessageLogDao
}
