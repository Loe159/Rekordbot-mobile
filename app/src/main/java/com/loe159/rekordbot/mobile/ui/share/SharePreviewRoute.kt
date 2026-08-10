package com.loe159.rekordbot.mobile.ui.share

import androidx.compose.runtime.Composable
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyShareParseResult

@Composable
fun SharePreviewRoute(
    parseResult: SpotifyShareParseResult,
    onBack: () -> Unit,
) {
    SharePreviewScreen(
        initialDraft = parseResult.draft,
        initialMessage = parseResult.userMessage(),
        isInitialError = parseResult.draft.spotifyTrackId.isBlank(),
        onBack = onBack,
    )
}
