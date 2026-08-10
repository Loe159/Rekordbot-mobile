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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.ui.components.RekordbotStatusBadge
import com.loe159.rekordbot.mobile.ui.theme.RekordbotBorder
import com.loe159.rekordbot.mobile.ui.theme.RekordbotError
import com.loe159.rekordbot.mobile.ui.theme.RekordbotMutedText
import com.loe159.rekordbot.mobile.ui.theme.RekordbotPrimary
import com.loe159.rekordbot.mobile.ui.theme.RekordbotTheme
import com.loe159.rekordbot.mobile.ui.theme.RekordbotWarning

@Composable
fun SharePreviewScreen(
    initialDraft: TrackDraft,
    initialMessage: String?,
    isInitialError: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var title by rememberSaveable(initialDraft) { mutableStateOf(initialDraft.title) }
    var artist by rememberSaveable(initialDraft) { mutableStateOf(initialDraft.artist) }
    var spotifyUrl by rememberSaveable(initialDraft) { mutableStateOf(initialDraft.spotifyUrl) }
    var trackId by rememberSaveable(initialDraft) { mutableStateOf(initialDraft.spotifyTrackId) }
    val isComplete = title.isNotBlank() && artist.isNotBlank() &&
        spotifyUrl.isNotBlank() && trackId.isNotBlank()

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
                title = title,
                artist = artist,
                spotifyUrl = spotifyUrl,
                trackId = trackId,
                isComplete = isComplete,
                onTitleChange = { title = it },
                onArtistChange = { artist = it },
                onSpotifyUrlChange = { spotifyUrl = it },
                onTrackIdChange = { trackId = it },
            )
            initialMessage?.let {
                ShareMessage(message = it, isError = isInitialError)
            }
            Text(
                text = "Aucun morceau n’est envoyé automatiquement. La création Airtable sera ajoutée en P3.",
                style = MaterialTheme.typography.labelMedium,
                color = RekordbotMutedText,
            )
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
        TextButton(onClick = onBack) { Text("‹ Retour") }
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
    isComplete: Boolean,
    onTitleChange: (String) -> Unit,
    onArtistChange: (String) -> Unit,
    onSpotifyUrlChange: (String) -> Unit,
    onTrackIdChange: (String) -> Unit,
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
            PreviewTextField("Titre", title, onTitleChange)
            PreviewTextField("Artiste", artist, onArtistChange)
            PreviewTextField("Lien Spotify", spotifyUrl, onSpotifyUrlChange)
            PreviewTextField("Spotify Track ID", trackId, onTrackIdChange)
        }
    }
}

@Composable
private fun PreviewTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = true,
        shape = MaterialTheme.shapes.medium,
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

@Preview(showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun SharePreviewScreenPreview() {
    RekordbotTheme {
        SharePreviewScreen(
            initialDraft = TrackDraft(
                spotifyTrackId = "5lFNqg3eMNMuJsnFRKB460",
                title = "Open Eye Signal",
                artist = "Jon Hopkins",
                spotifyUrl = "https://open.spotify.com/track/5lFNqg3eMNMuJsnFRKB460",
            ),
            initialMessage = null,
            isInitialError = false,
            onBack = {},
        )
    }
}
