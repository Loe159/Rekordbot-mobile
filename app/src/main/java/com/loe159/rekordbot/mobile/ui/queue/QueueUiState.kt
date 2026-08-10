package com.loe159.rekordbot.mobile.ui.queue

import com.loe159.rekordbot.mobile.domain.queue.QueuedTrackOperation
import com.loe159.rekordbot.mobile.domain.queue.QueueStatus
import com.loe159.rekordbot.mobile.domain.model.TrackDraft

data class QueueUiState(
    val operations: List<QueuedTrackOperation> = emptyList(),
    val isLoading: Boolean = true,
    val editor: QueueEditorState? = null,
)

data class QueueEditorState(
    val operationId: String,
    val originalStatus: QueueStatus,
    val draft: TrackDraft,
    val error: String? = null,
) {
    val canSave: Boolean
        get() = originalStatus == QueueStatus.DRAFT || draft.isReadyForAirtable
}

internal fun QueuedTrackOperation.toEditorState(): QueueEditorState = QueueEditorState(
    operationId = operationId,
    originalStatus = status,
    draft = draft,
)
