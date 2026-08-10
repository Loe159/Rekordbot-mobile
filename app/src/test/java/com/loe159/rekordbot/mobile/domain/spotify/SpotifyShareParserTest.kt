package com.loe159.rekordbot.mobile.domain.spotify

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SpotifyShareParserTest {
    private val trackId = "5lFNqg3eMNMuJsnFRKB460"

    @Test
    fun `parses a Spotify web track link and removes tracking parameters`() {
        val result = SpotifyShareParser.parse(
            "https://open.spotify.com/track/$trackId?si=123456",
        )

        assertEquals(trackId, result.draft.spotifyTrackId)
        assertEquals("https://open.spotify.com/track/$trackId", result.draft.spotifyUrl)
        assertEquals(SpotifyShareIssue.MISSING_TITLE_AND_ARTIST, result.issue)
    }

    @Test
    fun `parses a Spotify track URI`() {
        val result = SpotifyShareParser.parse("spotify:track:$trackId")

        assertEquals(trackId, result.draft.spotifyTrackId)
        assertEquals("https://open.spotify.com/track/$trackId", result.draft.spotifyUrl)
    }

    @Test
    fun `parses title and artist from subject`() {
        val result = SpotifyShareParser.parse(
            sharedText = "https://open.spotify.com/track/$trackId",
            sharedSubject = "Open Eye Signal · Jon Hopkins",
        )

        assertEquals("Open Eye Signal", result.draft.title)
        assertEquals("Jon Hopkins", result.draft.artist)
        assertNull(result.issue)
    }

    @Test
    fun `parses localized Spotify share text`() {
        val result = SpotifyShareParser.parse(
            "Écouter Open Eye Signal de Jon Hopkins sur Spotify https://open.spotify.com/track/$trackId",
        )

        assertEquals("Open Eye Signal", result.draft.title)
        assertEquals("Jon Hopkins", result.draft.artist)
        assertNull(result.issue)
    }

    @Test
    fun `rejects non-track Spotify links`() {
        val result = SpotifyShareParser.parse("https://open.spotify.com/album/123")

        assertEquals("", result.draft.spotifyTrackId)
        assertEquals(SpotifyShareIssue.NOT_A_SPOTIFY_TRACK, result.issue)
    }

    @Test
    fun `reports an empty share`() {
        val result = SpotifyShareParser.parse(" ")

        assertEquals(SpotifyShareIssue.EMPTY_SHARE, result.issue)
    }
}
