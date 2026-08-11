package com.loe159.rekordbot.mobile.data.remote.airtable

import java.net.HttpURLConnection
import java.net.URI
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class AirtableOAuthHttpRequest(
    val method: String,
    val url: String,
    val headers: Map<String, String> = emptyMap(),
    val body: String? = null,
)

data class AirtableOAuthHttpResponse(
    val statusCode: Int,
    val body: String,
)

fun interface AirtableOAuthTransport {
    suspend fun execute(request: AirtableOAuthHttpRequest): AirtableOAuthHttpResponse
}

class HttpUrlConnectionAirtableOAuthTransport : AirtableOAuthTransport {
    override suspend fun execute(request: AirtableOAuthHttpRequest): AirtableOAuthHttpResponse =
        withContext(Dispatchers.IO) {
            val connection = URI(request.url).toURL().openConnection() as HttpURLConnection
            try {
                connection.requestMethod = request.method
                connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
                connection.readTimeout = READ_TIMEOUT_MILLIS
                request.headers.forEach(connection::setRequestProperty)
                request.body?.let { body ->
                    connection.doOutput = true
                    connection.outputStream.bufferedWriter(StandardCharsets.UTF_8).use {
                        it.write(body)
                    }
                }
                val statusCode = connection.responseCode
                val responseBody = (
                    if (statusCode in 200..299) connection.inputStream else connection.errorStream
                )?.bufferedReader(StandardCharsets.UTF_8)?.use { it.readText() }.orEmpty()
                AirtableOAuthHttpResponse(statusCode, responseBody)
            } finally {
                connection.disconnect()
            }
        }

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 20_000
    }
}
