package com.loe159.rekordbot.mobile.domain.repository

import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration

interface AirtableConfigurationRepository {
    suspend fun load(): AirtableConfiguration

    suspend fun save(configuration: AirtableConfiguration)

    suspend fun isConnectionValidated(): Boolean

    suspend fun markConnectionValidated()
}
