package com.loe159.rekordbot.mobile.data.remote.spotify

import com.loe159.rekordbot.mobile.domain.spotify.SpotifySecret
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectSpotifyPlaylistGatewayTest {
    @Test
    fun `follows playlist pagination and never exposes token in request string`() = runBlocking {
        val requests = mutableListOf<SpotifyHttpRequest>()
        val gateway = DirectSpotifyPlaylistGateway(
            transport = SpotifyHttpTransport { request ->
                requests += request
                when {
                    request.url.contains("offset=50") -> SpotifyHttpResponse(
                        200,
                        """{"items":[{"id":"BBBBBBBBBBBBBBBBBBBBBB","name":"Other"}],"next":null}""",
                    )
                    else -> SpotifyHttpResponse(
                        200,
                        """{"items":[{"id":"AAAAAAAAAAAAAAAAAAAAAA","name":"Mes titres Shazam"}],"next":"https://api.spotify.com/v1/me/playlists?limit=50&offset=50"}""",
                    )
                }
            },
        )

        val playlists = gateway.listPlaylists(SpotifySecret.from("private-token")).getOrThrow()

        assertEquals(listOf("Mes titres Shazam", "Other"), playlists.map { it.name })
        assertEquals(2, requests.size)
        assertTrue(requests.all { it.headers["Authorization"] == "Bearer private-token" })
        assertTrue(requests.none { it.toString().contains("private-token") })
    }

    @Test
    fun `parses 2026 playlist item field and skips non tracks`() = runBlocking {
        val gateway = DirectSpotifyPlaylistGateway(
            transport = SpotifyHttpTransport {
                SpotifyHttpResponse(
                    200,
                    """{
                        "items": [
                          {"added_at":"2026-08-10T20:00:00Z","item":{"id":"5lFNqg3eMNMuJsnFRKB460","type":"track","name":"Open Eye Signal","artists":[{"name":"Jon Hopkins"}],"album":{"name":"Immunity","images":[{"url":"https://image"}]},"external_ids":{"isrc":"GBUM71301801"},"external_urls":{"spotify":"https://open.spotify.com/track/5lFNqg3eMNMuJsnFRKB460"}}},
                          {"item":{"id":"episode","type":"episode","name":"Podcast"}}
                        ],
                        "next": null
                    }""".trimIndent(),
                )
            },
        )

        val tracks = gateway.listPlaylistTracks(
            SpotifySecret.from("access"),
            "AAAAAAAAAAAAAAAAAAAAAA",
        ).getOrThrow()

        assertEquals(1, tracks.size)
        assertEquals("Open Eye Signal", tracks.single().title)
        assertEquals("Jon Hopkins", tracks.single().artist)
        assertEquals("GBUM71301801", tracks.single().isrc)
    }
}
