package com.loe159.rekordbot.mobile.data.remote.soundcharts

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HttpUrlConnectionSoundchartsTransport : SoundchartsTransport {
    override suspend fun execute(request: SoundchartsRequest): SoundchartsResponse =
        withContext(Dispatchers.IO) {
            val connection = URI(request.url).toURL().openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "GET"
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                request.headers.forEach(connection::setRequestProperty)
                val status = connection.responseCode
                val body = (if (status in 200..299) connection.inputStream else connection.errorStream)
                    ?.bufferedReader(StandardCharsets.UTF_8)
                    ?.use { it.readText() }
                    .orEmpty()
                SoundchartsResponse(status, body)
            } catch (exception: IOException) {
                throw SoundchartsApiException("Connexion à Soundcharts impossible.", cause = exception)
            } finally {
                connection.disconnect()
            }
        }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 10_000
        const val READ_TIMEOUT_MILLIS = 15_000
    }
}
