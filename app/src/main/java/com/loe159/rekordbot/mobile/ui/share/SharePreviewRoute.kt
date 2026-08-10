package com.loe159.rekordbot.mobile.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyMetadataGateway
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyShareParseResult

@Composable
fun SharePreviewRoute(
    parseResult: SpotifyShareParseResult,
    metadataGateway: SpotifyMetadataGateway,
    onBack: () -> Unit,
) {
    val needsMetadata = parseResult.draft.spotifyTrackId.isNotBlank() &&
        (parseResult.draft.title.isBlank() || parseResult.draft.artist.isBlank())
    val enrichment by produceState<SpotifyShareParseResult?>(
        initialValue = if (needsMetadata) null else parseResult,
        parseResult,
        metadataGateway,
    ) {
        if (!needsMetadata) {
            value = parseResult
            return@produceState
        }
        value = metadataGateway.fetchTrackMetadata(parseResult.draft.spotifyTrackId)
            .fold(
                onSuccess = parseResult::withMetadata,
                onFailure = { parseResult },
            )
    }
    val resolvedResult = enrichment ?: parseResult

    SharePreviewScreen(
        initialDraft = resolvedResult.draft,
        initialMessage = when {
            enrichment == null -> "Récupération du titre et de l’artiste depuis Spotify…"
            else -> resolvedResult.userMessage()
        },
        isInitialError = resolvedResult.draft.spotifyTrackId.isBlank(),
        isMetadataLoading = enrichment == null,
        onBack = onBack,
    )
}
