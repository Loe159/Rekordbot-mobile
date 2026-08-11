package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.airtable.AirtableAuthorizationSession
import com.loe159.rekordbot.mobile.domain.airtable.AirtableOAuthConfiguration
import com.loe159.rekordbot.mobile.domain.airtable.AirtableSecret
import com.loe159.rekordbot.mobile.domain.airtable.AirtableTokenSet
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AirtableOAuthClientTest {
    @Test
    fun `authorization exchange uses PKCE without client secret`() = runBlocking {
        var captured: AirtableOAuthHttpRequest? = null
        val client = AirtableOAuthClient(
            configuration = AirtableOAuthConfiguration(clientId = "client-id"),
            transport = AirtableOAuthTransport { request ->
                captured = request
                AirtableOAuthHttpResponse(
                    200,
                    """{"access_token":"access","refresh_token":"refresh","expires_in":3600,"scope":"schema.bases:read data.records:read data.records:write"}""",
                )
            },
            nowEpochSeconds = { 1_000L },
        )
        val session = AirtableAuthorizationSession(
            authorizationUrl = "https://airtable.test",
            state = "expected",
            codeVerifier = AirtableSecret.from("verifier-value"),
        )

        val result = client.exchangeAuthorizationCode("auth-code", "expected", session)

        assertTrue(result.isSuccess)
        assertEquals(4_600L, result.getOrThrow().expiresAtEpochSeconds)
        assertTrue(captured?.body.orEmpty().contains("code_verifier=verifier-value"))
        assertTrue(captured?.body.orEmpty().contains("client_id=client-id"))
        assertFalse(captured?.body.orEmpty().contains("client_secret"))
        assertFalse(captured.toString().contains("verifier-value"))
        assertFalse(captured.toString().contains("auth-code"))
    }

    @Test
    fun `refresh immediately keeps the rotated refresh token`() = runBlocking {
        val client = AirtableOAuthClient(
            configuration = AirtableOAuthConfiguration(clientId = "client-id"),
            transport = AirtableOAuthTransport {
                AirtableOAuthHttpResponse(
                    200,
                    """{"access_token":"new-access","refresh_token":"new-refresh","expires_in":3600}""",
                )
            },
            nowEpochSeconds = { 10L },
        )
        val current = AirtableTokenSet(
            accessToken = AirtableSecret.from("old-access"),
            refreshToken = AirtableSecret.from("old-refresh"),
            expiresAtEpochSeconds = 20L,
            scopes = setOf("schema.bases:read"),
        )

        val refreshed = client.refreshTokens(current).getOrThrow()

        assertEquals("new-refresh", refreshed.refreshToken.reveal())
        assertEquals(setOf("schema.bases:read"), refreshed.scopes)
    }
}
