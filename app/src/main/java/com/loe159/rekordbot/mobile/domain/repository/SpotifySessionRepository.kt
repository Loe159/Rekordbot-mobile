package com.loe159.rekordbot.mobile.domain.repository

import com.loe159.rekordbot.mobile.domain.spotify.SpotifyAuthorizationSession
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyTokenSet

interface SpotifySessionRepository {
    suspend fun loadTokens(): SpotifyTokenSet?
    suspend fun saveTokens(tokens: SpotifyTokenSet)
    suspend fun clearTokens()

    suspend fun loadPendingAuthorization(): SpotifyAuthorizationSession?
    suspend fun savePendingAuthorization(session: SpotifyAuthorizationSession)
    suspend fun clearPendingAuthorization()
}
