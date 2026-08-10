package com.loe159.rekordbot.mobile.data.remote.soundcharts

import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DirectSoundchartsGatewayTest {
    private val configuration = SoundchartsConfiguration(
        enabled = true,
        appId = " app-id ",
        apiKey = " api-key ",
    )

    @Test
    fun `uses official Spotify endpoint and legacy headers`() = runBlocking {
        var captured: SoundchartsRequest? = null
        val gateway = DirectSoundchartsGateway(
            transport = SoundchartsTransport { request ->
                captured = request
                SoundchartsResponse(200, """{"object":{"genres":[]}}""")
            },
        )

        gateway.fetchTrackMetadata(TRACK_ID, configuration).getOrThrow()

        assertEquals(
            "https://customer.api.soundcharts.com/api/v2.25/song/by-platform/spotify/$TRACK_ID",
            captured?.url,
        )
        assertEquals("app-id", captured?.headers?.get("x-app-id"))
        assertEquals("api-key", captured?.headers?.get("x-api-key"))
        assertEquals("application/json", captured?.headers?.get("Accept"))
    }

    @Test
    fun `authentication lookup and rate errors are returned as non throwing failures`() = runBlocking {
        listOf(401, 403, 404, 429).forEach { status ->
            val gateway = DirectSoundchartsGateway(
                transport = SoundchartsTransport { SoundchartsResponse(status, "{}") },
            )

            val result = gateway.fetchTrackMetadata(TRACK_ID, configuration)

            assertTrue("HTTP $status must stay in Result.failure", result.isFailure)
            assertEquals(status, (result.exceptionOrNull() as SoundchartsApiException).statusCode)
        }
    }

    private companion object {
        const val TRACK_ID = "5lFNqg3eMNMuJsnFRKB460"
    }
}
