package com.loe159.rekordbot.mobile.domain.queue

import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableGateway
import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableApiException
import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.AirtableTableSchema
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.repository.AirtableConfigurationRepository
import com.loe159.rekordbot.mobile.domain.repository.PendingTrackRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class QueuedTrackProcessorTest {
    @Test
    fun `retry resolves uncertain send by Spotify id without creating again`() = runBlocking {
        val pendingRepository = ProcessorPendingRepository(operation(attemptCount = 2))
        val gateway = ProcessorGateway(existingRecordId = "rec-existing")
        val processor = QueuedTrackProcessor(
            pendingRepository,
            ProcessorConfigurationRepository,
            gateway,
        )

        val result = processor.processNext()

        assertEquals(QueueProcessingResult.Sent, result)
        assertEquals("operation-id" to "rec-existing", pendingRepository.sent)
        assertFalse(gateway.createCalled)
    }

    @Test
    fun `retry never creates when idempotence probe fails`() = runBlocking {
        val pendingRepository = ProcessorPendingRepository(operation(attemptCount = 2))
        val gateway = ProcessorGateway(
            existingRecordId = null,
            findResult = Result.failure(AirtableApiException("Réseau indisponible")),
        )
        val processor = QueuedTrackProcessor(
            pendingRepository,
            ProcessorConfigurationRepository,
            gateway,
        )

        val result = processor.processNext()

        assertEquals(QueueProcessingResult.RetryLater, result)
        assertEquals(true, pendingRepository.failed?.third)
        assertFalse(gateway.createCalled)
    }
}

private fun operation(attemptCount: Int) = QueuedTrackOperation(
    operationId = "operation-id",
    draft = TrackDraft(
        spotifyTrackId = "spotify-id",
        title = "Open Eye Signal",
        artist = "Jon Hopkins",
        spotifyUrl = "https://open.spotify.com/track/spotify-id",
    ),
    status = QueueStatus.SENDING,
    attemptCount = attemptCount,
    lastError = null,
    willRetryAutomatically = false,
    createdAt = 1L,
    updatedAt = 2L,
    lastAttemptAt = 2L,
    sentAt = null,
    airtableRecordId = null,
)

private class ProcessorPendingRepository(
    private var next: QueuedTrackOperation?,
) : PendingTrackRepository {
    var sent: Pair<String, String>? = null
    var failed: Triple<String, String, Boolean>? = null

    override fun observeAll(): Flow<List<QueuedTrackOperation>> = flowOf(emptyList())
    override fun observeOpenCount(): Flow<Int> = flowOf(0)
    override suspend fun enqueue(
        operationId: String,
        track: TrackDraft,
        initialError: String,
    ): Result<String> = Result.success(operationId)
    override suspend fun claimNext(): QueuedTrackOperation? = next.also { next = null }
    override suspend fun markSent(operationId: String, recordId: String) {
        sent = operationId to recordId
    }
    override suspend fun markFailed(operationId: String, error: String, retryable: Boolean) {
        failed = Triple(operationId, error, retryable)
    }
    override suspend fun retry(operationId: String): Boolean = true
    override suspend fun delete(operationId: String): Boolean = true
    override suspend fun updateDraft(operationId: String, draft: TrackDraft): Boolean = true
    override suspend fun recoverInterrupted() = Unit
}

private object ProcessorConfigurationRepository : AirtableConfigurationRepository {
    override suspend fun load(): AirtableConfiguration = AirtableConfiguration(
        personalAccessToken = "pat-test",
        baseId = "app-test",
        table = "Tracks",
    )
    override suspend fun save(configuration: AirtableConfiguration) = Unit
    override suspend fun isConnectionValidated(): Boolean = true
    override suspend fun markConnectionValidated() = Unit
}

private class ProcessorGateway(
    private val existingRecordId: String?,
    private val findResult: Result<String?> = Result.success(existingRecordId),
) : AirtableGateway {
    var createCalled = false

    override suspend fun fetchTableSchema(
        configuration: AirtableConfiguration,
    ): Result<AirtableTableSchema> = error("Not used")
    override suspend fun createDemoRecord(
        configuration: AirtableConfiguration,
    ): Result<String> = error("Not used")
    override suspend fun findRecordBySpotifyTrackId(
        configuration: AirtableConfiguration,
        spotifyTrackId: String,
    ): Result<String?> = findResult
    override suspend fun createTrackRecord(
        configuration: AirtableConfiguration,
        track: TrackDraft,
    ): Result<String> {
        createCalled = true
        return Result.success("rec-created")
    }
}
