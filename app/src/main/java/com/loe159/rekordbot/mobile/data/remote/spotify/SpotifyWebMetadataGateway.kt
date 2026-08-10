package com.loe159.rekordbot.mobile.data.remote.spotify

import com.loe159.rekordbot.mobile.domain.spotify.SpotifyMetadataGateway
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyTrackMetadata
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpotifyWebMetadataGateway(
    private val webBaseUrl: String = "https://open.spotify.com",
) : SpotifyMetadataGateway {
    override suspend fun fetchTrackMetadata(trackId: String): Result<SpotifyTrackMetadata> = runCatching {
        require(trackId.matches(TRACK_ID_REGEX)) { "Identifiant Spotify invalide." }
        val html = request("$webBaseUrl/track/$trackId")
        SpotifyPageMetadataParser.parse(html)
            ?: error("Spotify n’a pas fourni le titre et l’artiste.")
    }

    private suspend fun request(url: String): String = withContext(Dispatchers.IO) {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("Accept", "text/html")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            connection.setRequestProperty("User-Agent", "Rekordbot-Mobile/0.1 Android")

            val statusCode = connection.responseCode
            if (statusCode !in 200..299) {
                error("Spotify a répondu avec l’erreur HTTP $statusCode.")
            }
            connection.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        } catch (exception: IOException) {
            throw IOException("Connexion à Spotify impossible.", exception)
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        val TRACK_ID_REGEX = Regex("[A-Za-z0-9]{22}")
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 10_000
    }
}
