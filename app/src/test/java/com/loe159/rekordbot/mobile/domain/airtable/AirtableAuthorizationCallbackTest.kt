package com.loe159.rekordbot.mobile.domain.airtable

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AirtableAuthorizationCallbackTest {
    private val redirectUri = "https://api.rekordbot.example/oauth/airtable/callback"

    @Test
    fun `accepts the configured HTTPS callback with OAuth parameters`() {
        assertTrue(
            AirtableAuthorizationCallback.isExpected(
                "$redirectUri?code=authorization-code&state=expected-state",
                redirectUri,
            ),
        )
    }

    @Test
    fun `rejects a callback from another origin or path`() {
        assertFalse(
            AirtableAuthorizationCallback.isExpected(
                "https://attacker.example/oauth/airtable/callback?code=code&state=state",
                redirectUri,
            ),
        )
        assertFalse(
            AirtableAuthorizationCallback.isExpected(
                "https://api.rekordbot.example/oauth/airtable/other?code=code&state=state",
                redirectUri,
            ),
        )
    }

    @Test
    fun `rejects custom schemes fragments and an unconfigured callback`() {
        assertFalse(
            AirtableAuthorizationCallback.isExpected(
                "rekordbot-mobile-login://airtable-callback?code=code&state=state",
                redirectUri,
            ),
        )
        assertFalse(
            AirtableAuthorizationCallback.isExpected(
                "$redirectUri?code=code&state=state#unexpected",
                redirectUri,
            ),
        )
        assertFalse(AirtableAuthorizationCallback.isExpected(redirectUri, ""))
    }

    @Test
    fun `only accepts an HTTPS redirect without embedded query or fragment`() {
        assertTrue(AirtableAuthorizationCallback.isSecureRedirectUri(redirectUri))
        assertFalse(
            AirtableAuthorizationCallback.isSecureRedirectUri(
                "http://api.rekordbot.example/oauth/airtable/callback",
            ),
        )
        assertFalse(
            AirtableAuthorizationCallback.isSecureRedirectUri(
                "$redirectUri?unexpected=parameter",
            ),
        )
    }
}
