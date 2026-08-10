package com.loe159.rekordbot.mobile.ui

import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class UserFacingErrorsTest {
    @Test
    fun `unexpected diagnostic is not exposed`() {
        val diagnostic = "token=secret; stack frame 42"

        val message = IllegalStateException(diagnostic).toUserFacingMessage(
            "Action impossible. Réessaie.",
        )

        assertEquals("Action impossible. Réessaie.", message)
        assertFalse(message.contains(diagnostic))
    }

    @Test
    fun `curated Airtable error stays actionable`() {
        val message = AirtableApiException(
            "Token Airtable invalide ou révoqué.",
            statusCode = 401,
        ).toUserFacingMessage("Connexion impossible.")

        assertEquals("Token Airtable invalide ou révoqué.", message)
    }
}
