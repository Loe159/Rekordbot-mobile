package com.loe159.rekordbot.mobile.data.remote.soundcharts

import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsAccessMode
import com.loe159.rekordbot.mobile.domain.soundcharts.SoundchartsConfiguration
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackendSoundchartsGatewayTest {
    @Test
    fun `managed service sends no Soundcharts credential from the app`() = runBlocking {
        var captured: SoundchartsRequest? = null
        val gateway = BackendSoundchartsGateway(
            publicApiBaseUrl = "https://api.rekordbot.test",
            transport = SoundchartsTransport { request ->
                captured = request
                SoundchartsResponse(
                    200,
                    """{"object":{"isrc":{"value":"USSM19902990"},"genres":[{"root":"Pop"}]}}""",
                )
            },
        )

        val result = gateway.fetchTrackMetadata(
            "4uLU6hMCjMI75M1A2tKUQC",
            SoundchartsConfiguration(
                enabled = true,
                accessMode = SoundchartsAccessMode.MANAGED_SERVICE,
            ),
        )

        assertTrue(result.isSuccess)
        assertEquals("USSM19902990", result.getOrThrow().isrc)
        assertEquals(setOf("Accept"), captured?.headers?.keys)
        assertEquals(
            "https://api.rekordbot.test/v1/soundcharts/tracks/spotify/4uLU6hMCjMI75M1A2tKUQC",
            captured?.url,
        )
    }
}
