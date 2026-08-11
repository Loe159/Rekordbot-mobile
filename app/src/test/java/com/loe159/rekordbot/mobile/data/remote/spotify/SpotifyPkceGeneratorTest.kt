package com.loe159.rekordbot.mobile.data.remote.spotify

import com.loe159.rekordbot.mobile.domain.spotify.SpotifyConfiguration
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SpotifyPkceGeneratorTest {
    @Test
    fun `creates S256 authorization URL with playlist and playback scopes`() {
        val generator = SpotifyPkceGenerator(
            SpotifyRandomBytesProvider { size -> ByteArray(size) { index -> index.toByte() } },
        )

        val session = generator.createAuthorizationSession(
            SpotifyConfiguration(clientId = "client-id"),
        )
        val parameters = URI(session.authorizationUrl).rawQuery.split('&').associate { part ->
            val (key, value) = part.split('=', limit = 2)
            URLDecoder.decode(key, StandardCharsets.UTF_8) to
                URLDecoder.decode(value, StandardCharsets.UTF_8)
        }

        assertEquals("code", parameters["response_type"])
        assertEquals("client-id", parameters["client_id"])
        assertEquals(SpotifyConfiguration.DEFAULT_REDIRECT_URI, parameters["redirect_uri"])
        assertEquals("S256", parameters["code_challenge_method"])
        assertEquals(
            "playlist-read-private user-modify-playback-state",
            parameters["scope"],
        )
        assertEquals(session.state, parameters["state"])
        assertEquals("[REDACTED]", session.codeVerifier.toString())
        assertTrue(generator.isExpectedState(session.state, session.state))
        assertFalse(generator.isExpectedState(session.state, "another-state"))
    }

    @Test
    fun `matches RFC 7636 S256 example`() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            SpotifyPkceGenerator.codeChallenge(
                "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk",
            ),
        )
    }
}
