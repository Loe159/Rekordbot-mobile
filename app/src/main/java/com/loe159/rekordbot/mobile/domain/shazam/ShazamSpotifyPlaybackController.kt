package com.loe159.rekordbot.mobile.domain.shazam

import com.loe159.rekordbot.mobile.domain.repository.SpotifyConfigurationRepository
import com.loe159.rekordbot.mobile.domain.repository.SpotifySessionRepository
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyConfiguration
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaybackAction
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaybackAuthorizationRequiredException
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaybackGateway
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyScopes
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyTokenSet

class ShazamSpotifyPlaybackController(
    private val configurationRepository: SpotifyConfigurationRepository,
    private val sessionRepository: SpotifySessionRepository,
    private val playbackGateway: SpotifyPlaybackGateway,
    private val tokenRefresherFactory: (SpotifyConfiguration) -> SpotifyTokenRefresher,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1_000 },
) {
    suspend fun toggle(
        spotifyTrackId: String,
        isCurrentlyPlaying: Boolean,
    ): Result<SpotifyPlaybackAction> = runCatching {
        val configuration = configurationRepository.load()
        val storedTokens = sessionRepository.loadTokens()
            ?: throw SpotifyPlaybackAuthorizationRequiredException("Connexion Spotify requise.")
        if (SpotifyScopes.PLAYBACK_CONTROL !in storedTokens.scopes) {
            throw SpotifyPlaybackAuthorizationRequiredException(
                "Active la pré-écoute en reconnectant Spotify.",
            )
        }
        val tokens = freshTokens(configuration, storedTokens)
        if (isCurrentlyPlaying) {
            playbackGateway.pause(tokens.accessToken).getOrThrow()
            SpotifyPlaybackAction.PAUSED
        } else {
            playbackGateway.playTrack(tokens.accessToken, spotifyTrackId).getOrThrow()
            SpotifyPlaybackAction.PLAYING
        }
    }

    private suspend fun freshTokens(
        configuration: SpotifyConfiguration,
        storedTokens: SpotifyTokenSet,
    ): SpotifyTokenSet {
        if (!storedTokens.needsRefresh(nowEpochSeconds())) return storedTokens
        if (storedTokens.refreshToken == null) {
            throw SpotifyPlaybackAuthorizationRequiredException(
                "Reconnecte Spotify pour utiliser la pré-écoute.",
            )
        }
        return tokenRefresherFactory(configuration)
            .refreshTokens(storedTokens)
            .getOrThrow()
            .also { sessionRepository.saveTokens(it) }
    }
}
