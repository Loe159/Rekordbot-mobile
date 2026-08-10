package com.loe159.rekordbot.mobile.domain.spotify

data class SpotifyPlaylist(
    val id: String,
    val name: String,
    val spotifyUrl: String?,
)

data class SpotifyPlaylistTrack(
    val spotifyTrackId: String,
    val title: String,
    val artists: List<String>,
    val album: String?,
    val artworkUrl: String?,
    val spotifyUrl: String?,
    val isrc: String?,
    val addedAt: String?,
) {
    val artist: String
        get() = artists.joinToString(", ")
}

data class SpotifyPlaylistImport(
    val playlist: SpotifyPlaylist,
    val tracks: List<SpotifyPlaylistTrack>,
)
