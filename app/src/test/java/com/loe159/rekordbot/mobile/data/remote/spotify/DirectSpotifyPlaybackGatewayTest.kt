package com.loe159.rekordbot.mobile.data.remote.spotify

import com.loe159.rekordbot.mobile.domain.spotify.SpotifySecret
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectSpotifyPlaybackGatewayTest {
    @Test
    fun `starts selected track on active Spotify device`() = runBlocking {
        var captured: SpotifyHttpRequest? = null
        val gateway = DirectSpotifyPlaybackGateway(
            transport = SpotifyHttpTransport { request ->
                captured = request
                SpotifyHttpResponse(204, "")
            },
        )

        gateway.playTrack(
            SpotifySecret.from("private-token"),
            "5lFNqg3eMNMuJsnFRKB460",
        ).getOrThrow()

        val request = requireNotNull(captured)
        assertEquals("PUT", request.method)
        assertEquals("https://api.spotify.com/v1/me/player/play", request.url)
        assertEquals("Bearer private-token", request.headers["Authorization"])
        val body = JSONObject(requireNotNull(request.body))
        assertEquals("spotify:track:5lFNqg3eMNMuJsnFRKB460", body.getJSONArray("uris").getString(0))
        assertEquals(0, body.getInt("position_ms"))
        assertFalse(request.toString().contains("private-token"))
    }

    @Test
    fun `pauses active Spotify device without a body`() = runBlocking {
        var captured: SpotifyHttpRequest? = null
        val gateway = DirectSpotifyPlaybackGateway(
            transport = SpotifyHttpTransport { request ->
                captured = request
                SpotifyHttpResponse(204, "")
            },
        )

        gateway.pause(SpotifySecret.from("access")).getOrThrow()

        val request = requireNotNull(captured)
        assertEquals("PUT", request.method)
        assertTrue(request.url.endsWith("/me/player/pause"))
        assertNull(request.body)
    }
}
