package com.loe159.rekordbot.mobile.data.remote.airtable

import com.loe159.rekordbot.mobile.domain.airtable.AirtableOAuthConfiguration
import com.loe159.rekordbot.mobile.domain.model.AirtableAuthenticationMode
import com.loe159.rekordbot.mobile.domain.model.AirtableConfiguration
import com.loe159.rekordbot.mobile.domain.repository.AirtableSessionRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

fun interface AirtableAccessTokenProvider {
    suspend fun accessToken(configuration: AirtableConfiguration): String
}

object PersonalAccessTokenProvider : AirtableAccessTokenProvider {
    override suspend fun accessToken(configuration: AirtableConfiguration): String =
        configuration.personalAccessToken.trim().ifBlank {
            throw AirtableApiException("Le token Airtable est absent.")
        }
}

class OAuthAwareAirtableAccessTokenProvider(
    private val oauthConfiguration: AirtableOAuthConfiguration,
    private val sessionRepository: AirtableSessionRepository,
    private val oauthClientFactory: (AirtableOAuthConfiguration) -> AirtableOAuthClient = {
        AirtableOAuthClient(it)
    },
    private val nowEpochSeconds: () -> Long = { System.currentTimeMillis() / 1_000 },
) : AirtableAccessTokenProvider {
    override suspend fun accessToken(configuration: AirtableConfiguration): String {
        if (configuration.authenticationMode == AirtableAuthenticationMode.PERSONAL_ACCESS_TOKEN) {
            return PersonalAccessTokenProvider.accessToken(configuration)
        }
        if (oauthConfiguration.clientId.isBlank()) {
            throw AirtableApiException(
                "Cette build ne contient pas de Client ID Airtable.",
                statusCode = 400,
            )
        }
        return refreshMutex.withLock {
            val storedTokens = sessionRepository.loadTokens()
                ?: throw AirtableApiException(
                    "Reconnecte Airtable dans les réglages.",
                    statusCode = 401,
                )
            if (!storedTokens.needsRefresh(nowEpochSeconds())) {
                storedTokens.accessToken.reveal()
            } else {
                oauthClientFactory(oauthConfiguration)
                    .refreshTokens(storedTokens)
                    .getOrThrow()
                    .also { sessionRepository.saveTokens(it) }
                    .accessToken
                    .reveal()
            }
        }
    }

    private companion object {
        val refreshMutex = Mutex()
    }
}
