package com.loe159.rekordbot.mobile.domain.queue

import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableApiException
import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableGateway
import com.loe159.rekordbot.mobile.domain.airtable.AirtableTrackSubmissionResult
import com.loe159.rekordbot.mobile.domain.airtable.SubmitTrackToAirtable
import com.loe159.rekordbot.mobile.domain.repository.AirtableConfigurationRepository
import com.loe159.rekordbot.mobile.domain.repository.PendingTrackRepository

sealed interface QueueProcessingResult {
    data object Empty : QueueProcessingResult
    data object Sent : QueueProcessingResult
    data object RetryLater : QueueProcessingResult
    data object PermanentlyFailed : QueueProcessingResult
}

class QueuedTrackProcessor(
    private val pendingTrackRepository: PendingTrackRepository,
    private val configurationRepository: AirtableConfigurationRepository,
    private val airtableGateway: AirtableGateway,
) {
    private val submitTrack = SubmitTrackToAirtable(configurationRepository, airtableGateway)

    suspend fun processNext(): QueueProcessingResult {
        val operation = pendingTrackRepository.claimNext() ?: return QueueProcessingResult.Empty

        // An earlier request may have reached Airtable before its response was lost. On retries,
        // the Spotify ID probe turns that uncertain result into a successful, idempotent send.
        if (operation.attemptCount > 1) {
            val configuration = runCatching { configurationRepository.load() }.getOrElse {
                pendingTrackRepository.markFailed(
                    operation.operationId,
                    "Impossible de lire la configuration Airtable avant la reprise.",
                    retryable = true,
                )
                return QueueProcessingResult.RetryLater
            }
            val duplicateProbe = airtableGateway.findRecordBySpotifyTrackId(
                configuration,
                operation.draft.spotifyTrackId,
            )
            duplicateProbe.exceptionOrNull()?.let { error ->
                val retryable = error !is AirtableApiException || error.isRetryable
                pendingTrackRepository.markFailed(
                    operation.operationId,
                    error.message ?: "Impossible de vérifier l’idempotence de l’envoi.",
                    retryable = retryable,
                )
                return if (!retryable) {
                    QueueProcessingResult.PermanentlyFailed
                } else {
                    QueueProcessingResult.RetryLater
                }
            }
            duplicateProbe.getOrNull()?.let { existing ->
                pendingTrackRepository.markSent(operation.operationId, existing)
                return QueueProcessingResult.Sent
            }
        }

        return when (val result = submitTrack(operation.draft)) {
            is AirtableTrackSubmissionResult.Added -> {
                pendingTrackRepository.markSent(operation.operationId, result.recordId)
                QueueProcessingResult.Sent
            }
            is AirtableTrackSubmissionResult.DuplicateBlocked -> {
                pendingTrackRepository.markSent(operation.operationId, result.recordId)
                QueueProcessingResult.Sent
            }
            is AirtableTrackSubmissionResult.Deferred -> {
                pendingTrackRepository.markFailed(
                    operation.operationId,
                    result.reason,
                    retryable = true,
                )
                QueueProcessingResult.RetryLater
            }
            is AirtableTrackSubmissionResult.Failed -> {
                pendingTrackRepository.markFailed(
                    operation.operationId,
                    result.message,
                    retryable = false,
                )
                QueueProcessingResult.PermanentlyFailed
            }
        }
    }
}
