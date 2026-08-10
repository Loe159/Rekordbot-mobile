package com.loe159.rekordbot.mobile.domain.spotify

data class SpotifyTokenSet(
    val accessToken: SpotifySecret,
    val refreshToken: SpotifySecret?,
    val expiresAtEpochSeconds: Long,
    val scopes: Set<String>,
    val tokenType: String = "Bearer",
) {
    fun needsRefresh(nowEpochSeconds: Long, leewaySeconds: Long = 60): Boolean =
        expiresAtEpochSeconds <= nowEpochSeconds + leewaySeconds
}
