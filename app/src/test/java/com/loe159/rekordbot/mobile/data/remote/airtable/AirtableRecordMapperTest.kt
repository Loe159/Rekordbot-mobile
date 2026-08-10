package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.AirtableFieldMappings
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AirtableRecordMapperTest {
    @Test
    fun `default contract matches Sons field names`() {
        val configuration = AirtableConfiguration()

        assertEquals("ISRC", configuration.fields.isrc)
        assertEquals("Genre brut", configuration.fields.rawGenre)
        assertEquals("Commentaire", configuration.fields.comment)
        assertEquals("État RekordBot", configuration.fields.rekordbotState)
        assertEquals("Erreur RekordBot", configuration.fields.rekordbotError)
        assertEquals("Dernière synchro", configuration.fields.lastSync)
        assertEquals("Méthode de matching", configuration.fields.matchingMethod)
        assertEquals("À traiter", configuration.defaultRekordbotState)
    }

    @Test
    fun `configured fields map required and available optional values`() {
        val configuration = AirtableConfiguration(
            fields = AirtableFieldMappings(
                title = "Name",
                artist = "Artists",
                spotifyUrl = "URL",
                spotifyTrackId = "Spotify ID",
                isrc = "ISRC code",
                status = "State",
                rekordbotState = "Bot state",
                rekordbotError = "Bot error",
                lastSync = "Bot date",
                matchingMethod = "Bot match",
                rawGenre = "Raw genre",
                energy = "Rating",
                mood = "Moods",
                situation = "Contexts",
                inspirationalDjs = "Inspired by",
                comment = "Notes",
                source = "Origin",
            ),
            defaultStatus = "Inbox",
            defaultSource = "Spotify Android",
            defaultRekordbotState = "À traiter",
        )
        val track = TrackDraft(
            spotifyTrackId = " track-id ",
            title = " Open Eye Signal ",
            artist = " Jon Hopkins ",
            spotifyUrl = " https://open.spotify.com/track/track-id ",
            isrc = " FR-ABC-12-34567 ",
            rawGenre = "Electronic",
            energy = 4,
            moods = listOf("Calme", "Mystérieux"),
            situations = listOf("Warm-up", "Closing"),
            inspirationalDjs = listOf("ANOTR", "Adam Ten"),
            comment = null,
        )

        val fields = AirtableRecordMapper.fields(configuration, track)

        assertEquals("Open Eye Signal", fields["Name"])
        assertEquals("Jon Hopkins", fields["Artists"])
        assertEquals("track-id", fields["Spotify ID"])
        assertEquals("FR-ABC-12-34567", fields["ISRC code"])
        assertEquals("Inbox", fields["State"])
        assertEquals("À traiter", fields["Bot state"])
        assertEquals("Spotify Android", fields["Origin"])
        assertEquals("Electronic", fields["Raw genre"])
        assertEquals(4, fields["Rating"])
        assertEquals(listOf("Calme", "Mystérieux"), fields["Moods"])
        assertEquals(listOf("Warm-up", "Closing"), fields["Contexts"])
        assertEquals(listOf("ANOTR", "Adam Ten"), fields["Inspired by"])
        assertFalse(fields["Rating"] is String)
        assertFalse(fields["Moods"] is String)
        assertFalse(fields.containsKey("Notes"))
        assertFalse(fields.containsKey("Bot error"))
        assertFalse(fields.containsKey("Bot date"))
        assertFalse(fields.containsKey("Bot match"))
    }

    @Test
    fun `formula escapes configured field and Spotify id`() {
        assertEquals(
            "{Spotify \\} ID}='abc\\'def\\\\ghi'",
            AirtableFormula.textEquals("Spotify } ID", "abc'def\\ghi"),
        )
    }
}
