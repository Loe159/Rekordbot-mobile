package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.AirtableTableSchema
import com.loe159.rekordbot.mobile.domain.model.TrackDraft

interface AirtableGateway {
    suspend fun fetchTableSchema(configuration: AirtableConfiguration): Result<AirtableTableSchema>

    suspend fun createDemoRecord(configuration: AirtableConfiguration): Result<String>

    suspend fun findRecordBySpotifyTrackId(
        configuration: AirtableConfiguration,
        spotifyTrackId: String,
    ): Result<String?>

    suspend fun createTrackRecord(
        configuration: AirtableConfiguration,
        track: TrackDraft,
    ): Result<String>
}
