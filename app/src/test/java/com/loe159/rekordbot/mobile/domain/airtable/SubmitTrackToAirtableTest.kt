package com.loe159.rekordbot.mobile.domain.airtable

import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableApiException
import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableGateway
import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.AirtableTableSchema
import com.loe159.rekordbot.mobile.domain.model.DuplicateStrategy
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.repository.AirtableConfigurationRepository
import com.loe159.rekordbot.mobile.domain.repository.PendingTrackRepository
import com.loe159.rekordbot.mobile.domain.queue.QueuedTrackOperation
import com.loe159.rekordbot.mobile.domain.queue.QueueOperationIdFactory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmitTrackToAirtableTest {
    private val draft = TrackDraft(
        spotifyTrackId = "spotify-id",
        title = "Open Eye Signal",
        artist = "Jon Hopkins",
        spotifyUrl = "https://open.spotify.com/track/spotify-id",
    )

    @Test
    fun `block strategy returns existing duplicate without creating`() = runBlocking {
        val gateway = FakeAirtableGateway(existingRecordId = "rec-existing")
        val useCase = SubmitTrackToAirtable(
            FakeConfigurationRepository(configuration()),
            gateway,
        )

        val result = useCase(draft)

        assertEquals(AirtableTrackSubmissionResult.DuplicateBlocked("rec-existing"), result)
        assertFalse(gateway.createCalled)
    }

    @Test
    fun `allow strategy skips duplicate lookup and creates record`() = runBlocking {
        val gateway = FakeAirtableGateway(existingRecordId = "rec-existing")
        val useCase = SubmitTrackToAirtable(
            FakeConfigurationRepository(configuration(DuplicateStrategy.ALLOW)),
            gateway,
        )

        val result = useCase(draft)

        assertEquals(AirtableTrackSubmissionResult.Added("rec-created"), result)
        assertFalse(gateway.findCalled)
        assertTrue(gateway.createCalled)
    }

    @Test
    fun `network failure is deferred explicitly without local persistence`() = runBlocking {
        val gateway = FakeAirtableGateway(
            createResult = Result.failure(AirtableApiException("Réseau indisponible")),
        )
        val useCase = SubmitTrackToAirtable(
            FakeConfigurationRepository(configuration()),
            gateway,
        )

        val result = useCase(draft)

        assertTrue(result is AirtableTrackSubmissionResult.Deferred)
        assertFalse((result as AirtableTrackSubmissionResult.Deferred).isPersisted)
        assertEquals("Réseau indisponible", result.reason)
    }

    @Test
    fun `network failure is persisted with stable operation id when queue is available`() = runBlocking {
        val queue = FakePendingTrackRepository()
        val gateway = FakeAirtableGateway(
            createResult = Result.failure(AirtableApiException("Réseau indisponible")),
        )
        val useCase = SubmitTrackToAirtable(
            FakeConfigurationRepository(configuration()),
            gateway,
            queue,
            QueueOperationIdFactory { "operation-stable" },
        )

        val result = useCase(draft) as AirtableTrackSubmissionResult.Deferred

        assertTrue(result.isPersisted)
        assertEquals("operation-stable", result.operationId)
        assertEquals("operation-stable", queue.enqueuedOperationId)
        assertEquals(draft, queue.enqueuedDraft)
    }

    @Test
    fun `non retryable Airtable failure is detailed`() = runBlocking {
        val gateway = FakeAirtableGateway(
            createResult = Result.failure(AirtableApiException("Champ invalide", 422)),
        )
        val useCase = SubmitTrackToAirtable(
            FakeConfigurationRepository(configuration()),
            gateway,
        )

        assertEquals(AirtableTrackSubmissionResult.Failed("Champ invalide"), useCase(draft))
    }

    private fun configuration(
        duplicateStrategy: DuplicateStrategy = DuplicateStrategy.BLOCK,
    ) = AirtableConfiguration(
        personalAccessToken = "pat-test",
        baseId = "app-test",
        table = "Tracks",
        duplicateStrategy = duplicateStrategy,
    )
}

private class FakePendingTrackRepository : PendingTrackRepository {
    var enqueuedOperationId: String? = null
    var enqueuedDraft: TrackDraft? = null

    override fun observeAll(): Flow<List<QueuedTrackOperation>> = flowOf(emptyList())
    override fun observeOpenCount(): Flow<Int> = flowOf(0)
    override suspend fun enqueue(
        operationId: String,
        track: TrackDraft,
        initialError: String,
    ): Result<String> {
        enqueuedOperationId = operationId
        enqueuedDraft = track
        return Result.success(operationId)
    }
    override suspend fun claimNext(): QueuedTrackOperation? = null
    override suspend fun markSent(operationId: String, recordId: String) = Unit
    override suspend fun markFailed(operationId: String, error: String, retryable: Boolean) = Unit
    override suspend fun retry(operationId: String): Boolean = false
    override suspend fun delete(operationId: String): Boolean = false
    override suspend fun updateDraft(operationId: String, draft: TrackDraft): Boolean = false
    override suspend fun recoverInterrupted() = Unit
}

private class FakeConfigurationRepository(
    private var configuration: AirtableConfiguration,
    private val validated: Boolean = true,
) : AirtableConfigurationRepository {
    override suspend fun load(): AirtableConfiguration = configuration

    override suspend fun save(configuration: AirtableConfiguration) {
        this.configuration = configuration
    }

    override suspend fun isConnectionValidated(): Boolean = validated

    override suspend fun markConnectionValidated() = Unit
}

private class FakeAirtableGateway(
    private val existingRecordId: String? = null,
    private val createResult: Result<String> = Result.success("rec-created"),
) : AirtableGateway {
    var findCalled = false
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
    ): Result<String?> {
        findCalled = true
        return Result.success(existingRecordId)
    }

    override suspend fun createTrackRecord(
        configuration: AirtableConfiguration,
        track: TrackDraft,
    ): Result<String> {
        createCalled = true
        return createResult
    }
}
