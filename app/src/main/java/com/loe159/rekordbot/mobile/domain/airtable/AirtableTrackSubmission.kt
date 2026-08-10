package com.loe159.rekordbot.mobile.domain.airtable

import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableApiException
import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableGateway
import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.AirtableConfigurationValidator
import com.loe159.rekordbot.mobile.domain.model.DuplicateStrategy
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.repository.AirtableConfigurationRepository
import com.loe159.rekordbot.mobile.domain.queue.QueueOperationIdFactory
import com.loe159.rekordbot.mobile.domain.queue.UuidQueueOperationIdFactory
import com.loe159.rekordbot.mobile.domain.repository.PendingTrackRepository

sealed interface AirtableTrackSubmissionResult {
    data class Added(val recordId: String) : AirtableTrackSubmissionResult

    data class DuplicateBlocked(val recordId: String) : AirtableTrackSubmissionResult

    data class Failed(val message: String) : AirtableTrackSubmissionResult

    /** P4 will replace this transient state with a durable Room queue. */
    data class Deferred(
        val reason: String,
        val isPersisted: Boolean = false,
        val operationId: String? = null,
    ) : AirtableTrackSubmissionResult
}

class SubmitTrackToAirtable(
    private val configurationRepository: AirtableConfigurationRepository,
    private val airtableGateway: AirtableGateway,
    private val pendingTrackRepository: PendingTrackRepository? = null,
    private val operationIdFactory: QueueOperationIdFactory = UuidQueueOperationIdFactory,
) {
    suspend operator fun invoke(draft: TrackDraft): AirtableTrackSubmissionResult {
        if (!draft.isReadyForAirtable) {
            return AirtableTrackSubmissionResult.Failed(
                "Complète le titre, l’artiste, le lien Spotify et le Track ID.",
            )
        }

        val configuration = runCatching { configurationRepository.load() }
            .getOrElse {
                return AirtableTrackSubmissionResult.Failed(
                    "Impossible de lire la configuration Airtable.",
                )
            }
        val configurationErrors = AirtableConfigurationValidator.localErrors(configuration)
        val isConnectionValidated = runCatching {
            configurationRepository.isConnectionValidated()
        }.getOrDefault(false)
        if (configurationErrors.isNotEmpty() || !isConnectionValidated) {
            return AirtableTrackSubmissionResult.Failed(
                "Configure et teste la connexion Airtable avant d’ajouter un morceau.",
            )
        }

        if (configuration.duplicateStrategy == DuplicateStrategy.BLOCK) {
            airtableGateway.findRecordBySpotifyTrackId(configuration, draft.spotifyTrackId)
                .fold(
                    onSuccess = { existingRecordId ->
                        if (existingRecordId != null) {
                            return AirtableTrackSubmissionResult.DuplicateBlocked(existingRecordId)
                        }
                    },
                    onFailure = { return deferOrFail(draft, it) },
                )
        }

        return airtableGateway.createTrackRecord(configuration, draft).fold(
            onSuccess = { AirtableTrackSubmissionResult.Added(it) },
            onFailure = { deferOrFail(draft, it) },
        )
    }

    private suspend fun deferOrFail(
        draft: TrackDraft,
        error: Throwable,
    ): AirtableTrackSubmissionResult {
        val message = error.message?.takeIf(String::isNotBlank)
            ?: "Impossible d’ajouter le morceau à Airtable."
        if (error !is AirtableApiException || !error.isRetryable) {
            return AirtableTrackSubmissionResult.Failed(message)
        }
        val repository = pendingTrackRepository
            ?: return AirtableTrackSubmissionResult.Deferred(reason = message)
        val operationId = operationIdFactory.create()
        return repository.enqueue(operationId, draft, message).fold(
            onSuccess = {
                AirtableTrackSubmissionResult.Deferred(
                    reason = message,
                    isPersisted = true,
                    operationId = operationId,
                )
            },
            onFailure = {
                AirtableTrackSubmissionResult.Failed(
                    "L’envoi a échoué et n’a pas pu être conservé localement.",
                )
            },
        )
    }
}
