package com.loe159.rekordbot.mobile.data.local

import android.content.Context
import com.loe159.rekordbot.mobile.domain.repository.SpotifyConfigurationRepository
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyConfiguration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SharedPreferencesSpotifyConfigurationRepository(
    context: Context,
    private val bundledClientId: String = "",
) : SpotifyConfigurationRepository {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    override suspend fun load(): SpotifyConfiguration = withContext(Dispatchers.IO) {
        SpotifyConfiguration(
            clientId = bundledClientId.trim().ifBlank {
                preferences.getString(KEY_CLIENT_ID, "").orEmpty()
            },
            redirectUri = preferences.getString(
                KEY_REDIRECT_URI,
                SpotifyConfiguration.DEFAULT_REDIRECT_URI,
            ).orEmpty(),
            playlistName = preferences.getString(
                KEY_PLAYLIST_NAME,
                SpotifyConfiguration.DEFAULT_SHAZAM_PLAYLIST_NAME,
            ).orEmpty(),
        )
    }

    override suspend fun save(configuration: SpotifyConfiguration) = withContext(Dispatchers.IO) {
        preferences.edit().apply {
            if (bundledClientId.isBlank()) {
                putString(KEY_CLIENT_ID, configuration.clientId.trim())
            } else {
                remove(KEY_CLIENT_ID)
            }
        }
            .putString(KEY_REDIRECT_URI, configuration.redirectUri.trim())
            .putString(KEY_PLAYLIST_NAME, configuration.playlistName.trim())
            .commit()
        Unit
    }

    private companion object {
        const val PREFERENCES_NAME = "rekordbot_spotify_configuration"
        const val KEY_CLIENT_ID = "client_id"
        const val KEY_REDIRECT_URI = "redirect_uri"
        const val KEY_PLAYLIST_NAME = "playlist_name"
    }
}
