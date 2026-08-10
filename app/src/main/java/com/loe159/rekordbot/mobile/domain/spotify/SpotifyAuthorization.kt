package com.loe159.rekordbot.mobile.domain.spotify

class SpotifySecret private constructor(private val value: String) {
    internal fun reveal(): String = value

    override fun toString(): String = "[REDACTED]"

    companion object {
        fun from(value: String): SpotifySecret {
            require(value.isNotBlank()) { "Une valeur secrète Spotify ne peut pas être vide." }
            return SpotifySecret(value)
        }
    }
}

data class SpotifyAuthorizationSession(
    val authorizationUrl: String,
    val state: String,
    val codeVerifier: SpotifySecret,
)

object SpotifyScopes {
    val SHAZAM_PLAYLIST_READ: Set<String> = setOf("playlist-read-private")
}
