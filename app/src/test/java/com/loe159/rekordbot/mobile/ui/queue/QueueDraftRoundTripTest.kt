package com.loe159.rekordbot.mobile.ui.queue

import com.loe159.rekordbot.mobile.data.local.queue.toDomain
import com.loe159.rekordbot.mobile.data.local.queue.toEntity
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.queue.QueueStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QueueDraftRoundTripTest {
    @Test
    fun `DJ fields survive Room domain and editor state roundtrip`() {
        val draft = TrackDraft(
            spotifyTrackId = "spotify-id",
            title = "Open Eye Signal",
            artist = "Jon Hopkins",
            spotifyUrl = "https://open.spotify.com/track/spotify-id",
            isrc = "GB-CEL-21-00001",
            energy = 4,
            moods = listOf("Calme", "Mystérieux"),
            situations = listOf("Warm-up", "Sunset"),
            inspirationalDjs = listOf("ANOTR", "Adam Ten"),
            comment = "Longue intro",
        )

        val editor = draft.toEntity(
            operationId = "draft-id",
            initialError = "",
            now = 42L,
            status = QueueStatus.DRAFT,
        ).toDomain().toEditorState()

        assertEquals(draft, editor.draft)
        assertEquals(QueueStatus.DRAFT, editor.originalStatus)
        assertTrue(editor.canSave)
    }

    @Test
    fun `incomplete local draft stays editable but cannot be sent`() {
        val editor = QueueEditorState(
            operationId = "draft-id",
            originalStatus = QueueStatus.DRAFT,
            draft = TrackDraft("spotify-id", "", "", "", energy = 3),
        )

        assertTrue(editor.canSave)
        assertEquals(false, editor.draft.isReadyForAirtable)
    }
}
