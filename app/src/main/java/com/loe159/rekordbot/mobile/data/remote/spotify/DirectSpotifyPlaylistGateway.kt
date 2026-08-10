package com.loe159.rekordbot.mobile.data.remote.spotify

import com.loe159.rekordbot.mobile.domain.spotify.SpotifyConfiguration
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaylist
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaylistGateway
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaylistImport
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaylistTrack
import com.loe159.rekordbot.mobile.domain.spotify.SpotifySecret
import java.net.URI

class DirectSpotifyPlaylistGateway(
    private val transport: SpotifyHttpTransport = HttpUrlConnectionSpotifyTransport(),
    private val apiBaseUrl: String = DEFAULT_API_BASE_URL,
) : SpotifyPlaylistGateway {
    override suspend fun listPlaylists(
        accessToken: SpotifySecret,
    ): Result<List<SpotifyPlaylist>> = runCatching {
        collectPages("${apiBaseUrl.trimEnd('/')}/me/playlists?limit=$PAGE_LIMIT", accessToken) { body ->
            SpotifyPlaylistJsonParser.parsePage(body)
        }
    }

    override suspend fun findPlaylistByName(
        accessToken: SpotifySecret,
        playlistName: String,
    ): Result<SpotifyPlaylist?> = runCatching {
        require(playlistName.isNotBlank()) { "Le nom de la playlist Shazam est obligatoire." }
        val matching = listPlaylists(accessToken).getOrThrow().filter {
            it.name.trim().equals(playlistName.trim(), ignoreCase = true)
        }
        if (matching.size > 1) {
            throw SpotifyApiException(
                "Plusieurs playlists Spotify portent le nom « ${playlistName.trim()} ».",
            )
        }
        matching.singleOrNull()
    }

    override suspend fun listPlaylistTracks(
        accessToken: SpotifySecret,
        playlistId: String,
    ): Result<List<SpotifyPlaylistTrack>> = runCatching {
        require(playlistId.matches(SPOTIFY_ID_REGEX)) { "Identifiant de playlist Spotify invalide." }
        collectPages(
            initialUrl = "${apiBaseUrl.trimEnd('/')}/playlists/$playlistId/items?limit=$PAGE_LIMIT",
            accessToken = accessToken,
        ) { body -> SpotifyPlaylistItemsJsonParser.parsePage(body) }
    }

    override suspend fun fetchConfiguredPlaylist(
        accessToken: SpotifySecret,
        configuration: SpotifyConfiguration,
    ): Result<SpotifyPlaylistImport> = runCatching {
        val playlist = findPlaylistByName(accessToken, configuration.playlistName).getOrThrow()
            ?: throw SpotifyApiException(
                "Playlist Spotify « ${configuration.playlistName.trim()} » introuvable.",
                statusCode = 404,
            )
        SpotifyPlaylistImport(
            playlist = playlist,
            tracks = listPlaylistTracks(accessToken, playlist.id).getOrThrow(),
        )
    }

    private suspend fun <T> collectPages(
        initialUrl: String,
        accessToken: SpotifySecret,
        parse: (String) -> SpotifyParsedPage<T>,
    ): List<T> {
        val items = mutableListOf<T>()
        val visitedUrls = mutableSetOf<String>()
        var nextUrl: String? = initialUrl
        while (nextUrl != null) {
            val url = nextUrl
            if (!visitedUrls.add(url) || visitedUrls.size > MAX_PAGE_COUNT) {
                throw SpotifyApiException("Pagination Spotify invalide.")
            }
            requireTrustedApiUrl(url)
            val response = transport.execute(
                SpotifyHttpRequest(
                    method = "GET",
                    url = url,
                    headers = mapOf(
                        "Accept" to "application/json",
                        "Authorization" to "Bearer ${accessToken.reveal()}",
                    ),
                ),
            )
            if (response.statusCode !in 200..299) {
                throw SpotifyApiException(apiError(response.statusCode), response.statusCode)
            }
            val page = parse(response.body)
            items += page.items
            nextUrl = page.nextUrl
        }
        return items
    }

    private fun requireTrustedApiUrl(url: String) {
        val expected = URI(apiBaseUrl)
        val actual = runCatching { URI(url) }.getOrNull()
        require(
            actual != null &&
                actual.scheme.equals(expected.scheme, ignoreCase = true) &&
                actual.authority.equals(expected.authority, ignoreCase = true) &&
                actual.path.startsWith(expected.path.trimEnd('/') + "/"),
        ) { "URL de pagination Spotify refusée." }
    }

    private fun apiError(statusCode: Int): String = when (statusCode) {
        401 -> "La connexion Spotify a expiré."
        403 -> "Spotify refuse l’accès à cette playlist."
        404 -> "Playlist Spotify introuvable."
        429 -> "Limite Spotify atteinte. Réessaie plus tard."
        in 500..599 -> "Spotify est temporairement indisponible."
        else -> "Erreur Spotify HTTP $statusCode."
    }

    private companion object {
        const val DEFAULT_API_BASE_URL = "https://api.spotify.com/v1"
        const val PAGE_LIMIT = 50
        const val MAX_PAGE_COUNT = 200
        val SPOTIFY_ID_REGEX = Regex("[A-Za-z0-9]{22}")
    }
}
