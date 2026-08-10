package com.loe159.rekordbot.mobile.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableGateway
import com.loe159.rekordbot.mobile.domain.airtable.AirtableTrackSubmissionResult
import com.loe159.rekordbot.mobile.domain.airtable.SubmitTrackToAirtable
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.repository.AirtableConfigurationRepository
import com.loe159.rekordbot.mobile.domain.repository.PendingTrackRepository
import com.loe159.rekordbot.mobile.domain.queue.QueueWorkScheduler
import com.loe159.rekordbot.mobile.domain.queue.QueueOperationIdFactory
import com.loe159.rekordbot.mobile.domain.queue.UuidQueueOperationIdFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SharePreviewViewModel(
    initialDraft: TrackDraft,
    private val configurationRepository: AirtableConfigurationRepository,
    airtableGateway: AirtableGateway,
    private val pendingTrackRepository: PendingTrackRepository,
    private val queueWorkScheduler: QueueWorkScheduler,
    private val operationIdFactory: QueueOperationIdFactory = UuidQueueOperationIdFactory,
) : ViewModel() {
    private val submitTrack = SubmitTrackToAirtable(
        configurationRepository,
        airtableGateway,
        pendingTrackRepository,
    )
    private val mutableState = MutableStateFlow(SharePreviewUiState(draft = initialDraft))
    val state: StateFlow<SharePreviewUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            val configured = runCatching { configurationRepository.isConnectionValidated() }
                .getOrDefault(false)
            mutableState.update {
                it.copy(
                    isConfigurationLoading = false,
                    isAirtableConfigured = configured,
                )
            }
        }
    }

    fun applyEnrichedDraft(enrichedDraft: TrackDraft) {
        mutableState.update { current ->
            if (current.submissionResult.isComplete()) return@update current
            current.copy(
                draft = current.draft.copy(
                    title = current.draft.title.ifBlank { enrichedDraft.title },
                    artist = current.draft.artist.ifBlank { enrichedDraft.artist },
                    spotifyUrl = current.draft.spotifyUrl.ifBlank { enrichedDraft.spotifyUrl },
                    spotifyTrackId = current.draft.spotifyTrackId.ifBlank {
                        enrichedDraft.spotifyTrackId
                    },
                ),
            )
        }
    }

    fun updateDraft(transform: (TrackDraft) -> TrackDraft) {
        mutableState.update { current ->
            if (
                current.isSubmitting || current.isSavingDraft ||
                current.submissionResult.isComplete() || current.savedDraftOperationId != null
            ) {
                current
            } else {
                current.copy(
                    draft = transform(current.draft),
                    submissionResult = null,
                    draftSaveError = null,
                )
            }
        }
    }

    fun saveDraft() {
        val current = state.value
        if (
            current.isSubmitting || current.isSavingDraft ||
            current.savedDraftOperationId != null
        ) {
            return
        }

        viewModelScope.launch {
            mutableState.update { it.copy(isSavingDraft = true, draftSaveError = null) }
            val operationId = operationIdFactory.create()
            pendingTrackRepository.saveDraft(operationId, current.draft)
                .onSuccess {
                    mutableState.update {
                        it.copy(isSavingDraft = false, savedDraftOperationId = operationId)
                    }
                }
                .onFailure {
                    mutableState.update {
                        it.copy(
                            isSavingDraft = false,
                            draftSaveError = "Impossible d’enregistrer le brouillon localement.",
                        )
                    }
                }
        }
    }

    fun submit() {
        val current = state.value
        if (
            current.isSubmitting || current.isSavingDraft ||
            current.savedDraftOperationId != null || current.submissionResult.isComplete()
        ) {
            return
        }

        viewModelScope.launch {
            mutableState.update { it.copy(isSubmitting = true, submissionResult = null) }
            val result = submitTrack(current.draft)
            if (result is AirtableTrackSubmissionResult.Deferred && result.isPersisted) {
                queueWorkScheduler.schedule()
            }
            mutableState.update {
                it.copy(
                    isSubmitting = false,
                    submissionResult = result,
                    isAirtableConfigured = result !is AirtableTrackSubmissionResult.Failed ||
                        !result.message.startsWith("Configure et teste"),
                )
            }
        }
    }

    companion object {
        fun factory(
            initialDraft: TrackDraft,
            configurationRepository: AirtableConfigurationRepository,
            airtableGateway: AirtableGateway,
            pendingTrackRepository: PendingTrackRepository,
            queueWorkScheduler: QueueWorkScheduler,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SharePreviewViewModel(
                initialDraft = initialDraft,
                configurationRepository = configurationRepository,
                airtableGateway = airtableGateway,
                pendingTrackRepository = pendingTrackRepository,
                queueWorkScheduler = queueWorkScheduler,
            ) as T
        }
    }
}

private fun AirtableTrackSubmissionResult?.isComplete(): Boolean =
    this is AirtableTrackSubmissionResult.Added ||
        (this is AirtableTrackSubmissionResult.Deferred && isPersisted)
