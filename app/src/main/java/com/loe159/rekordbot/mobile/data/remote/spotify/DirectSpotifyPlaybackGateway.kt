package com.loe159.rekordbot.mobile.data.remote.spotify

import com.loe159.rekordbot.mobile.domain.spotify.SpotifyPlaybackGateway
import com.loe159.rekordbot.mobile.domain.spotify.SpotifySecret
import org.json.JSONArray
import org.json.JSONObject

class DirectSpotifyPlaybackGateway(
    private val transport: SpotifyHttpTransport = HttpUrlConnectionSpotifyTransport(),
    private val apiBaseUrl: String = DEFAULT_API_BASE_URL,
) : SpotifyPlaybackGateway {
    override suspend fun playTrack(
        accessToken: SpotifySecret,
        spotifyTrackId: String,
    ): Result<Unit> = runCatching {
        require(spotifyTrackId.matches(SPOTIFY_ID_REGEX)) { "Identifiant Spotify invalide." }
        execute(
            accessToken = accessToken,
            path = "/me/player/play",
            body = JSONObject()
                .put("uris", JSONArray().put("spotify:track:$spotifyTrackId"))
                .put("position_ms", 0)
                .toString(),
        )
    }

    override suspend fun pause(accessToken: SpotifySecret): Result<Unit> = runCatching {
        execute(accessToken = accessToken, path = "/me/player/pause", body = null)
    }

    private suspend fun execute(
        accessToken: SpotifySecret,
        path: String,
        body: String?,
    ) {
        val response = transport.execute(
            SpotifyHttpRequest(
                method = "PUT",
                url = "${apiBaseUrl.trimEnd('/')}$path",
                headers = buildMap {
                    put("Accept", "application/json")
                    put("Authorization", "Bearer ${accessToken.reveal()}")
                    if (body != null) put("Content-Type", "application/json")
                },
                body = body,
            ),
        )
        if (response.statusCode !in 200..299) {
            throw SpotifyApiException(playbackError(response.statusCode), response.statusCode)
        }
    }

    private fun playbackError(statusCode: Int): String = when (statusCode) {
        401 -> "La connexion Spotify a expiré."
        403 -> "La pré-écoute nécessite Spotify Premium et l’autorisation de lecture."
        404 -> "Aucun appareil Spotify actif. Ouvre Spotify puis réessaie."
        429 -> "Limite Spotify atteinte. Réessaie plus tard."
        in 500..599 -> "Spotify est temporairement indisponible."
        else -> "Pré-écoute Spotify impossible (HTTP $statusCode)."
    }

    private companion object {
        const val DEFAULT_API_BASE_URL = "https://api.spotify.com/v1"
        val SPOTIFY_ID_REGEX = Regex("[A-Za-z0-9]{22}")
    }
}
