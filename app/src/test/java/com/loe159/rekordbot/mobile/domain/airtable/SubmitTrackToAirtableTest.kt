package com.loe159.rekordbot.mobile.domain.airtable

import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableApiException
import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableGateway
import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.AirtableTableSchema
import com.loe159.rekordbot.mobile.domain.model.DuplicateStrategy
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.repository.AirtableConfigurationRepository
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
