package com.loe159.rekordbot.mobile.data.local.shazam

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.TypeConverter
import com.loe159.rekordbot.mobile.domain.shazam.ShazamDecision
import com.loe159.rekordbot.mobile.domain.shazam.ShazamInboxTrack
import com.loe159.rekordbot.mobile.domain.shazam.ShazamPlaylistTrack

@Entity(
    tableName = "shazam_inbox_tracks",
    indices = [
        Index(value = ["decision"]),
        Index(value = ["playlistAddedAtEpochMillis", "playlistPosition", "firstSeenAt"]),
    ],
)
data class ShazamInboxEntity(
    @PrimaryKey val spotifyTrackId: String,
    val title: String,
    val artist: String,
    val spotifyUrl: String,
    val albumName: String?,
    val artworkUrl: String?,
    val isrc: String?,
    val playlistAddedAtEpochMillis: Long?,
    val playlistPosition: Int,
    val decision: ShazamDecision,
    val decisionUpdatedAt: Long?,
    val firstSeenAt: Long,
    val lastSeenAt: Long,
)

internal fun ShazamInboxEntity.toDomain(): ShazamInboxTrack = ShazamInboxTrack(
    spotifyTrackId = spotifyTrackId,
    title = title,
    artist = artist,
    spotifyUrl = spotifyUrl,
    albumName = albumName,
    artworkUrl = artworkUrl,
    isrc = isrc,
    playlistAddedAtEpochMillis = playlistAddedAtEpochMillis,
    playlistPosition = playlistPosition,
    decision = decision,
    decisionUpdatedAt = decisionUpdatedAt,
    firstSeenAt = firstSeenAt,
    lastSeenAt = lastSeenAt,
)

internal fun ShazamPlaylistTrack.toEntity(now: Long): ShazamInboxEntity = ShazamInboxEntity(
    spotifyTrackId = spotifyTrackId,
    title = title,
    artist = artist,
    spotifyUrl = spotifyUrl,
    albumName = albumName,
    artworkUrl = artworkUrl,
    isrc = isrc,
    playlistAddedAtEpochMillis = playlistAddedAtEpochMillis,
    playlistPosition = playlistPosition,
    decision = ShazamDecision.PENDING,
    decisionUpdatedAt = null,
    firstSeenAt = now,
    lastSeenAt = now,
)

class ShazamConverters {
    @TypeConverter
    fun shazamDecisionToString(decision: ShazamDecision): String = decision.name

    @TypeConverter
    fun stringToShazamDecision(value: String): ShazamDecision = ShazamDecision.valueOf(value)
}
