package com.loe159.rekordbot.mobile.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TrackDraftTest {
    @Test
    fun `draft is ready when required Spotify fields are present`() {
        val draft = TrackDraft(
            spotifyTrackId = "5lFNqg3eMNMuJsnFRKB460",
            title = "Open Eye Signal",
            artist = "Jon Hopkins",
            spotifyUrl = "https://open.spotify.com/track/5lFNqg3eMNMuJsnFRKB460",
        )

        assertTrue(draft.isReadyForAirtable)
    }

    @Test
    fun `draft is not ready when title is blank`() {
        val draft = TrackDraft(
            spotifyTrackId = "5lFNqg3eMNMuJsnFRKB460",
            title = " ",
            artist = "Jon Hopkins",
            spotifyUrl = "https://open.spotify.com/track/5lFNqg3eMNMuJsnFRKB460",
        )

        assertFalse(draft.isReadyForAirtable)
    }
}

