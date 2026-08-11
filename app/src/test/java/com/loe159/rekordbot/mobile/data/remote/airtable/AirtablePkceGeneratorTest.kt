package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.airtable.AirtableOAuthConfiguration
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AirtablePkceGeneratorTest {
    @Test
    fun `creates public client authorization URL with exact scopes`() {
        val generator = AirtablePkceGenerator(
            AirtableRandomBytesProvider { size -> ByteArray(size) { it.toByte() } },
        )

        val session = generator.createAuthorizationSession(
            AirtableOAuthConfiguration(
                clientId = "airtable-client",
                redirectUri = "https://api.rekordbot.example/oauth/airtable/callback",
            ),
        )
        val parameters = URI(session.authorizationUrl).rawQuery.split('&').associate { part ->
            val (key, value) = part.split('=', limit = 2)
            URLDecoder.decode(key, StandardCharsets.UTF_8) to
                URLDecoder.decode(value, StandardCharsets.UTF_8)
        }

        assertEquals("code", parameters["response_type"])
        assertEquals("airtable-client", parameters["client_id"])
        assertEquals(
            "https://api.rekordbot.example/oauth/airtable/callback",
            parameters["redirect_uri"],
        )
        assertEquals("S256", parameters["code_challenge_method"])
        assertEquals(
            "data.records:read data.records:write schema.bases:read",
            parameters["scope"],
        )
        assertEquals(session.state, parameters["state"])
        assertTrue(generator.isExpectedState(session.state, session.state))
        assertFalse(generator.isExpectedState(session.state, "wrong"))
        assertFalse(session.codeVerifier.toString().contains(session.codeVerifier.reveal()))
    }

    @Test
    fun `matches RFC 7636 S256 example`() {
        assertEquals(
            "E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM",
            AirtablePkceGenerator.codeChallenge(
                "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk",
            ),
        )
    }

    @Test
    fun `rejects a non HTTPS redirect URI`() {
        val generator = AirtablePkceGenerator(
            AirtableRandomBytesProvider { size -> ByteArray(size) { it.toByte() } },
        )

        assertThrows(IllegalArgumentException::class.java) {
            generator.createAuthorizationSession(
                AirtableOAuthConfiguration(
                    clientId = "airtable-client",
                    redirectUri = "rekordbot-mobile-login://airtable-callback",
                ),
            )
        }
    }
}
