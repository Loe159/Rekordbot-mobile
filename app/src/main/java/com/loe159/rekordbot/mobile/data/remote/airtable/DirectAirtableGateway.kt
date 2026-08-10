package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.model.AirtableConfigurationValidator
import com.loe159.rekordbot.mobile.domain.model.AirtableFieldSchema
import com.loe159.rekordbot.mobile.domain.model.AirtableTableSchema
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URI
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DirectAirtableGateway(
    private val apiBaseUrl: String = "https://api.airtable.com/v0",
) : AirtableGateway {
    override suspend fun fetchTableSchema(
        configuration: AirtableConfiguration,
    ): Result<AirtableTableSchema> = runCatching {
        val response = request(
            method = "GET",
            url = "$apiBaseUrl/meta/bases/${encodePathSegment(configuration.baseId.trim())}/tables",
            token = configuration.personalAccessToken,
        )
        parseTableSchema(response, configuration.table)
    }

    override suspend fun createDemoRecord(
        configuration: AirtableConfiguration,
    ): Result<String> {
        val schema = fetchTableSchema(configuration).getOrElse { return Result.failure(it) }
        val schemaErrors = AirtableConfigurationValidator.schemaErrors(configuration, schema)
        if (schemaErrors.isNotEmpty()) {
            return Result.failure(AirtableApiException(schemaErrors.joinToString("\n")))
        }

        return runCatching {
            val fields = JSONObject().apply {
                put(configuration.fields.title, "Test Rekordbot Mobile")
                put(configuration.fields.artist, "Connexion Airtable")
                put(configuration.fields.spotifyUrl, "https://open.spotify.com/")
                put(
                    configuration.fields.spotifyTrackId,
                    "rekordbot-test-${System.currentTimeMillis()}",
                )
                put(configuration.fields.status, configuration.defaultStatus)
                if (configuration.fields.source.isNotBlank() && configuration.defaultSource.isNotBlank()) {
                    put(configuration.fields.source, configuration.defaultSource)
                }
            }
            val body = JSONObject()
                .put("records", org.json.JSONArray().put(JSONObject().put("fields", fields)))
                .toString()
            val response = request(
                method = "POST",
                url = "$apiBaseUrl/${encodePathSegment(configuration.baseId.trim())}/${encodePathSegment(schema.id)}",
                token = configuration.personalAccessToken,
                body = body,
            )
            JSONObject(response).getJSONArray("records").getJSONObject(0).getString("id")
        }
    }

    override suspend fun findRecordBySpotifyTrackId(
        configuration: AirtableConfiguration,
        spotifyTrackId: String,
    ): Result<String?> = runCatching {
        val formula = AirtableFormula.textEquals(
            configuration.fields.spotifyTrackId.trim(),
            spotifyTrackId.trim(),
        )
        val response = request(
            method = "GET",
            url = buildString {
                append(apiBaseUrl)
                append('/')
                append(encodePathSegment(configuration.baseId.trim()))
                append('/')
                append(encodePathSegment(configuration.table.trim()))
                append("?maxRecords=1&filterByFormula=")
                append(encodeQueryParameter(formula))
            },
            token = configuration.personalAccessToken,
        )
        val records = JSONObject(response).getJSONArray("records")
        if (records.length() == 0) null else records.getJSONObject(0).getString("id")
    }

    override suspend fun createTrackRecord(
        configuration: AirtableConfiguration,
        track: TrackDraft,
    ): Result<String> = runCatching {
        val fields = JSONObject(AirtableRecordMapper.fields(configuration, track))
        val body = JSONObject()
            .put("records", org.json.JSONArray().put(JSONObject().put("fields", fields)))
            .toString()
        val response = request(
            method = "POST",
            url = "$apiBaseUrl/${encodePathSegment(configuration.baseId.trim())}/${encodePathSegment(configuration.table.trim())}",
            token = configuration.personalAccessToken,
            body = body,
        )
        JSONObject(response).getJSONArray("records").getJSONObject(0).getString("id")
    }

    private suspend fun request(
        method: String,
        url: String,
        token: String,
        body: String? = null,
    ): String = withContext(Dispatchers.IO) {
        val connection = URI(url).toURL().openConnection() as HttpURLConnection
        try {
            connection.requestMethod = method
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.setRequestProperty("Authorization", "Bearer ${token.trim()}")
            connection.setRequestProperty("Accept", "application/json")
            if (body != null) {
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { it.write(body) }
            }

            val statusCode = connection.responseCode
            val responseBody = (if (statusCode in 200..299) connection.inputStream else connection.errorStream)
                ?.bufferedReader(StandardCharsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            if (statusCode !in 200..299) {
                throw AirtableApiException(
                    message = readableError(statusCode, responseBody),
                    statusCode = statusCode,
                )
            }
            responseBody
        } catch (exception: AirtableApiException) {
            throw exception
        } catch (exception: IOException) {
            throw AirtableApiException(
                "Connexion à Airtable impossible. Vérifie le réseau puis réessaie.",
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun parseTableSchema(response: String, configuredTable: String): AirtableTableSchema {
        val tables = JSONObject(response).getJSONArray("tables")
        for (index in 0 until tables.length()) {
            val table = tables.getJSONObject(index)
            val matches = table.getString("id") == configuredTable.trim() ||
                table.getString("name").equals(configuredTable.trim(), ignoreCase = true)
            if (!matches) continue

            val fieldsJson = table.getJSONArray("fields")
            val fields = buildList {
                for (fieldIndex in 0 until fieldsJson.length()) {
                    val field = fieldsJson.getJSONObject(fieldIndex)
                    add(
                        AirtableFieldSchema(
                            id = field.getString("id"),
                            name = field.getString("name"),
                            type = field.getString("type"),
                        ),
                    )
                }
            }
            return AirtableTableSchema(
                id = table.getString("id"),
                name = table.getString("name"),
                fields = fields,
            )
        }
        throw AirtableApiException("Table « $configuredTable » introuvable dans cette base.")
    }

    private fun readableError(statusCode: Int, responseBody: String): String {
        val airtableDetail = runCatching {
            val error = JSONObject(responseBody).opt("error")
            when (error) {
                is JSONObject -> error.optString("message").ifBlank { error.optString("type") }
                is String -> error
                else -> ""
            }
        }.getOrDefault("")

        return when (statusCode) {
            HttpURLConnection.HTTP_UNAUTHORIZED ->
                "Token Airtable invalide ou révoqué."
            HttpURLConnection.HTTP_FORBIDDEN ->
                "Accès refusé. Vérifie l’accès du token à la base et les droits schema.bases:read / data.records:read / data.records:write."
            HttpURLConnection.HTTP_NOT_FOUND ->
                "Base Airtable introuvable ou inaccessible. Vérifie le Base ID et les ressources du token."
            422 -> airtableDetail.ifBlank {
                "Airtable refuse la configuration. Vérifie les noms et les types des champs."
            }
            429 -> "Trop de requêtes envoyées à Airtable. Réessaie dans quelques secondes."
            else -> airtableDetail.ifBlank { "Erreur Airtable HTTP $statusCode." }
        }
    }

    private fun encodePathSegment(value: String): String = URLEncoder
        .encode(value, StandardCharsets.UTF_8.toString())
        .replace("+", "%20")

    private fun encodeQueryParameter(value: String): String = URLEncoder
        .encode(value, StandardCharsets.UTF_8.toString())
        .replace("+", "%20")

    private companion object {
        const val CONNECT_TIMEOUT_MILLIS = 15_000
        const val READ_TIMEOUT_MILLIS = 20_000
    }
}
