package com.loe159.rekordbot.mobile.data.local.queue

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.queue.QueueStatus
import com.loe159.rekordbot.mobile.domain.queue.QueuedTrackOperation

@Entity(tableName = "queued_tracks")
data class QueuedTrackEntity(
    @PrimaryKey val operationId: String,
    val spotifyTrackId: String,
    val title: String,
    val artist: String,
    val spotifyUrl: String,
    val rawGenre: String?,
    val comment: String?,
    val status: QueueStatus,
    val attemptCount: Int,
    val lastError: String?,
    val retryable: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAttemptAt: Long?,
    val sentAt: Long?,
    val airtableRecordId: String?,
)

internal fun QueuedTrackEntity.toDomain(): QueuedTrackOperation = QueuedTrackOperation(
    operationId = operationId,
    draft = TrackDraft(
        spotifyTrackId = spotifyTrackId,
        title = title,
        artist = artist,
        spotifyUrl = spotifyUrl,
        rawGenre = rawGenre,
        comment = comment,
    ),
    status = status,
    attemptCount = attemptCount,
    lastError = lastError,
    willRetryAutomatically = retryable,
    createdAt = createdAt,
    updatedAt = updatedAt,
    lastAttemptAt = lastAttemptAt,
    sentAt = sentAt,
    airtableRecordId = airtableRecordId,
)

internal fun TrackDraft.toEntity(
    operationId: String,
    initialError: String,
    now: Long,
): QueuedTrackEntity = QueuedTrackEntity(
    operationId = operationId,
    spotifyTrackId = spotifyTrackId,
    title = title,
    artist = artist,
    spotifyUrl = spotifyUrl,
    rawGenre = rawGenre,
    comment = comment,
    status = QueueStatus.PENDING,
    attemptCount = 0,
    lastError = initialError.takeIf(String::isNotBlank),
    retryable = true,
    createdAt = now,
    updatedAt = now,
    lastAttemptAt = null,
    sentAt = null,
    airtableRecordId = null,
)
