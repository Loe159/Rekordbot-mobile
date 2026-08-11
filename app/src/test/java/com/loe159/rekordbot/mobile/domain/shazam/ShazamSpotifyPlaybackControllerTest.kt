package com.loe159.rekordbot.mobile.domain.shazam

import com.loe159.rekordbot.mobile.domain.repository.SpotifyConfigurationRepository
import com.loe159.rekordbot.mobile.domain.repository.SpotifySessionRepository
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyAuthorizationSession
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyConfiguration
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaybackAction
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaybackAuthorizationRequiredException
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaybackGateway
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyScopes
import com.loe159.rekordbot.mobile.domain.spotify.SpotifySecret
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyTokenSet
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShazamSpotifyPlaybackControllerTest {
    @Test
    fun `plays and pauses selected track with existing Spotify session`() = runBlocking {
        val gateway = RecordingPlaybackGateway()
        val controller = controller(
            tokens = tokens(scopes = SpotifyScopes.SHAZAM),
            gateway = gateway,
        )

        val playing = controller.toggle(TRACK_ID, isCurrentlyPlaying = false).getOrThrow()
        val paused = controller.toggle(TRACK_ID, isCurrentlyPlaying = true).getOrThrow()

        assertEquals(SpotifyPlaybackAction.PLAYING, playing)
        assertEquals(SpotifyPlaybackAction.PAUSED, paused)
        assertEquals(listOf("play:$TRACK_ID", "pause"), gateway.actions)
    }

    @Test
    fun `requires reconnection when stored grant lacks playback scope`() = runBlocking {
        val controller = controller(
            tokens = tokens(scopes = setOf(SpotifyScopes.PLAYLIST_READ_PRIVATE)),
            gateway = RecordingPlaybackGateway(),
        )

        val error = controller.toggle(TRACK_ID, isCurrentlyPlaying = false).exceptionOrNull()

        assertTrue(error is SpotifyPlaybackAuthorizationRequiredException)
    }

    private fun controller(
        tokens: SpotifyTokenSet,
        gateway: SpotifyPlaybackGateway,
    ): ShazamSpotifyPlaybackController {
        val sessionRepository = InMemorySpotifySessionRepository(tokens)
        return ShazamSpotifyPlaybackController(
            configurationRepository = object : SpotifyConfigurationRepository {
                override suspend fun load() = SpotifyConfiguration(clientId = "client-id")
                override suspend fun save(configuration: SpotifyConfiguration) = Unit
            },
            sessionRepository = sessionRepository,
            playbackGateway = gateway,
            tokenRefresherFactory = {
                SpotifyTokenRefresher { Result.success(tokens) }
            },
            nowEpochSeconds = { 1_000L },
        )
    }

    private fun tokens(scopes: Set<String>) = SpotifyTokenSet(
        accessToken = SpotifySecret.from("access"),
        refreshToken = SpotifySecret.from("refresh"),
        expiresAtEpochSeconds = 10_000L,
        scopes = scopes,
    )

    private class RecordingPlaybackGateway : SpotifyPlaybackGateway {
        val actions = mutableListOf<String>()

        override suspend fun playTrack(
            accessToken: SpotifySecret,
            spotifyTrackId: String,
        ): Result<Unit> = Result.success(Unit).also { actions += "play:$spotifyTrackId" }

        override suspend fun pause(accessToken: SpotifySecret): Result<Unit> =
            Result.success(Unit).also { actions += "pause" }
    }

    private class InMemorySpotifySessionRepository(
        private var tokens: SpotifyTokenSet?,
    ) : SpotifySessionRepository {
        override suspend fun loadTokens() = tokens
        override suspend fun saveTokens(tokens: SpotifyTokenSet) {
            this.tokens = tokens
        }

        override suspend fun clearTokens() {
            tokens = null
        }

        override suspend fun loadPendingAuthorization(): SpotifyAuthorizationSession? = null
        override suspend fun savePendingAuthorization(session: SpotifyAuthorizationSession) = Unit
        override suspend fun clearPendingAuthorization() = Unit
    }

    private companion object {
        const val TRACK_ID = "5lFNqg3eMNMuJsnFRKB460"
    }
}
