package com.loe159.rekordbot.mobile.data.local

import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import kotlinx.coroutines.flow.Flow

/** Local persistence boundary. Room will implement it during P4. */
interface PendingTrackStore {
    fun observeAll(): Flow<List<TrackDraft>>

    suspend fun save(track: TrackDraft)
}

