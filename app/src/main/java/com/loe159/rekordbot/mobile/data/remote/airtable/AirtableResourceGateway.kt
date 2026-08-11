package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.airtable.AirtableBaseSummary
import com.loe159.rekordbot.mobile.domain.airtable.AirtableTableSummary
import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration

interface AirtableResourceGateway {
    suspend fun listBases(configuration: AirtableConfiguration): Result<List<AirtableBaseSummary>>

    suspend fun listTables(
        configuration: AirtableConfiguration,
        baseId: String,
    ): Result<List<AirtableTableSummary>>
}
