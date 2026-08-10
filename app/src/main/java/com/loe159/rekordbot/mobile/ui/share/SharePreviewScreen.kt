package com.loe159.rekordbot.mobile.ui.share

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.airtable.AirtableTrackSubmissionResult
import com.loe159.rekordbot.mobile.ui.components.RekordbotPrimaryButton
import com.loe159.rekordbot.mobile.ui.components.DjQualificationFields
import com.loe159.rekordbot.mobile.ui.components.RekordbotStatusBadge
import com.loe159.rekordbot.mobile.ui.theme.RekordbotBorder
import com.loe159.rekordbot.mobile.ui.theme.RekordbotError
import com.loe159.rekordbot.mobile.ui.theme.RekordbotMutedText
import com.loe159.rekordbot.mobile.ui.theme.RekordbotPrimary
import com.loe159.rekordbot.mobile.ui.theme.RekordbotSuccess
import com.loe159.rekordbot.mobile.ui.theme.RekordbotTheme
import com.loe159.rekordbot.mobile.ui.theme.RekordbotWarning
import com.loe159.rekordbot.mobile.R

@Composable
fun SharePreviewScreen(
    state: SharePreviewUiState,
    initialMessage: String?,
    isInitialError: Boolean,
    isMetadataLoading: Boolean = false,
    onDraftChange: ((TrackDraft) -> TrackDraft) -> Unit,
    onSaveDraft: () -> Unit,
    onSubmit: () -> Unit,
    onReplaceSuggestedGenre: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val draft = state.draft
    val isQueued = (state.submissionResult as? AirtableTrackSubmissionResult.Deferred)
        ?.isPersisted == true
    val isLocked = state.isSubmitting ||
        state.isSavingDraft || state.savedDraftOperationId != null ||
        state.submissionResult is AirtableTrackSubmissionResult.Added || isQueued

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ShareHeader(onBack)
            PreviewCard(
                title = draft.title,
                artist = draft.artist,
                spotifyUrl = draft.spotifyUrl,
                trackId = draft.spotifyTrackId,
                isrc = draft.isrc.orEmpty(),
                rawGenre = draft.rawGenre.orEmpty(),
                isComplete = draft.isReadyForAirtable,
                onTitleChange = { value -> onDraftChange { it.copy(title = value) } },
                onArtistChange = { value -> onDraftChange { it.copy(artist = value) } },
                onSpotifyUrlChange = { value -> onDraftChange { it.copy(spotifyUrl = value) } },
                onTrackIdChange = { value ->
                    onDraftChange { it.copy(spotifyTrackId = value) }
                },
                onIsrcChange = { value -> onDraftChange { it.copy(isrc = value.ifBlank { null }) } },
                onRawGenreChange = { value ->
                    onDraftChange { it.copy(rawGenre = value.ifBlank { null }) }
                },
                enabled = !isMetadataLoading && !isLocked,
            )
            DjQualificationFields(
                draft = draft,
                onDraftChange = { changed -> onDraftChange { changed } },
                enabled = !isLocked,
            )
            initialMessage?.let {
                ShareMessage(message = it, isError = isInitialError)
            }
            state.soundchartsMessage?.let {
                SoundchartsSuggestionMessage(
                    message = it,
                    suggestion = state.suggestedRawGenre,
                    onReplace = onReplaceSuggestedGenre,
                )
            }
            state.submissionResult?.let { SubmissionMessage(it) }
            state.savedDraftOperationId?.let {
                ShareMessage(
                    message = "Brouillon enregistré dans la file. Tu pourras le modifier et l’envoyer plus tard.",
                    isError = false,
                )
            }
            state.draftSaveError?.let { ShareMessage(message = it, isError = true) }
            if (!state.isConfigurationLoading && !state.isAirtableConfigured) {
                ShareMessage(
                    message = "Configure et teste Airtable depuis l’accueil avant l’envoi.",
                    isError = true,
                )
            }
            RekordbotPrimaryButton(
                label = when {
                    state.submissionResult is AirtableTrackSubmissionResult.Added -> "Ajouté à Airtable"
                    isQueued -> "Conservé dans la file"
                    state.isSubmitting -> "Ajout en cours…"
                    else -> stringResource(R.string.add_to_airtable)
                },
                onClick = onSubmit,
                enabled = draft.isReadyForAirtable && state.isAirtableConfigured && !isLocked,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedButton(
                onClick = onSaveDraft,
                enabled = !isLocked,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(
                    if (state.isSavingDraft) {
                        stringResource(R.string.saving)
                    } else {
                        stringResource(R.string.save_draft)
                    },
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun ShareHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        Column {
            Text(
                text = "PARTAGE SPOTIFY",
                style = MaterialTheme.typography.labelLarge,
                color = RekordbotPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Aperçu du morceau",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun PreviewCard(
    title: String,
    artist: String,
    spotifyUrl: String,
    trackId: String,
    isrc: String,
    rawGenre: String,
    isComplete: Boolean,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onSpotifyUrlChange: (String) -> Unit,
    onTrackIdChange: (String) -> Unit,
    onIsrcChange: (String) -> Unit,
    onRawGenreChange: (String) -> Unit,
    enabled: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, RekordbotBorder),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Métadonnées",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                RekordbotStatusBadge(
                    label = if (isComplete) "Prêt" else "À compléter",
                    isPositive = isComplete,
                )
            }
            PreviewTextField("Titre", title, onTitleChange, enabled)
            PreviewTextField("Artiste", artist, onArtistChange, enabled)
            PreviewTextField("Lien Spotify", spotifyUrl, onSpotifyUrlChange, enabled)
            PreviewTextField("Spotify Track ID", trackId, onTrackIdChange, enabled)
            PreviewTextField("ISRC (optionnel)", isrc, onIsrcChange, enabled)
            PreviewTextField("Genre brut (optionnel)", rawGenre, onRawGenreChange, enabled)
        }
    }
}

@Composable
private fun SoundchartsSuggestionMessage(
    message: String,
    suggestion: String?,
    onReplace: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = RekordbotPrimary.copy(alpha = 0.1f),
        border = BorderStroke(1.dp, RekordbotPrimary.copy(alpha = 0.45f)),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
            if (suggestion != null) {
                Text("Suggestion : $suggestion", style = MaterialTheme.typography.bodySmall)
                TextButton(onClick = onReplace) { Text("Remplacer par la suggestion") }
            }
        }
    }
}

@Composable
private fun PreviewTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
        enabled = enabled,
    )
}

@Composable
private fun ShareMessage(message: String, isError: Boolean) {
    val accent = if (isError) RekordbotError else RekordbotWarning
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = accent,
        )
    }
}

@Composable
private fun SubmissionMessage(result: AirtableTrackSubmissionResult) {
    val (message, accent) = when (result) {
        is AirtableTrackSubmissionResult.Added ->
            "Morceau ajouté à Airtable · ${result.recordId}" to RekordbotSuccess
        is AirtableTrackSubmissionResult.DuplicateBlocked ->
            "Doublon bloqué : ce Track ID existe déjà · ${result.recordId}" to RekordbotWarning
        is AirtableTrackSubmissionResult.Failed -> result.message to RekordbotError
        is AirtableTrackSubmissionResult.Deferred ->
            if (result.isPersisted) {
                "Réseau indisponible : morceau conservé dans la file. L’envoi reprendra automatiquement." to RekordbotWarning
            } else {
                "Envoi différé — non enregistré localement. ${result.reason}" to RekordbotWarning
            }
    }
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = accent,
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun SharePreviewScreenPreview() {
    RekordbotTheme {
        SharePreviewScreen(
            state = SharePreviewUiState(
                draft = TrackDraft(
                    spotifyTrackId = "5lFNqg3eMNMuJsnFRKB460",
                    title = "Open Eye Signal",
                    artist = "Jon Hopkins",
                    spotifyUrl = "https://open.spotify.com/track/5lFNqg3eMNMuJsnFRKB460",
                ),
                isConfigurationLoading = false,
                isAirtableConfigured = true,
            ),
            initialMessage = null,
            isInitialError = false,
            onDraftChange = {},
            onSaveDraft = {},
            onSubmit = {},
            onReplaceSuggestedGenre = {},
            onBack = {},
        )
    }
}
