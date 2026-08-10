package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.model.TrackDraft

/** Remote boundary. The direct Airtable client will implement it during P1/P3. */
interface AirtableGateway {
    suspend fun createTrack(track: TrackDraft): Result<String>
}

