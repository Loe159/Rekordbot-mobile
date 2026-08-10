package com.loe159.rekordbot.mobile.data.local.shazam

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.loe159.rekordbot.mobile.domain.shazam.ShazamDecision
import kotlinx.coroutines.flow.Flow

data class ShazamInboxCountsProjection(
    val pending: Int,
    val saved: Int,
    val ignored: Int,
    val alreadyPresent: Int,
)

@Dao
interface ShazamInboxDao {
    @Query(
        "SELECT * FROM shazam_inbox_tracks " +
            "ORDER BY playlistAddedAtEpochMillis IS NULL ASC, " +
            "playlistAddedAtEpochMillis DESC, playlistPosition ASC, firstSeenAt DESC",
    )
    fun observeAll(): Flow<List<ShazamInboxEntity>>

    @Query(
        "SELECT * FROM shazam_inbox_tracks WHERE decision = :decision " +
            "ORDER BY playlistAddedAtEpochMillis IS NULL ASC, " +
            "playlistAddedAtEpochMillis DESC, playlistPosition ASC, firstSeenAt DESC",
    )
    fun observeByDecision(decision: ShazamDecision): Flow<List<ShazamInboxEntity>>

    @Query(
        "SELECT " +
            "COALESCE(SUM(CASE WHEN decision = 'PENDING' THEN 1 ELSE 0 END), 0) AS pending, " +
            "COALESCE(SUM(CASE WHEN decision = 'SAVED' THEN 1 ELSE 0 END), 0) AS saved, " +
            "COALESCE(SUM(CASE WHEN decision = 'IGNORED' THEN 1 ELSE 0 END), 0) AS ignored, " +
            "COALESCE(SUM(CASE WHEN decision = 'ALREADY_PRESENT' THEN 1 ELSE 0 END), 0) " +
            "AS alreadyPresent FROM shazam_inbox_tracks",
    )
    fun observeCounts(): Flow<ShazamInboxCountsProjection>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIfAbsent(entity: ShazamInboxEntity): Long

    /** Refreshes source metadata without ever changing the user's decision or first-seen time. */
    @Query(
        "UPDATE shazam_inbox_tracks SET title = :title, artist = :artist, " +
            "spotifyUrl = :spotifyUrl, albumName = COALESCE(:albumName, albumName), " +
            "artworkUrl = COALESCE(:artworkUrl, artworkUrl), isrc = COALESCE(:isrc, isrc), " +
            "playlistAddedAtEpochMillis = COALESCE(:playlistAddedAtEpochMillis, " +
            "playlistAddedAtEpochMillis), playlistPosition = :playlistPosition, " +
            "lastSeenAt = :lastSeenAt WHERE spotifyTrackId = :spotifyTrackId",
    )
    suspend fun refreshSourceMetadata(
        spotifyTrackId: String,
        title: String,
        artist: String,
        spotifyUrl: String,
        albumName: String?,
        artworkUrl: String?,
        isrc: String?,
        playlistAddedAtEpochMillis: Long?,
        playlistPosition: Int,
        lastSeenAt: Long,
    ): Int

    @Transaction
    suspend fun synchronize(entities: List<ShazamInboxEntity>): Int {
        var insertedCount = 0
        entities.forEach { entity ->
            if (insertIfAbsent(entity) != -1L) insertedCount += 1
            refreshSourceMetadata(
                spotifyTrackId = entity.spotifyTrackId,
                title = entity.title,
                artist = entity.artist,
                spotifyUrl = entity.spotifyUrl,
                albumName = entity.albumName,
                artworkUrl = entity.artworkUrl,
                isrc = entity.isrc,
                playlistAddedAtEpochMillis = entity.playlistAddedAtEpochMillis,
                playlistPosition = entity.playlistPosition,
                lastSeenAt = entity.lastSeenAt,
            )
        }
        return insertedCount
    }

    @Query(
        "UPDATE shazam_inbox_tracks SET decision = :decision, decisionUpdatedAt = :now " +
            "WHERE spotifyTrackId = :spotifyTrackId",
    )
    suspend fun markDecision(
        spotifyTrackId: String,
        decision: ShazamDecision,
        now: Long,
    ): Int
}
