package com.loe159.rekordbot.mobile.ui.share

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.produceState
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loe159.rekordbot.mobile.data.remote.airtable.AirtableGateway
import com.loe159.rekordbot.mobile.domain.repository.AirtableConfigurationRepository
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyMetadataGateway
import com.loe159.rekordbot.mobile.domain.spotify.SpotifyShareParseResult

@Composable
fun SharePreviewRoute(
    parseResult: SpotifyShareParseResult,
    metadataGateway: SpotifyMetadataGateway,
    configurationRepository: AirtableConfigurationRepository,
    airtableGateway: AirtableGateway,
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
    val viewModel: SharePreviewViewModel = viewModel(
        key = "share-${System.identityHashCode(parseResult)}",
        factory = SharePreviewViewModel.factory(
            initialDraft = parseResult.draft,
            configurationRepository = configurationRepository,
            airtableGateway = airtableGateway,
        ),
    )
    val state by viewModel.state.collectAsState()

    LaunchedEffect(resolvedResult.draft) {
        viewModel.applyEnrichedDraft(resolvedResult.draft)
    }

    SharePreviewScreen(
        state = state,
        initialMessage = when {
            enrichment == null -> "Récupération du titre et de l’artiste depuis Spotify…"
            else -> resolvedResult.userMessage()
        },
        isInitialError = resolvedResult.draft.spotifyTrackId.isBlank(),
        isMetadataLoading = enrichment == null,
        onDraftChange = viewModel::updateDraft,
        onSubmit = viewModel::submit,
        onBack = onBack,
    )
}
