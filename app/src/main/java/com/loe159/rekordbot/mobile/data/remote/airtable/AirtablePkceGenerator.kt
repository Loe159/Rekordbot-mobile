package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.airtable.AirtableAuthorizationSession
import com.loe159.rekordbot.mobile.domain.airtable.AirtableOAuthConfiguration
import com.loe159.rekordbot.mobile.domain.airtable.AirtableScopes
import com.loe159.rekordbot.mobile.domain.airtable.AirtableSecret
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

class AirtablePkceGenerator private constructor(
    private val randomBytes: (Int) -> ByteArray,
) {
    constructor() : this(::secureRandomBytes)

    internal constructor(bytesProvider: AirtableRandomBytesProvider) : this(bytesProvider::nextBytes)

    fun createAuthorizationSession(
        configuration: AirtableOAuthConfiguration,
        scopes: Set<String> = AirtableScopes.REKORDBOT,
        authorizationEndpoint: String = DEFAULT_AUTHORIZATION_ENDPOINT,
    ): AirtableAuthorizationSession {
        require(configuration.clientId.isNotBlank()) { "Le Client ID Airtable est obligatoire." }
        require(configuration.redirectUri.isNotBlank()) {
            "L’URI de redirection Airtable est obligatoire."
        }
        val verifier = base64Url(randomBytes(VERIFIER_RANDOM_BYTE_COUNT))
        val state = base64Url(randomBytes(STATE_RANDOM_BYTE_COUNT))
        val parameters = linkedMapOf(
            "response_type" to "code",
            "client_id" to configuration.clientId.trim(),
            "redirect_uri" to configuration.redirectUri.trim(),
            "code_challenge_method" to "S256",
            "code_challenge" to codeChallenge(verifier),
            "state" to state,
            "scope" to scopes.sorted().joinToString(" "),
        )
        val query = parameters.entries.joinToString("&") { (key, value) ->
            "${key.percentEncode()}=${value.percentEncode()}"
        }
        return AirtableAuthorizationSession(
            authorizationUrl = "${authorizationEndpoint.trimEnd('?')}?$query",
            state = state,
            codeVerifier = AirtableSecret.from(verifier),
        )
    }

    fun isExpectedState(expected: String, returned: String?): Boolean {
        if (expected.isBlank() || returned.isNullOrBlank()) return false
        return MessageDigest.isEqual(
            expected.encodeToByteArray(),
            returned.encodeToByteArray(),
        )
    }

    companion object {
        const val DEFAULT_AUTHORIZATION_ENDPOINT = "https://airtable.com/oauth2/v1/authorize"
        private const val VERIFIER_RANDOM_BYTE_COUNT = 32
        private const val STATE_RANDOM_BYTE_COUNT = 24
        private val secureRandom = SecureRandom()

        fun codeChallenge(codeVerifier: String): String {
            require(codeVerifier.length in 43..128) { "Code verifier PKCE invalide." }
            val digest = MessageDigest.getInstance("SHA-256")
                .digest(codeVerifier.toByteArray(StandardCharsets.US_ASCII))
            return base64Url(digest)
        }

        private fun secureRandomBytes(size: Int): ByteArray =
            ByteArray(size).also(secureRandom::nextBytes)

        private fun base64Url(bytes: ByteArray): String =
            Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)

        private fun String.percentEncode(): String =
            URLEncoder.encode(this, StandardCharsets.UTF_8.toString()).replace("+", "%20")
    }
}

internal fun interface AirtableRandomBytesProvider {
    fun nextBytes(size: Int): ByteArray
}
