package com.loe159.rekordbot.mobile.domain.shazam

enum class ShazamDecision {
    PENDING,
    SAVED,
    IGNORED,
    ALREADY_PRESENT,
}

/** A Spotify track read from the playlist synchronized by Shazam. */
data class ShazamPlaylistTrack(
    val spotifyTrackId: String,
    val title: String,
    val artist: String,
    val spotifyUrl: String,
    val albumName: String? = null,
    val artworkUrl: String? = null,
    val isrc: String? = null,
    val playlistAddedAtEpochMillis: Long? = null,
    val playlistPosition: Int,
)

/** A durable inbox entry, including the user's decision for this track. */
data class ShazamInboxTrack(
    val spotifyTrackId: String,
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

data class ShazamInboxCounts(
    val pending: Int,
    val saved: Int,
    val ignored: Int,
    val alreadyPresent: Int,
) {
    val total: Int
        get() = pending + saved + ignored + alreadyPresent
}

data class ShazamSyncResult(
    val receivedCount: Int,
    val distinctCount: Int,
    val addedCount: Int,
) {
    val refreshedCount: Int
        get() = distinctCount - addedCount
}
