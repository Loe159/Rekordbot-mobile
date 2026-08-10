package com.loe159.rekordbot.mobile.ui.share

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableGateway
import com.loe159.rekordbot.mobile.domain.airtable.AirtableTrackSubmissionResult
import com.loe159.rekordbot.mobile.domain.airtable.SubmitTrackToAirtable
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.repository.AirtableConfigurationRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SharePreviewViewModel(
    initialDraft: TrackDraft,
    private val configurationRepository: AirtableConfigurationRepository,
    airtableGateway: AirtableGateway,
) : ViewModel() {
    private val submitTrack = SubmitTrackToAirtable(configurationRepository, airtableGateway)
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
            if (current.submissionResult is AirtableTrackSubmissionResult.Added) return@update current
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
            if (current.isSubmitting || current.submissionResult is AirtableTrackSubmissionResult.Added) {
                current
            } else {
                current.copy(draft = transform(current.draft), submissionResult = null)
            }
        }
    }

    fun submit() {
        val current = state.value
        if (current.isSubmitting || current.submissionResult is AirtableTrackSubmissionResult.Added) return

        viewModelScope.launch {
            mutableState.update { it.copy(isSubmitting = true, submissionResult = null) }
            val result = submitTrack(current.draft)
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
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SharePreviewViewModel(
                initialDraft = initialDraft,
                configurationRepository = configurationRepository,
                airtableGateway = airtableGateway,
            ) as T
        }
    }
}
