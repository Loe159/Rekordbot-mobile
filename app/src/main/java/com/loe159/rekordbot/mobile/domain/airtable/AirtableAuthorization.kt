package com.loe159.rekordbot.mobile.domain.airtable

data class AirtableOAuthConfiguration(
    val clientId: String,
    val redirectUri: String = DEFAULT_REDIRECT_URI,
) {
    companion object {
        const val DEFAULT_REDIRECT_URI = "rekordbot-mobile-login://airtable-callback"
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
