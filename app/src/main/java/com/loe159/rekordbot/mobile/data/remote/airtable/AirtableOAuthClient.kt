package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.airtable.AirtableAuthorizationSession
import com.loe159.rekordbot.mobile.domain.airtable.AirtableOAuthConfiguration
import com.loe159.rekordbot.mobile.domain.airtable.AirtableScopes
import com.loe159.rekordbot.mobile.domain.airtable.AirtableSecret
import com.loe159.rekordbot.mobile.domain.airtable.AirtableTokenSet
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONObject

class AirtableOAuthClient(
    private val configuration: AirtableOAuthConfiguration,
    private val transport: AirtableOAuthTransport = HttpUrlConnectionAirtableOAuthTransport(),
    private val pkceGenerator: AirtablePkceGenerator = AirtablePkceGenerator(),
    private val tokenEndpoint: String = DEFAULT_TOKEN_ENDPOINT,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1_000 },
) {
    fun createAuthorizationSession(): AirtableAuthorizationSession =
        pkceGenerator.createAuthorizationSession(configuration)

    suspend fun exchangeAuthorizationCode(
        code: String,
        returnedState: String?,
        session: AirtableAuthorizationSession,
    ): Result<AirtableTokenSet> = runCatching {
        require(code.isNotBlank()) { "Le code d’autorisation Airtable est absent." }
        require(pkceGenerator.isExpectedState(session.state, returnedState)) {
            "La réponse Airtable ne correspond pas à la connexion initiée."
        }
        requestTokens(
            form = linkedMapOf(
                "grant_type" to "authorization_code",
                "code" to code,
                "redirect_uri" to configuration.redirectUri.trim(),
                "client_id" to configuration.clientId.trim(),
                "code_verifier" to session.codeVerifier.reveal(),
            ),
            previousRefreshToken = null,
            previousScopes = AirtableScopes.REKORDBOT,
        ).getOrThrow()
    }

    suspend fun refreshTokens(currentTokens: AirtableTokenSet): Result<AirtableTokenSet> =
        requestTokens(
            form = linkedMapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to currentTokens.refreshToken.reveal(),
                "client_id" to configuration.clientId.trim(),
            ),
            previousRefreshToken = currentTokens.refreshToken,
            previousScopes = currentTokens.scopes,
        )

    private suspend fun requestTokens(
        form: Map<String, String>,
        previousRefreshToken: AirtableSecret?,
        previousScopes: Set<String>,
    ): Result<AirtableTokenSet> = runCatching {
        require(configuration.clientId.isNotBlank()) { "Le Client ID Airtable est obligatoire." }
        val response = transport.execute(
            AirtableOAuthHttpRequest(
                method = "POST",
                url = tokenEndpoint,
                headers = mapOf(
                    "Accept" to "application/json",
                    "Content-Type" to "application/x-www-form-urlencoded",
                ),
                body = form.entries.joinToString("&") { (key, value) ->
                    "${key.formEncode()}=${value.formEncode()}"
                },
            ),
        )
        if (response.statusCode !in 200..299) {
            throw AirtableApiException(oauthError(response.statusCode), response.statusCode)
        }
        AirtableTokenResponseParser.parse(
            body = response.body,
            previousRefreshToken = previousRefreshToken,
            previousScopes = previousScopes,
            nowEpochSeconds = nowEpochSeconds(),
        )
    }

    private fun oauthError(statusCode: Int): String = when (statusCode) {
        400 -> "La réponse de connexion Airtable est invalide ou expirée."
        401 -> "L’intégration Airtable n’est pas autorisée."
        429 -> "Limite Airtable atteinte. Réessaie plus tard."
        else -> "Connexion Airtable impossible (HTTP $statusCode)."
    }

    private fun String.formEncode(): String =
        URLEncoder.encode(this, StandardCharsets.UTF_8.toString())

    companion object {
        const val DEFAULT_TOKEN_ENDPOINT = "https://airtable.com/oauth2/v1/token"
    }
}

internal object AirtableTokenResponseParser {
    fun parse(
        body: String,
        previousRefreshToken: AirtableSecret?,
        previousScopes: Set<String>,
        nowEpochSeconds: Long,
    ): AirtableTokenSet {
        val json = JSONObject(body)
        val accessToken = json.optString("access_token").trim()
        val expiresIn = json.optLong("expires_in", 0)
        val refreshToken = json.optString("refresh_token").trim()
            .takeIf(String::isNotBlank)
            ?.let(AirtableSecret::from)
            ?: previousRefreshToken
        if (accessToken.isBlank() || expiresIn <= 0 || refreshToken == null) {
            throw AirtableApiException("Réponse d’authentification Airtable incomplète.")
        }
        val returnedScopes = json.optString("scope")
            .split(' ')
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
        return AirtableTokenSet(
            accessToken = AirtableSecret.from(accessToken),
            refreshToken = refreshToken,
            expiresAtEpochSeconds = nowEpochSeconds + expiresIn,
            scopes = returnedScopes.ifEmpty { previousScopes },
            tokenType = json.optString("token_type", "Bearer").ifBlank { "Bearer" },
        )
    }
}
