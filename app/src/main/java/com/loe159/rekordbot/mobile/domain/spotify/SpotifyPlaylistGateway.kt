package com.loe159.rekordbot.mobile.domain.spotify

interface SpotifyPlaylistGateway {
    suspend fun listPlaylists(accessToken: SpotifySecret): Result<List<SpotifyPlaylist>>

    suspend fun findPlaylistByName(
        accessToken: SpotifySecret,
        playlistName: String,
    ): Result<SpotifyPlaylist?>

    suspend fun listPlaylistTracks(
        accessToken: SpotifySecret,
        playlistId: String,
    ): Result<List<SpotifyPlaylistTrack>>

    suspend fun fetchConfiguredPlaylist(
        accessToken: SpotifySecret,
        configuration: SpotifyConfiguration,
    ): Result<SpotifyPlaylistImport>
}
