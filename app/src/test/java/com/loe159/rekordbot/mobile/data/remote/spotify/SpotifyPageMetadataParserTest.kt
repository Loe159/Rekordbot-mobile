package com.loe159.rekordbot.mobile.data.remote.spotify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyPageMetadataParserTest {
    @Test
    fun `parses title and artist from Spotify open graph metadata`() {
        val html = """
            <html>
              <head>
                <title>Open Eye Signal - under the fabric - song and lyrics by Jon Hopkins | Spotify</title>
                <meta property="og:title" content="Open Eye Signal - under the fabric" />
                <meta property="og:description" content="Listen on Spotify. Song · Jon Hopkins · 2026." />
              </head>
            </html>
        """.trimIndent()

        val metadata = SpotifyPageMetadataParser.parse(html)

        assertEquals("Open Eye Signal - under the fabric", metadata?.title)
        assertEquals("Jon Hopkins", metadata?.artist)
    }

    @Test
    fun `falls back to the document title and decodes entities`() {
        val html = """
            <title>Odd &amp; Even - song and lyrics by A &amp; B | Spotify</title>
        """.trimIndent()

        val metadata = SpotifyPageMetadataParser.parse(html)

        assertEquals("Odd & Even", metadata?.title)
        assertEquals("A & B", metadata?.artist)
    }

    @Test
    fun `returns null when artist is absent`() {
        val html = "<meta property=\"og:title\" content=\"Open Eye Signal\" />"

        assertNull(SpotifyPageMetadataParser.parse(html))
    }
}
