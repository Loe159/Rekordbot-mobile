package com.loe159.rekordbot.mobile.domain.airtable

import java.net.URI

data class AirtableOAuthConfiguration(
    val clientId: String,
    val redirectUri: String,
)

/** Strictly matches the verified HTTPS App Link registered with Airtable. */
object AirtableAuthorizationCallback {
    fun isSecureRedirectUri(uri: String): Boolean {
        if (uri.isBlank()) return false
        return runCatching {
            val parsed = URI(uri)
            parsed.scheme.equals("https", ignoreCase = true) &&
                !parsed.host.isNullOrBlank() &&
                parsed.userInfo == null &&
                parsed.rawQuery == null &&
                parsed.fragment == null
        }.getOrDefault(false)
    }

    fun isExpected(callbackUri: String, expectedRedirectUri: String): Boolean {
        if (callbackUri.isBlank() || !isSecureRedirectUri(expectedRedirectUri)) return false
        return runCatching {
            val callback = URI(callbackUri)
            val expected = URI(expectedRedirectUri)
            expected.scheme.equals("https", ignoreCase = true) &&
                callback.scheme.equals(expected.scheme, ignoreCase = true) &&
                callback.host.equals(expected.host, ignoreCase = true) &&
                callback.port == expected.port &&
                callback.rawPath == expected.rawPath &&
                callback.userInfo == null &&
                callback.fragment == null &&
                expected.userInfo == null &&
                expected.rawQuery == null &&
                expected.fragment == null
        }.getOrDefault(false)
    }
}

class AirtableSecret private constructor(private val value: String) {
    fun reveal(): String = value

    override fun toString(): String = "AirtableSecret(***)"

    override fun equals(other: Any?): Boolean = other is AirtableSecret && value == other.value

    override fun hashCode(): Int = value.hashCode()

    companion object {
        fun from(value: String): AirtableSecret {
            require(value.isNotBlank()) { "Un secret Airtable vide est invalide." }
            return AirtableSecret(value)
        }
    }
}

data class AirtableAuthorizationSession(
    val authorizationUrl: String,
    val state: String,
    val codeVerifier: AirtableSecret,
)

data class AirtableTokenSet(
    val accessToken: AirtableSecret,
    val refreshToken: AirtableSecret,
    val expiresAtEpochSeconds: Long,
    val scopes: Set<String>,
    val tokenType: String = "Bearer",
) {
    fun needsRefresh(nowEpochSeconds: Long, marginSeconds: Long = 60): Boolean =
        expiresAtEpochSeconds <= nowEpochSeconds + marginSeconds
}

object AirtableScopes {
    const val SCHEMA_BASES_READ = "schema.bases:read"
    const val DATA_RECORDS_READ = "data.records:read"
    const val DATA_RECORDS_WRITE = "data.records:write"

    val REKORDBOT: Set<String> = setOf(
        SCHEMA_BASES_READ,
        DATA_RECORDS_READ,
        DATA_RECORDS_WRITE,
    )
}

data class AirtableBaseSummary(
    val id: String,
    val name: String,
    val permissionLevel: String?,
)

data class AirtableTableSummary(
    val id: String,
    val name: String,
)
