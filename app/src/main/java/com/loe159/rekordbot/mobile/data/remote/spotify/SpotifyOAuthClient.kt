package com.loe159.rekordbot.mobile.data.remote.spotify

import com.loe159.rekordbot.mobile.domain.spotify.SpotifyAuthorizationSession
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyConfiguration
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyScopes
import com.loe159.rekordbot.mobile.domain.spotify.SpotifySecret
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyTokenSet
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import org.json.JSONObject

class SpotifyOAuthClient(
    private val configuration: SpotifyConfiguration,
    private val transport: SpotifyHttpTransport = HttpUrlConnectionSpotifyTransport(),
    private val pkceGenerator: SpotifyPkceGenerator = SpotifyPkceGenerator(),
    private val tokenEndpoint: String = DEFAULT_TOKEN_ENDPOINT,
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1_000 },
) {
    fun createAuthorizationSession(showDialog: Boolean = false): SpotifyAuthorizationSession =
        pkceGenerator.createAuthorizationSession(
            configuration = configuration,
            scopes = SpotifyScopes.SHAZAM_PLAYLIST_READ,
            showDialog = showDialog,
        )

    suspend fun exchangeAuthorizationCode(
        code: String,
        returnedState: String?,
        session: SpotifyAuthorizationSession,
    ): Result<SpotifyTokenSet> = runCatching {
        require(code.isNotBlank()) { "Le code d’autorisation Spotify est absent." }
        require(pkceGenerator.isExpectedState(session.state, returnedState)) {
            "La réponse Spotify ne correspond pas à la connexion initiée."
        }
        exchangeCode(code, session.codeVerifier).getOrThrow()
    }

    suspend fun exchangeCode(
        code: String,
        codeVerifier: SpotifySecret,
    ): Result<SpotifyTokenSet> = requestTokens(
        form = linkedMapOf(
            "grant_type" to "authorization_code",
            "code" to code,
            "redirect_uri" to configuration.redirectUri.trim(),
            "client_id" to configuration.clientId.trim(),
            "code_verifier" to codeVerifier.reveal(),
        ),
        previousRefreshToken = null,
        previousScopes = SpotifyScopes.SHAZAM_PLAYLIST_READ,
    )

    suspend fun refreshTokens(currentTokens: SpotifyTokenSet): Result<SpotifyTokenSet> {
        val refreshToken = currentTokens.refreshToken
            ?: return Result.failure(IllegalStateException("Aucun refresh token Spotify disponible."))
        return requestTokens(
            form = linkedMapOf(
                "grant_type" to "refresh_token",
                "refresh_token" to refreshToken.reveal(),
                "client_id" to configuration.clientId.trim(),
            ),
            previousRefreshToken = refreshToken,
            previousScopes = currentTokens.scopes,
        )
    }

    private suspend fun requestTokens(
        form: Map<String, String>,
        previousRefreshToken: SpotifySecret?,
        previousScopes: Set<String>,
    ): Result<SpotifyTokenSet> = runCatching {
        require(configuration.clientId.isNotBlank()) { "Le Client ID Spotify est obligatoire." }
        val response = transport.execute(
            SpotifyHttpRequest(
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
            throw SpotifyApiException(oauthError(response.statusCode), response.statusCode)
        }
        SpotifyTokenResponseParser.parse(
            body = response.body,
            previousRefreshToken = previousRefreshToken,
            previousScopes = previousScopes,
            nowEpochSeconds = nowEpochSeconds(),
        )
    }

    private fun oauthError(statusCode: Int): String = when (statusCode) {
        400 -> "La réponse de connexion Spotify est invalide ou expirée."
        401 -> "L’application Spotify n’est pas autorisée."
        429 -> "Limite Spotify atteinte. Réessaie plus tard."
        else -> "Connexion Spotify impossible (HTTP $statusCode)."
    }

    private fun String.formEncode(): String = URLEncoder.encode(this, StandardCharsets.UTF_8)

    companion object {
        const val DEFAULT_TOKEN_ENDPOINT = "https://accounts.spotify.com/api/token"
    }
}

internal object SpotifyTokenResponseParser {
    fun parse(
        body: String,
        previousRefreshToken: SpotifySecret?,
        previousScopes: Set<String>,
        nowEpochSeconds: Long,
    ): SpotifyTokenSet {
        val json = JSONObject(body)
        val accessToken = json.optString("access_token").trim()
        val expiresIn = json.optLong("expires_in", 0)
        if (accessToken.isBlank() || expiresIn <= 0) {
            throw SpotifyApiException("Réponse d’authentification Spotify incomplète.")
        }
        val returnedRefreshToken = json.optString("refresh_token").trim()
            .takeIf(String::isNotBlank)
            ?.let(SpotifySecret::from)
        val returnedScopes = json.optString("scope")
            .split(' ')
            .map(String::trim)
            .filter(String::isNotBlank)
            .toSet()
        return SpotifyTokenSet(
            accessToken = SpotifySecret.from(accessToken),
            refreshToken = returnedRefreshToken ?: previousRefreshToken,
            expiresAtEpochSeconds = nowEpochSeconds + expiresIn,
            scopes = returnedScopes.ifEmpty { previousScopes },
            tokenType = json.optString("token_type", "Bearer").ifBlank { "Bearer" },
        )
    }
}
