package com.loe159.rekordbot.mobile.domain.queue

import com.loe159.rekordbot.mobile.domain.model.TrackDraft

enum class QueueStatus {
    DRAFT,
    PENDING,
    SENDING,
    SENT,
    FAILED,
}

data class QueuedTrackOperation(
    val operationId: String,
    val draft: TrackDraft,
    val status: QueueStatus,
    val attemptCount: Int,
    val lastError: String?,
    val willRetryAutomatically: Boolean,
    val createdAt: Long,
    val updatedAt: Long,
    val lastAttemptAt: Long?,
    val sentAt: Long?,
    val airtableRecordId: String?,
) {
    val canRetry: Boolean
        get() = status == QueueStatus.FAILED

    val canDelete: Boolean
        get() = status != QueueStatus.SENDING

    val canEdit: Boolean
        get() = status == QueueStatus.DRAFT ||
            status == QueueStatus.PENDING || status == QueueStatus.FAILED

    val canSendDraft: Boolean
        get() = status == QueueStatus.DRAFT && draft.isReadyForAirtable
}

object QueueStateMachine {
    fun canClaim(status: QueueStatus): Boolean =
        status == QueueStatus.PENDING || status == QueueStatus.FAILED

    fun canRetry(status: QueueStatus): Boolean = status == QueueStatus.FAILED

    fun canDelete(status: QueueStatus): Boolean = status != QueueStatus.SENDING
}
