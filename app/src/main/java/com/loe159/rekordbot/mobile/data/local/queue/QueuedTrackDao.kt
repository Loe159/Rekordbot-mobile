package com.loe159.rekordbot.mobile.data.local.queue

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface QueuedTrackDao {
    @Query("SELECT * FROM queued_tracks ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<QueuedTrackEntity>>

    @Query("SELECT COUNT(*) FROM queued_tracks WHERE status != 'SENT'")
    fun observeOpenCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: QueuedTrackEntity): Long

    @Query(
        "SELECT * FROM queued_tracks " +
            "WHERE status = 'PENDING' OR (status = 'FAILED' AND retryable = 1) " +
            "ORDER BY createdAt ASC LIMIT 1",
    )
    suspend fun nextSendable(): QueuedTrackEntity?

    @Query("SELECT * FROM queued_tracks WHERE operationId = :operationId")
    suspend fun get(operationId: String): QueuedTrackEntity?

    @Query(
        "UPDATE queued_tracks SET status = 'SENDING', attemptCount = attemptCount + 1, " +
            "lastError = NULL, retryable = 0, lastAttemptAt = :now, updatedAt = :now " +
            "WHERE operationId = :operationId AND " +
            "(status = 'PENDING' OR (status = 'FAILED' AND retryable = 1))",
    )
    suspend fun markSending(operationId: String, now: Long): Int

    @Transaction
    suspend fun claimNext(now: Long): QueuedTrackEntity? {
        val candidate = nextSendable() ?: return null
        if (markSending(candidate.operationId, now) != 1) return null
        return get(candidate.operationId)
    }

    @Query(
        "UPDATE queued_tracks SET status = 'SENT', airtableRecordId = :recordId, " +
            "lastError = NULL, sentAt = :now, updatedAt = :now " +
            "WHERE operationId = :operationId AND status = 'SENDING'",
    )
    suspend fun markSent(operationId: String, recordId: String, now: Long): Int

    @Query(
        "UPDATE queued_tracks SET status = 'FAILED', lastError = :error, retryable = :retryable, " +
            "updatedAt = :now " +
            "WHERE operationId = :operationId AND status = 'SENDING'",
    )
    suspend fun markFailed(operationId: String, error: String, retryable: Boolean, now: Long): Int

    @Query(
        "UPDATE queued_tracks SET status = 'PENDING', lastError = NULL, retryable = 0, " +
            "updatedAt = :now " +
            "WHERE operationId = :operationId AND status = 'FAILED'",
    )
    suspend fun retry(operationId: String, now: Long): Int

    @Query("DELETE FROM queued_tracks WHERE operationId = :operationId AND status != 'SENDING'")
    suspend fun delete(operationId: String): Int

    @Query(
        "UPDATE queued_tracks SET spotifyTrackId = :spotifyTrackId, title = :title, " +
            "artist = :artist, spotifyUrl = :spotifyUrl, rawGenre = :rawGenre, " +
            "isrc = :isrc, " +
            "energy = :energy, moods = :moods, situations = :situations, " +
            "inspirationalDjs = :inspirationalDjs, comment = :comment, updatedAt = :now " +
            "WHERE operationId = :operationId AND status IN ('DRAFT', 'PENDING', 'FAILED')",
    )
    suspend fun updateDraft(
        operationId: String,
        spotifyTrackId: String,
        title: String,
        artist: String,
        spotifyUrl: String,
        isrc: String?,
        rawGenre: String?,
        energy: Int?,
        moods: List<String>,
        situations: List<String>,
        inspirationalDjs: List<String>,
        comment: String?,
        now: Long,
    ): Int

    @Query(
        "UPDATE queued_tracks SET status = 'PENDING', lastError = NULL, retryable = 0, " +
            "updatedAt = :now WHERE operationId = :operationId AND status = 'DRAFT'",
    )
    suspend fun sendDraft(operationId: String, now: Long): Int

    @Query(
        "UPDATE queued_tracks SET status = 'PENDING', " +
            "lastError = 'Envoi interrompu, nouvelle tentative planifiée.', retryable = 1, " +
            "updatedAt = :now " +
            "WHERE status = 'SENDING'",
    )
    suspend fun recoverInterrupted(now: Long): Int
}
