package com.loe159.rekordbot.mobile.ui.share

import com.loe159.rekordbot.mobile.domain.airtable.AirtableTrackSubmissionResult
import com.loe159.rekordbot.mobile.domain.model.TrackDraft

data class SharePreviewUiState(
    val draft: TrackDraft,
    val isConfigurationLoading: Boolean = true,
    val isAirtableConfigured: Boolean = false,
    val isSubmitting: Boolean = false,
    val isSavingDraft: Boolean = false,
    val savedDraftOperationId: String? = null,
    val draftSaveError: String? = null,
    val submissionResult: AirtableTrackSubmissionResult? = null,
)
