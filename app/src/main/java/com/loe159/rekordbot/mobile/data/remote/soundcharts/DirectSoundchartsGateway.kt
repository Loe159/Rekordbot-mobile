package com.loe159.rekordbot.mobile.data.remote.soundcharts

import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsConfiguration
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsGateway
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsMetadata

class DirectSoundchartsGateway(
    private val transport: SoundchartsTransport = HttpUrlConnectionSoundchartsTransport(),
    private val apiBaseUrl: String = "https://customer.api.soundcharts.com/api/v2.25",
) : SoundchartsGateway {
    override suspend fun fetchTrackMetadata(
        spotifyTrackId: String,
        configuration: SoundchartsConfiguration,
    ): Result<SoundchartsMetadata> = runCatching {
        require(spotifyTrackId.matches(SPOTIFY_TRACK_ID_REGEX)) {
            "Identifiant Spotify invalide."
        }
        require(configuration.isComplete) { "Configuration Soundcharts incomplète." }
        val response = transport.execute(
            SoundchartsRequest(
                url = "${apiBaseUrl.trimEnd('/')}/song/by-platform/spotify/$spotifyTrackId",
                headers = mapOf(
                    "Accept" to "application/json",
                    "x-app-id" to configuration.appId.trim(),
                    "x-api-key" to configuration.apiKey.trim(),
                ),
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
        401 -> "Identifiants Soundcharts invalides."
        403 -> "Accès Soundcharts refusé pour ces identifiants."
        404 -> "Morceau introuvable dans Soundcharts."
        429 -> "Limite Soundcharts atteinte. Réessaie plus tard."
        else -> "Erreur Soundcharts HTTP $statusCode."
    }

    private companion object {
        val SPOTIFY_TRACK_ID_REGEX = Regex("[A-Za-z0-9]{22}")
    }
}
