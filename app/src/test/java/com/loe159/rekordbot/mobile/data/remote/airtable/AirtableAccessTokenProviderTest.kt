package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.airtable.AirtableAuthorizationSession
import com.loe159.rekordbot.mobile.domain.airtable.AirtableOAuthConfiguration
import com.loe159.rekordbot.mobile.domain.airtable.AirtableSecret
import com.loe159.rekordbot.mobile.domain.airtable.AirtableTokenSet
import com.loe159.rekordbot.mobile.domain.model.AirtableAuthenticationMode
import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.repository.AirtableSessionRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class AirtableAccessTokenProviderTest {
    @Test
    fun `personal token remains available during migration`() = runBlocking {
        val provider = OAuthAwareAirtableAccessTokenProvider(
            oauthConfiguration = AirtableOAuthConfiguration(clientId = "public-client"),
            sessionRepository = FakeAirtableSessionRepository(),
        )

        val token = provider.accessToken(
            AirtableConfiguration(personalAccessToken = "pat-value"),
        )

        assertEquals("pat-value", token)
    }

    @Test
    fun `expired oauth session is refreshed and persisted before use`() = runBlocking {
        val repository = FakeAirtableSessionRepository(
            tokens = AirtableTokenSet(
                accessToken = AirtableSecret.from("expired"),
                refreshToken = AirtableSecret.from("refresh"),
                expiresAtEpochSeconds = 10,
                scopes = setOf("schema.bases:read"),
            ),
        )
        val provider = OAuthAwareAirtableAccessTokenProvider(
            oauthConfiguration = AirtableOAuthConfiguration(clientId = "public-client"),
            sessionRepository = repository,
            oauthClientFactory = { configuration ->
                AirtableOAuthClient(
                    configuration = configuration,
                    transport = AirtableOAuthTransport {
                        AirtableOAuthHttpResponse(
                            200,
                            """{"access_token":"fresh","refresh_token":"rotated","expires_in":3600}""",
                        )
                    },
                    nowEpochSeconds = { 100 },
                )
            },
            nowEpochSeconds = { 100 },
        )

        val token = provider.accessToken(
            AirtableConfiguration(authenticationMode = AirtableAuthenticationMode.OAUTH),
        )

        assertEquals("fresh", token)
        assertEquals("rotated", repository.tokens?.refreshToken?.reveal())
    }
}

private class FakeAirtableSessionRepository(
    var tokens: AirtableTokenSet? = null,
) : AirtableSessionRepository {
    private var pending: AirtableAuthorizationSession? = null

    override suspend fun loadTokens(): AirtableTokenSet? = tokens

    override suspend fun saveTokens(tokens: AirtableTokenSet) {
        this.tokens = tokens
    }

    override suspend fun clearTokens() {
        tokens = null
    }

    override suspend fun loadPendingAuthorization(): AirtableAuthorizationSession? = pending

    override suspend fun savePendingAuthorization(session: AirtableAuthorizationSession) {
        pending = session
    }

    override suspend fun clearPendingAuthorization() {
        pending = null
    }
}
