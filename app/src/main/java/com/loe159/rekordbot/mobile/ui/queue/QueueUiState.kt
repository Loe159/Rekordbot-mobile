package com.loe159.rekordbot.mobile.ui.queue

import com.loe159.rekordbot.mobile.domain.queue.QueuedTrackOperation

data class QueueUiState(
    val operations: List<QueuedTrackOperation> = emptyList(),
    val isLoading: Boolean = true,
)
