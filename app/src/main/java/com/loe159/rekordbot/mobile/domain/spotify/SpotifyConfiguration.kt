package com.loe159.rekordbot.mobile.domain.spotify

import java.net.URI

data class SpotifyConfiguration(
    val clientId: String,
    val redirectUri: String = DEFAULT_REDIRECT_URI,
    val playlistName: String = DEFAULT_SHAZAM_PLAYLIST_NAME,
) {
    companion object {
        const val DEFAULT_REDIRECT_URI = "rekordbot-mobile-login://callback"
        const val DEFAULT_SHAZAM_PLAYLIST_NAME = "Mes titres Shazam"
    }
}

object SpotifyConfigurationValidator {
    fun localErrors(configuration: SpotifyConfiguration): List<String> = buildList {
        if (configuration.clientId.isBlank()) {
            add("Le Client ID Spotify est obligatoire.")
        }
        if (!configuration.redirectUri.isUsableRedirectUri()) {
            add("L’URI de redirection Spotify est invalide.")
        }
        if (configuration.playlistName.isBlank()) {
            add("Le nom de la playlist Shazam est obligatoire.")
        }
    }

    private fun String.isUsableRedirectUri(): Boolean = runCatching {
        val uri = URI(trim())
        uri.isAbsolute && !uri.scheme.isNullOrBlank() && uri.fragment == null
    }.getOrDefault(false)
}
