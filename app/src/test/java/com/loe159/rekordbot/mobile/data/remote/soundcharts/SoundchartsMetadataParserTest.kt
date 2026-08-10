package com.loe159.rekordbot.mobile.data.remote.soundcharts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SoundchartsMetadataParserTest {
    @Test
    fun `parses envelope roots subs in order and removes duplicates`() {
        val metadata = SoundchartsMetadataParser.parse(
            """{
                "object": {
                    "isrc": {"value": "GB-CEL-21-00001"},
                    "genres": [
                        {"root": "Electronic", "sub": ["House", "Afro House"]},
                        {"root": "electronic", "sub": ["Deep House", "House"]}
                    ]
                }
            }""".trimIndent(),
        )

        assertEquals(
            listOf("Electronic", "House", "Afro House", "Deep House"),
            metadata.genres,
        )
        assertEquals("Electronic, House, Afro House, Deep House", metadata.rawGenre)
        assertEquals("GB-CEL-21-00001", metadata.isrc)
    }

    @Test
    fun `accepts flexible string object and array genre formats`() {
        val metadata = SoundchartsMetadataParser.parse(
            """{
                "object": {
                    "isrc": "US-ABC-24-00002",
                    "genres": [
                        {"root": {"name": "Dance"}, "sub": "Melodic House"},
                        {"root": ["Electronic"], "sub": [{"value": "Tech House"}]}
                    ]
                }
            }""".trimIndent(),
        )

        assertEquals(listOf("Dance", "Melodic House", "Electronic", "Tech House"), metadata.genres)
        assertEquals("US-ABC-24-00002", metadata.isrc)
    }

    @Test
    fun `missing optional metadata stays empty`() {
        val metadata = SoundchartsMetadataParser.parse("""{"object": {}}""")

        assertEquals(emptyList<String>(), metadata.genres)
        assertNull(metadata.rawGenre)
        assertNull(metadata.isrc)
    }
}
