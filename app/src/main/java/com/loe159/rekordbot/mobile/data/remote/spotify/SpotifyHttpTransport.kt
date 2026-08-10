package com.loe159.rekordbot.mobile.data.remote.spotify

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SpotifyHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
) {
    override fun toString(): String = "SpotifyHttpRequest(method=$method, url=$url, body=[REDACTED])"
}

class SpotifyHttpResponse(
    val statusCode: Int,
    val body: String,
) {
    override fun toString(): String = "SpotifyHttpResponse(statusCode=$statusCode, body=[REDACTED])"
}

fun interface SpotifyHttpTransport {
    suspend fun execute(request: SpotifyHttpRequest): SpotifyHttpResponse
}

class HttpUrlConnectionSpotifyTransport : SpotifyHttpTransport {
    override suspend fun execute(request: SpotifyHttpRequest): SpotifyHttpResponse =
        withContext(Dispatchers.IO) {
            val connection = URI(request.url).toURL().openConnection() as HttpURLConnection
            try {
                connection.requestMethod = request.method
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                connection.instanceFollowRedirects = false
                request.headers.forEach(connection::setRequestProperty)
                request.body?.let { body ->
                    connection.doOutput = true
                    connection.outputStream.use { output ->
                        output.write(body.toByteArray(StandardCharsets.UTF_8))
                    }
                }
                val statusCode = connection.responseCode
                val responseBody = (
                    if (statusCode in 200..299) connection.inputStream else connection.errorStream
                    )
                    ?.bufferedReader(StandardCharsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()
                SpotifyHttpResponse(statusCode, responseBody)
            } catch (exception: IOException) {
                throw SpotifyApiException("Connexion à Spotify impossible.", cause = exception)
            } finally {
                connection.disconnect()
            }
        }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000
    }
}
