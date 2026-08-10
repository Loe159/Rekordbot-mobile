package com.loe159.rekordbot.mobile.data.local

import com.loe159.rekordbot.mobile.domain.queue.QueuedTrackOperation
import kotlinx.coroutines.flow.Flow

/** Persistence boundary implemented by Room. */
interface PendingTrackStore {
    fun observeAll(): Flow<List<QueuedTrackOperation>>

    fun observeOpenCount(): Flow<Int>
}
