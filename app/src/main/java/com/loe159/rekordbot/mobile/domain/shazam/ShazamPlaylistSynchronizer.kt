package com.loe159.rekordbot.mobile.domain.shazam

import com.loe159.rekordbot.mobile.domain.repository.ShazamInboxRepository
import com.loe159.rekordbot.mobile.domain.repository.SpotifyConfigurationRepository
import com.loe159.rekordbot.mobile.domain.repository.SpotifySessionRepository
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyConfiguration
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyConfigurationValidator
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaylist
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaylistGateway
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaylistTrack
import com.loe159.rekordbot.mobile.domain.spotify.SpotifySecret
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyTokenSet
import java.time.Instant

fun interface SpotifyTokenRefresher {
    suspend fun refreshTokens(currentTokens: SpotifyTokenSet): Result<SpotifyTokenSet>
}

data class ShazamPlaylistSyncSummary(
    val playlistId: String,
    val playlistName: String,
    val receivedTrackCount: Int,
    val distinctTrackCount: Int,
    val addedCount: Int,
    val refreshedCount: Int,
    val tokensRefreshed: Boolean,
)

class ShazamSyncPrerequisiteException(message: String) : IllegalStateException(message)

/** Coordinates one complete Spotify-to-Room synchronization without Android dependencies. */
class ShazamPlaylistSynchronizer(
    private val configurationRepository: SpotifyConfigurationRepository,
    private val sessionRepository: SpotifySessionRepository,
    private val playlistGateway: SpotifyPlaylistGateway,
    private val inboxRepository: ShazamInboxRepository,
    private val tokenRefresherFactory: (SpotifyConfiguration) -> SpotifyTokenRefresher,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1_000 },
) {
    suspend fun synchronize(): Result<ShazamPlaylistSyncSummary> = runCatching {
        val configuration = configurationRepository.load()
        val configurationErrors = SpotifyConfigurationValidator.localErrors(configuration)
        if (configurationErrors.isNotEmpty()) {
            throw ShazamSyncPrerequisiteException(configurationErrors.joinToString(" "))
        }

        val storedTokens = sessionRepository.loadTokens()
            ?: throw ShazamSyncPrerequisiteException("Connexion Spotify requise.")
        val (tokens, tokensRefreshed) = freshTokens(configuration, storedTokens)
        val playlist = findShazamPlaylist(tokens.accessToken, configuration.playlistName)
        val spotifyTracks = playlistGateway.listPlaylistTracks(tokens.accessToken, playlist.id)
            .getOrThrow()
        val inboxTracks = spotifyTracks.mapIndexed { position, track ->
            track.toShazamPlaylistTrack(position)
        }
        val result = inboxRepository.synchronize(inboxTracks)

        ShazamPlaylistSyncSummary(
            playlistId = playlist.id,
            playlistName = playlist.name,
            receivedTrackCount = result.receivedCount,
            distinctTrackCount = result.distinctCount,
            addedCount = result.addedCount,
            refreshedCount = result.refreshedCount,
            tokensRefreshed = tokensRefreshed,
        )
    }

    private suspend fun freshTokens(
        configuration: SpotifyConfiguration,
        storedTokens: SpotifyTokenSet,
    ): Pair<SpotifyTokenSet, Boolean> {
        if (!storedTokens.needsRefresh(nowEpochSeconds())) return storedTokens to false
        if (storedTokens.refreshToken == null) {
            throw ShazamSyncPrerequisiteException("Reconnecte Spotify pour synchroniser Shazam.")
        }
        val refreshed = tokenRefresherFactory(configuration)
            .refreshTokens(storedTokens)
            .getOrThrow()
        sessionRepository.saveTokens(refreshed)
        return refreshed to true
    }

    private suspend fun findShazamPlaylist(
        accessToken: SpotifySecret,
        configuredName: String,
    ): SpotifyPlaylist {
        val candidateNames = playlistCandidateNames(configuredName)
        candidateNames.forEach { name ->
            playlistGateway.findPlaylistByName(accessToken, name)
                .getOrThrow()
                ?.let { return it }
        }
        throw ShazamSyncPrerequisiteException(
            "Playlist Spotify ${candidateNames.joinToString(" ou ") { "« $it »" }} introuvable.",
        )
    }

    private fun playlistCandidateNames(configuredName: String): List<String> {
        val configured = configuredName.trim()
        val usesKnownDefault = KNOWN_SHAZAM_PLAYLIST_NAMES.any {
            it.equals(configured, ignoreCase = true)
        }
        return if (usesKnownDefault) {
            listOf(configured).plus(KNOWN_SHAZAM_PLAYLIST_NAMES)
                .distinctBy { it.lowercase() }
        } else {
            listOf(configured)
        }
    }

    private fun SpotifyPlaylistTrack.toShazamPlaylistTrack(position: Int): ShazamPlaylistTrack =
        ShazamPlaylistTrack(
            spotifyTrackId = spotifyTrackId,
            title = title,
            artist = artist,
            spotifyUrl = spotifyUrl ?: "https://open.spotify.com/track/$spotifyTrackId",
            albumName = album,
            artworkUrl = artworkUrl,
            isrc = isrc,
            playlistAddedAtEpochMillis = addedAt.toEpochMillisOrNull(),
            playlistPosition = position,
        )

    private fun String?.toEpochMillisOrNull(): Long? = this
        ?.takeIf(String::isNotBlank)
        ?.let { value -> runCatching { Instant.parse(value).toEpochMilli() }.getOrNull() }

    private companion object {
        val KNOWN_SHAZAM_PLAYLIST_NAMES = listOf(
            SpotifyConfiguration.DEFAULT_SHAZAM_PLAYLIST_NAME,
            "My Shazam Tracks",
        )
    }
}
