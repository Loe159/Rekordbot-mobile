package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.AirtableFieldMappings
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class AirtableRecordMapperTest {
    @Test
    fun `configured fields map required and available optional values`() {
        val configuration = AirtableConfiguration(
            fields = AirtableFieldMappings(
                title = "Name",
                artist = "Artists",
                spotifyUrl = "URL",
                spotifyTrackId = "Spotify ID",
                status = "State",
                rawGenre = "Raw genre",
                comment = "Notes",
                source = "Origin",
            ),
            defaultStatus = "Inbox",
            defaultSource = "Spotify Android",
        )
        val track = TrackDraft(
            spotifyTrackId = " track-id ",
            title = " Open Eye Signal ",
            artist = " Jon Hopkins ",
            spotifyUrl = " https://open.spotify.com/track/track-id ",
            rawGenre = "Electronic",
            comment = null,
        )

        val fields = AirtableRecordMapper.fields(configuration, track)

        assertEquals("Open Eye Signal", fields["Name"])
        assertEquals("Jon Hopkins", fields["Artists"])
        assertEquals("track-id", fields["Spotify ID"])
        assertEquals("Inbox", fields["State"])
        assertEquals("Spotify Android", fields["Origin"])
        assertEquals("Electronic", fields["Raw genre"])
        assertFalse(fields.containsKey("Notes"))
    }

    @Test
    fun `formula escapes configured field and Spotify id`() {
        assertEquals(
            "{Spotify \\} ID}='abc\\'def\\\\ghi'",
            AirtableFormula.textEquals("Spotify } ID", "abc'def\\ghi"),
        )
    }
}
