package com.loe159.rekordbot.mobile.domain.shazam

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class ShazamPlaylistNormalizationTest {
    @Test
    fun `duplicate Spotify IDs keep the most recent playlist occurrence`() {
        val older = track(id = "same-id", addedAt = 100L, position = 8, title = "Old title")
        val newer = track(id = "same-id", addedAt = 200L, position = 2, title = "New title")

        val normalized = listOf(older, newer).distinctForInbox()

        assertEquals(1, normalized.size)
        assertEquals("New title", normalized.single().title)
        assertEquals(2, normalized.single().playlistPosition)
    }

    @Test
    fun `tracks are ordered from most recent with playlist position as fallback`() {
        val withoutDate = track(id = "no-date", addedAt = null, position = 0)
        val older = track(id = "older", addedAt = 100L, position = 9)
        val newerSecond = track(id = "newer-second", addedAt = 200L, position = 2)
        val newerFirst = track(id = "newer-first", addedAt = 200L, position = 1)

        val ids = listOf(withoutDate, older, newerSecond, newerFirst)
            .distinctForInbox()
            .map(ShazamPlaylistTrack::spotifyTrackId)

        assertEquals(listOf("newer-first", "newer-second", "older", "no-date"), ids)
    }

    @Test
    fun `blank Spotify ID is rejected before persistence`() {
        assertThrows(IllegalArgumentException::class.java) {
            listOf(track(id = "  ", addedAt = null, position = 0)).distinctForInbox()
        }
    }

    private fun track(
        id: String,
        addedAt: Long?,
        position: Int,
        title: String = id,
    ) = ShazamPlaylistTrack(
        spotifyTrackId = id,
        title = title,
        artist = "Artist",
        spotifyUrl = "https://open.spotify.com/track/$id",
        playlistAddedAtEpochMillis = addedAt,
        playlistPosition = position,
    )
}
