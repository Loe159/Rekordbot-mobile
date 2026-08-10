package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.AirtableTableSchema

interface AirtableGateway {
    suspend fun fetchTableSchema(configuration: AirtableConfiguration): Result<AirtableTableSchema>

    suspend fun createDemoRecord(configuration: AirtableConfiguration): Result<String>
}
