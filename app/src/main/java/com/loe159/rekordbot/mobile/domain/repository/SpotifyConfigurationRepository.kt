package com.loe159.rekordbot.mobile.domain.repository

import com.loe159.rekordbot.mobile.domain.spotify.SpotifyConfiguration

interface SpotifyConfigurationRepository {
    suspend fun load(): SpotifyConfiguration
    suspend fun save(configuration: SpotifyConfiguration)
}
