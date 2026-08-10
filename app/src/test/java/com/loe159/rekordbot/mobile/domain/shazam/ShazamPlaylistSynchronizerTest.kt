package com.loe159.rekordbot.mobile.domain.shazam

import com.loe159.rekordbot.mobile.domain.repository.ShazamInboxRepository
import com.loe159.rekordbot.mobile.domain.repository.SpotifyConfigurationRepository
import com.loe159.rekordbot.mobile.domain.repository.SpotifySessionRepository
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyAuthorizationSession
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyConfiguration
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaylist
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaylistGateway
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaylistImport
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaylistTrack
import com.loe159.rekordbot.mobile.domain.spotify.SpotifySecret
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyTokenSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShazamPlaylistSynchronizerTest {
    @Test
    fun `expired tokens are refreshed saved and English playlist is used as fallback`() =
        runBlocking {
            val oldTokens = tokens(access = "old", expiresAt = 10, refresh = "refresh")
            val newTokens = tokens(access = "new", expiresAt = 10_000, refresh = "refresh")
            val session = FakeSessionRepository(oldTokens)
            val gateway = FakePlaylistGateway(
                playlistsByName = mapOf(
                    "My Shazam Tracks" to SpotifyPlaylist(
                        id = "1234567890123456789012",
                        name = "My Shazam Tracks",
                        spotifyUrl = null,
                    ),
                ),
                tracks = listOf(
                    spotifyTrack(addedAt = "2026-08-10T18:30:00Z"),
                    spotifyTrack(addedAt = "not-an-instant"),
                ),
            )
            val inbox = FakeInboxRepository(addedCount = 1)
            var refreshCalls = 0
            val synchronizer = synchronizer(
                session = session,
                gateway = gateway,
                inbox = inbox,
                nowEpochSeconds = 100,
                tokenRefresher = SpotifyTokenRefresher {
                    refreshCalls += 1
                    Result.success(newTokens)
                },
            )

            val summary = synchronizer.synchronize().getOrThrow()

            assertEquals(1, refreshCalls)
            assertEquals(newTokens, session.savedTokens)
            assertEquals(listOf("Mes titres Shazam", "My Shazam Tracks"), gateway.searchedNames)
            assertEquals("My Shazam Tracks", summary.playlistName)
            assertEquals(2, summary.receivedTrackCount)
            assertEquals(1, summary.distinctTrackCount)
            assertEquals(1, summary.addedCount)
            assertEquals(0, summary.refreshedCount)
            assertTrue(summary.tokensRefreshed)
            assertEquals(1_786_386_600_000L, inbox.synchronizedTracks[0].playlistAddedAtEpochMillis)
            assertEquals(null, inbox.synchronizedTracks[1].playlistAddedAtEpochMillis)
            assertEquals(0, inbox.synchronizedTracks[0].playlistPosition)
            assertEquals(1, inbox.synchronizedTracks[1].playlistPosition)
            assertEquals(
                "https://open.spotify.com/track/1234567890123456789012",
                inbox.synchronizedTracks[0].spotifyUrl,
            )
        }

    @Test
    fun `valid tokens and custom playlist do not refresh or try Shazam fallback names`() =
        runBlocking {
            val currentTokens = tokens(access = "current", expiresAt = 10_000, refresh = null)
            val session = FakeSessionRepository(currentTokens)
            val gateway = FakePlaylistGateway(
                playlistsByName = mapOf(
                    "My discoveries" to SpotifyPlaylist(
                        id = "1234567890123456789012",
                        name = "My discoveries",
                        spotifyUrl = null,
                    ),
                ),
                tracks = emptyList(),
            )
            var refreshed = false
            val synchronizer = synchronizer(
                session = session,
                gateway = gateway,
                inbox = FakeInboxRepository(addedCount = 0),
                configuration = SpotifyConfiguration(
                    clientId = "client-id",
                    playlistName = "My discoveries",
                ),
                nowEpochSeconds = 100,
                tokenRefresher = SpotifyTokenRefresher {
                    refreshed = true
                    Result.failure(AssertionError("refresh must not be called"))
                },
            )

            val summary = synchronizer.synchronize().getOrThrow()

            assertFalse(refreshed)
            assertEquals(null, session.savedTokens)
            assertEquals(listOf("My discoveries"), gateway.searchedNames)
            assertFalse(summary.tokensRefreshed)
        }

    @Test
    fun `missing session fails before Spotify API is called`() = runBlocking {
        val gateway = FakePlaylistGateway(emptyMap(), emptyList())
        val synchronizer = synchronizer(
            session = FakeSessionRepository(null),
            gateway = gateway,
            inbox = FakeInboxRepository(addedCount = 0),
            nowEpochSeconds = 100,
            tokenRefresher = SpotifyTokenRefresher { Result.success(it) },
        )

        val failure = synchronizer.synchronize().exceptionOrNull()

        assertTrue(failure is ShazamSyncPrerequisiteException)
        assertTrue(gateway.searchedNames.isEmpty())
    }

    private fun synchronizer(
        session: FakeSessionRepository,
        gateway: FakePlaylistGateway,
        inbox: FakeInboxRepository,
        configuration: SpotifyConfiguration = SpotifyConfiguration(clientId = "client-id"),
        nowEpochSeconds: Long,
        tokenRefresher: SpotifyTokenRefresher,
    ) = ShazamPlaylistSynchronizer(
        configurationRepository = FakeConfigurationRepository(configuration),
        sessionRepository = session,
        playlistGateway = gateway,
        inboxRepository = inbox,
        tokenRefresherFactory = { tokenRefresher },
        nowEpochSeconds = { nowEpochSeconds },
    )

    private fun tokens(access: String, expiresAt: Long, refresh: String?) = SpotifyTokenSet(
        accessToken = SpotifySecret.from(access),
        refreshToken = refresh?.let(SpotifySecret::from),
        expiresAtEpochSeconds = expiresAt,
        scopes = setOf("playlist-read-private"),
    )

    private fun spotifyTrack(addedAt: String?) = SpotifyPlaylistTrack(
        spotifyTrackId = "1234567890123456789012",
        title = "Open Eye Signal",
        artists = listOf("Jon Hopkins"),
        album = "Immunity",
        artworkUrl = null,
        spotifyUrl = null,
        isrc = "GB-CEL-13-00001",
        addedAt = addedAt,
    )
}

private class FakeConfigurationRepository(
    private val configuration: SpotifyConfiguration,
) : SpotifyConfigurationRepository {
    override suspend fun load(): SpotifyConfiguration = configuration
    override suspend fun save(configuration: SpotifyConfiguration) = Unit
}

private class FakeSessionRepository(
    private val loadedTokens: SpotifyTokenSet?,
) : SpotifySessionRepository {
    var savedTokens: SpotifyTokenSet? = null

    override suspend fun loadTokens(): SpotifyTokenSet? = loadedTokens
    override suspend fun saveTokens(tokens: SpotifyTokenSet) {
        savedTokens = tokens
    }

    override suspend fun clearTokens() = Unit
    override suspend fun loadPendingAuthorization(): SpotifyAuthorizationSession? = null
    override suspend fun savePendingAuthorization(session: SpotifyAuthorizationSession) = Unit
    override suspend fun clearPendingAuthorization() = Unit
}

private class FakePlaylistGateway(
    private val playlistsByName: Map<String, SpotifyPlaylist>,
    private val tracks: List<SpotifyPlaylistTrack>,
) : SpotifyPlaylistGateway {
    val searchedNames = mutableListOf<String>()

    override suspend fun listPlaylists(accessToken: SpotifySecret) =
        Result.success(playlistsByName.values.toList())

    override suspend fun findPlaylistByName(
        accessToken: SpotifySecret,
        playlistName: String,
    ): Result<SpotifyPlaylist?> {
        searchedNames += playlistName
        return Result.success(playlistsByName[playlistName])
    }

    override suspend fun listPlaylistTracks(
        accessToken: SpotifySecret,
        playlistId: String,
    ) = Result.success(tracks)

    override suspend fun fetchConfiguredPlaylist(
        accessToken: SpotifySecret,
        configuration: SpotifyConfiguration,
    ): Result<SpotifyPlaylistImport> = error("Not used by synchronizer")
}

private class FakeInboxRepository(
    private val addedCount: Int,
) : ShazamInboxRepository {
    var synchronizedTracks: List<ShazamPlaylistTrack> = emptyList()

    override fun observeAll(): Flow<List<ShazamInboxTrack>> = flowOf(emptyList())
    override fun observePending(): Flow<List<ShazamInboxTrack>> = flowOf(emptyList())
    override fun observeIgnored(): Flow<List<ShazamInboxTrack>> = flowOf(emptyList())
    override fun observeCounts(): Flow<ShazamInboxCounts> =
        flowOf(ShazamInboxCounts(0, 0, 0, 0))

    override suspend fun synchronize(tracks: List<ShazamPlaylistTrack>): ShazamSyncResult {
        synchronizedTracks = tracks
        return ShazamSyncResult(
            receivedCount = tracks.size,
            distinctCount = tracks.distinctBy(ShazamPlaylistTrack::spotifyTrackId).size,
            addedCount = addedCount,
        )
    }

    override suspend fun markSaved(spotifyTrackId: String) = false
    override suspend fun markIgnored(spotifyTrackId: String) = false
    override suspend fun markAlreadyPresent(spotifyTrackId: String) = false
    override suspend fun reopen(spotifyTrackId: String) = false
}
