package com.loe159.rekordbot.mobile.data.remote.soundcharts

data class SoundchartsRequest(
    val url: String,
    val headers: Map<String, String>,
)

data class SoundchartsResponse(
    val statusCode: Int,
    val body: String,
)

fun interface SoundchartsTransport {
    suspend fun execute(request: SoundchartsRequest): SoundchartsResponse
}
