package com.loe159.rekordbot.mobile.domain.repository

import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsConfiguration

interface SoundchartsConfigurationRepository {
    suspend fun load(): SoundchartsConfiguration
    suspend fun save(configuration: SoundchartsConfiguration)
}
