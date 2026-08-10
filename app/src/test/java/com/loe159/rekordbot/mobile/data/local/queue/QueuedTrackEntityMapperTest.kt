package com.loe159.rekordbot.mobile.data.local.queue

import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.queue.QueueStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QueuedTrackEntityMapperTest {
    @Test
    fun `draft mapping keeps operation id data and timestamps`() {
        val draft = TrackDraft(
            spotifyTrackId = "spotify-id",
            title = "Open Eye Signal",
            artist = "Jon Hopkins",
            spotifyUrl = "https://open.spotify.com/track/spotify-id",
            rawGenre = "Electronic",
            comment = "Closing",
        )

        val entity = draft.toEntity("operation-id", "Hors ligne", 42L)
        val operation = entity.toDomain()

        assertEquals("operation-id", operation.operationId)
        assertEquals(draft, operation.draft)
        assertEquals(QueueStatus.PENDING, operation.status)
        assertEquals(0, operation.attemptCount)
        assertEquals(42L, operation.createdAt)
        assertEquals("Hors ligne", operation.lastError)
        assertEquals(true, operation.willRetryAutomatically)
        assertNull(operation.airtableRecordId)
    }
}
