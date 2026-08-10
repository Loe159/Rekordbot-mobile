package com.loe159.rekordbot.mobile.data.local.shazam

import com.loe159.rekordbot.mobile.domain.shazam.ShazamDecision
import com.loe159.rekordbot.mobile.domain.shazam.ShazamPlaylistTrack
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ShazamInboxEntityMapperTest {
    @Test
    fun `new synchronized track starts pending and keeps source metadata`() {
        val source = ShazamPlaylistTrack(
            spotifyTrackId = "spotify-id",
            title = "Open Eye Signal",
            artist = "Jon Hopkins",
            spotifyUrl = "https://open.spotify.com/track/spotify-id",
            albumName = "Immunity",
            artworkUrl = "https://image.example/cover.jpg",
            isrc = "GB-CEL-13-00001",
            playlistAddedAtEpochMillis = 123L,
            playlistPosition = 4,
        )

        val domain = source.toEntity(now = 456L).toDomain()

        assertEquals(source.spotifyTrackId, domain.spotifyTrackId)
        assertEquals(source.title, domain.title)
        assertEquals(source.albumName, domain.albumName)
        assertEquals(source.isrc, domain.isrc)
        assertEquals(ShazamDecision.PENDING, domain.decision)
        assertEquals(456L, domain.firstSeenAt)
        assertEquals(456L, domain.lastSeenAt)
        assertNull(domain.decisionUpdatedAt)
    }
}
