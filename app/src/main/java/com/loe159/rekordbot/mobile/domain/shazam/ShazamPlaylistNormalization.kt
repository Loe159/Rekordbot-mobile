package com.loe159.rekordbot.mobile.domain.shazam

/**
 * Collapses duplicate Spotify tracks while retaining their most recent playlist occurrence.
 * Spotify playlists may legally contain the same track more than once, whereas the inbox is
 * deliberately keyed by Spotify Track ID.
 */
internal fun List<ShazamPlaylistTrack>.distinctForInbox(): List<ShazamPlaylistTrack> {
    val bySpotifyId = linkedMapOf<String, ShazamPlaylistTrack>()
    forEach { track ->
        require(track.spotifyTrackId.isNotBlank()) { "Spotify Track ID manquant." }
        require(track.playlistPosition >= 0) { "Position Spotify invalide." }
        val key = track.spotifyTrackId.trim()
        val normalized = track.copy(spotifyTrackId = key)
        val current = bySpotifyId[key]
        if (current == null || normalized.isMoreRecentThan(current)) {
            bySpotifyId[key] = normalized
        }
    }
    return bySpotifyId.values.sortedWith(shazamPlaylistRecencyComparator)
}

private fun ShazamPlaylistTrack.isMoreRecentThan(other: ShazamPlaylistTrack): Boolean =
    shazamPlaylistRecencyComparator.compare(this, other) < 0

private val shazamPlaylistRecencyComparator =
    compareByDescending<ShazamPlaylistTrack> { it.playlistAddedAtEpochMillis != null }
        .thenByDescending { it.playlistAddedAtEpochMillis ?: Long.MIN_VALUE }
        .thenBy { it.playlistPosition }
