package com.loe159.rekordbot.mobile.data.remote.soundcharts

import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsConfiguration
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsGateway
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsMetadata

/** Calls the Rekordbot service; Soundcharts credentials never leave the server. */
class BackendSoundchartsGateway(
    private val publicApiBaseUrl: String,
    private val transport: SoundchartsTransport = HttpUrlConnectionSoundchartsTransport(),
) : SoundchartsGateway {
    override suspend fun fetchTrackMetadata(
        spotifyTrackId: String,
        configuration: SoundchartsConfiguration,
    ): Result<SoundchartsMetadata> = runCatching {
        require(spotifyTrackId.matches(SPOTIFY_TRACK_ID_REGEX)) {
            "Identifiant Spotify invalide."
        }
        require(publicApiBaseUrl.isNotBlank()) { "Service Rekordbot non configuré." }
        val response = transport.execute(
            SoundchartsRequest(
                url = "${publicApiBaseUrl.trimEnd('/')}/v1/soundcharts/tracks/spotify/" +
                    spotifyTrackId,
                headers = mapOf("Accept" to "application/json"),
            ),
        )
        if (response.statusCode !in 200..299) {
            throw SoundchartsApiException(
                message = readableError(response.statusCode),
                statusCode = response.statusCode,
            )
        }
        SoundchartsMetadataParser.parse(response.body)
    }

    private fun readableError(statusCode: Int): String = when (statusCode) {
        401 -> "Application Rekordbot non autorisée."
        403 -> "Accès Soundcharts indisponible pour cette application."
        404 -> "Morceau introuvable dans Soundcharts."
        429 -> "Limite Soundcharts atteinte. Réessaie plus tard."
        in 500..599 -> "Service d’enrichissement temporairement indisponible."
        else -> "Enrichissement Soundcharts impossible (HTTP $statusCode)."
    }

    private companion object {
        val SPOTIFY_TRACK_ID_REGEX = Regex("[A-Za-z0-9]{22}")
    }
}
