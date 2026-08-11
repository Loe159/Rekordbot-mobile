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
    const val PLAYLIST_READ_PRIVATE = "playlist-read-private"
    const val PLAYBACK_CONTROL = "user-modify-playback-state"

    val SHAZAM: Set<String> = setOf(
        PLAYLIST_READ_PRIVATE,
        PLAYBACK_CONTROL,
    )
}
