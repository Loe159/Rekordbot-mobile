package com.loe159.rekordbot.mobile.domain.soundcharts

import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoundchartsDraftEnricherTest {
    @Test
    fun `fills only empty genre and isrc`() {
        val merged = SoundchartsDraftEnricher.merge(
            current = draft(),
            metadata = SoundchartsMetadata(listOf("Electronic", "House"), "GB-NEW-01"),
        )

        assertEquals("Electronic, House", merged.draft.rawGenre)
        assertEquals("GB-NEW-01", merged.draft.isrc)
        assertNull(merged.genreSuggestion)
    }

    @Test
    fun `manual edits made before response win and genre becomes explicit suggestion`() {
        val latestEditedDraft = draft().copy(
            title = "Titre corrigé pendant la requête",
            rawGenre = "Afro House manuel",
            isrc = "ISRC-MANUEL",
            comment = "Édité pendant la requête",
        )

        val merged = SoundchartsDraftEnricher.merge(
            current = latestEditedDraft,
            metadata = SoundchartsMetadata(listOf("Electronic", "House"), "ISRC-AUTO"),
        )

        assertEquals(latestEditedDraft, merged.draft)
        assertEquals("Electronic, House", merged.genreSuggestion)
    }

    private fun draft() = TrackDraft(
        spotifyTrackId = "5lFNqg3eMNMuJsnFRKB460",
        title = "Open Eye Signal",
        artist = "Jon Hopkins",
        spotifyUrl = "https://open.spotify.com/track/5lFNqg3eMNMuJsnFRKB460",
    )
}
