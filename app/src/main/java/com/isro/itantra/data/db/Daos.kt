package com.isro.itantra.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PeerDao {
    @Query("SELECT * FROM peers ORDER BY lastSeenEpochMs DESC")
    suspend fun all(): List<PeerEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PeerEntity)
}

@Dao
interface OutboxDao {
    @Query("SELECT * FROM outbox ORDER BY seq ASC")
    suspend fun all(): List<OutboxEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OutboxEntity)

    @Query("DELETE FROM outbox WHERE seq = :seq")
    suspend fun delete(seq: Int)

    @Query("DELETE FROM outbox")
    suspend fun clear()
}

@Dao
interface MessageLogDao {
    @Insert
    suspend fun insert(entity: MessageLogEntity)

    @Query("SELECT * FROM message_log ORDER BY epochMs DESC LIMIT :limit")
    suspend fun recent(limit: Int): List<MessageLogEntity>
}
