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
import com.loe159.rekordbot.mobile.domain.repository.SoundchartsConfigurationRepository
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsDraftEnricher
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsGateway
import com.loe159.rekordbot.mobile.domain.queue.QueueWorkScheduler
import com.loe159.rekordbot.mobile.domain.queue.QueueOperationIdFactory
import com.loe159.rekordbot.mobile.domain.queue.UuidQueueOperationIdFactory
import com.loe159.rekordbot.mobile.ui.toUserFacingMessage
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
    private val soundchartsConfigurationRepository: SoundchartsConfigurationRepository,
    private val soundchartsGateway: SoundchartsGateway,
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
        enrichWithSoundcharts()
    }

    private fun enrichWithSoundcharts() {
        val trackId = state.value.draft.spotifyTrackId
        if (trackId.isBlank()) return
        viewModelScope.launch {
            val configuration = runCatching { soundchartsConfigurationRepository.load() }
                .getOrElse {
                    mutableState.update {
                        it.copy(soundchartsMessage = "Enrichissement Soundcharts indisponible.")
                    }
                    return@launch
                }
            if (!configuration.enabled || !configuration.isComplete) return@launch
            mutableState.update {
                it.copy(
                    isSoundchartsEnriching = true,
                    soundchartsMessage = "Recherche du genre et de l’ISRC dans Soundcharts…",
                )
            }
            soundchartsGateway.fetchTrackMetadata(trackId, configuration)
                .onSuccess { metadata ->
                    mutableState.update { current ->
                        if (current.draft.spotifyTrackId != trackId) {
                            return@update current.copy(
                                isSoundchartsEnriching = false,
                                soundchartsMessage = "Track ID modifié : résultat Soundcharts ignoré.",
                            )
                        }
                        val enrichment = SoundchartsDraftEnricher.merge(current.draft, metadata)
                        current.copy(
                            draft = enrichment.draft,
                            isSoundchartsEnriching = false,
                            suggestedRawGenre = enrichment.genreSuggestion,
                            soundchartsMessage = if (enrichment.genreSuggestion != null) {
                                "Soundcharts propose un autre genre. Ta saisie a été conservée."
                            } else {
                                "Métadonnées Soundcharts ajoutées quand les champs étaient vides."
                            },
                        )
                    }
                }
                .onFailure { error ->
                    mutableState.update {
                        it.copy(
                            isSoundchartsEnriching = false,
                            soundchartsMessage = error.toUserFacingMessage(
                                "Enrichissement Soundcharts indisponible. Vérifie le réseau ou désactive-le dans les réglages.",
                            ) + " L’envoi reste disponible.",
                        )
                    }
                }
        }
    }

    fun replaceGenreWithSuggestion() {
        mutableState.update { current ->
            val suggestion = current.suggestedRawGenre ?: return@update current
            if (current.submissionResult.isComplete()) return@update current
            current.copy(
                draft = current.draft.copy(rawGenre = suggestion),
                suggestedRawGenre = null,
                soundchartsMessage = "Genre remplacé par la suggestion Soundcharts.",
            )
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
            soundchartsConfigurationRepository: SoundchartsConfigurationRepository,
            soundchartsGateway: SoundchartsGateway,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = SharePreviewViewModel(
                initialDraft = initialDraft,
                configurationRepository = configurationRepository,
                airtableGateway = airtableGateway,
                pendingTrackRepository = pendingTrackRepository,
                queueWorkScheduler = queueWorkScheduler,
                soundchartsConfigurationRepository = soundchartsConfigurationRepository,
                soundchartsGateway = soundchartsGateway,
            ) as T
        }
    }
}

private fun AirtableTrackSubmissionResult?.isComplete(): Boolean =
    this is AirtableTrackSubmissionResult.Added ||
        (this is AirtableTrackSubmissionResult.Deferred && isPersisted)
