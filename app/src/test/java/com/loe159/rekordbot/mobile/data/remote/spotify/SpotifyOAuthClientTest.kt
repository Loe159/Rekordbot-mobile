package com.loe159.rekordbot.mobile.data.remote.spotify

import com.loe159.rekordbot.mobile.domain.spotify.SpotifyConfiguration
import com.loe159.rekordbot.mobile.domain.spotify.SpotifySecret
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyTokenSet
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyOAuthClientTest {
    @Test
    fun `authorization exchange uses PKCE without client secret`() = runBlocking {
        var captured: SpotifyHttpRequest? = null
        val client = SpotifyOAuthClient(
            configuration = SpotifyConfiguration(clientId = "client-id"),
            transport = SpotifyHttpTransport { request ->
                captured = request
                SpotifyHttpResponse(
                    200,
                    """{"access_token":"access","token_type":"Bearer","expires_in":3600,"refresh_token":"refresh","scope":"playlist-read-private"}""",
                )
            },
            nowEpochSeconds = { 1_000L },
        )

        val result = client.exchangeCode("auth-code", SpotifySecret.from("verifier-value"))

        assertTrue(result.isSuccess)
        assertEquals(4_600L, result.getOrThrow().expiresAtEpochSeconds)
        assertTrue(captured?.body.orEmpty().contains("code_verifier=verifier-value"))
        assertTrue(captured?.body.orEmpty().contains("client_id=client-id"))
        assertFalse(captured?.body.orEmpty().contains("client_secret"))
        assertFalse(captured.toString().contains("verifier-value"))
        assertFalse(captured.toString().contains("auth-code"))
    }

    @Test
    fun `refresh preserves refresh token and scopes when Spotify omits both`() = runBlocking {
        val client = SpotifyOAuthClient(
            configuration = SpotifyConfiguration(clientId = "client-id"),
            transport = SpotifyHttpTransport {
                SpotifyHttpResponse(200, """{"access_token":"new-access","expires_in":3600}""")
            },
            nowEpochSeconds = { 10L },
        )
        val current = SpotifyTokenSet(
            accessToken = SpotifySecret.from("old-access"),
            refreshToken = SpotifySecret.from("refresh"),
            expiresAtEpochSeconds = 20L,
            scopes = setOf("playlist-read-private"),
        )

        val refreshed = client.refreshTokens(current).getOrThrow()

        assertEquals("refresh", refreshed.refreshToken?.reveal())
        assertEquals(setOf("playlist-read-private"), refreshed.scopes)
    }
}
