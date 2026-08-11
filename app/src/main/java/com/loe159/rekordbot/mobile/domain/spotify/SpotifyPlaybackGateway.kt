package com.loe159.rekordbot.mobile.domain.spotify

interface SpotifyPlaybackGateway {
    suspend fun playTrack(accessToken: SpotifySecret, spotifyTrackId: String): Result<Unit>
    suspend fun pause(accessToken: SpotifySecret): Result<Unit>
}

enum class SpotifyPlaybackAction {
    PLAYING,
    PAUSED,
}

class SpotifyPlaybackAuthorizationRequiredException(message: String) :
    IllegalStateException(message)
