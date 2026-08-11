package com.loe159.rekordbot.mobile.domain.repository

import com.loe159.rekordbot.mobile.domain.airtable.AirtableAuthorizationSession
import com.loe159.rekordbot.mobile.domain.airtable.AirtableTokenSet

interface AirtableSessionRepository {
    suspend fun loadTokens(): AirtableTokenSet?

    suspend fun saveTokens(tokens: AirtableTokenSet)

    suspend fun clearTokens()

    suspend fun loadPendingAuthorization(): AirtableAuthorizationSession?

    suspend fun savePendingAuthorization(session: AirtableAuthorizationSession)

    suspend fun clearPendingAuthorization()
}
