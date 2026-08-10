package com.loe159.rekordbot.mobile.ui.shazam

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loe159.rekordbot.mobile.domain.shazam.ShazamDecision
import com.loe159.rekordbot.mobile.domain.shazam.ShazamInboxCounts
import com.loe159.rekordbot.mobile.domain.shazam.ShazamInboxTrack
import com.loe159.rekordbot.mobile.ui.components.RekordbotPrimaryButton
import com.loe159.rekordbot.mobile.ui.components.RekordbotStatusBadge
import com.loe159.rekordbot.mobile.ui.theme.RekordbotBorder
import com.loe159.rekordbot.mobile.ui.theme.RekordbotError
import com.loe159.rekordbot.mobile.ui.theme.RekordbotMutedText
import com.loe159.rekordbot.mobile.ui.theme.RekordbotPrimary
import com.loe159.rekordbot.mobile.ui.theme.RekordbotSuccess
import com.loe159.rekordbot.mobile.ui.theme.RekordbotTheme

@Composable
fun ShazamScreen(
    state: ShazamUiState,
    onBack: () -> Unit,
    onClientIdChange: (String) -> Unit,
    onPlaylistNameChange: (String) -> Unit,
    onSaveConfiguration: () -> Unit,
    onConnect: () -> Unit,
    onSynchronize: () -> Unit,
    onTabSelected: (ShazamInboxTab) -> Unit,
    onPrepare: (ShazamInboxTrack) -> Unit,
    onIgnore: (ShazamInboxTrack) -> Unit,
    onRestore: (ShazamInboxTrack) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                ShazamHeader(onBack = onBack)
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (state.isConnected) {
                item {
                    ConnectedSection(
                        state = state,
                        onSynchronize = onSynchronize,
                    )
                }
            } else {
                item {
                    SetupSection(
                        state = state,
                        onClientIdChange = onClientIdChange,
                        onPlaylistNameChange = onPlaylistNameChange,
                        onSaveConfiguration = onSaveConfiguration,
                        onConnect = onConnect,
                    )
                }
            }

            state.message?.let { message ->
                item {
                    FeedbackCard(message = message, isError = false)
                }
            }
            state.errorMessage?.let { error ->
                item {
                    FeedbackCard(message = error, isError = true)
                }
            }

            if (state.isConnected) {
                item {
                    InboxTabs(
                        selectedTab = state.selectedTab,
                        counts = state.counts,
                        enabled = !state.isLoading,
                        onTabSelected = onTabSelected,
                    )
                }

                if (state.isLoading && state.visibleTracks.isEmpty()) {
                    item { LoadingState() }
                } else if (state.visibleTracks.isEmpty()) {
                    item { EmptyState(tab = state.selectedTab) }
                } else {
                    items(
                        items = state.visibleTracks,
                        key = ShazamInboxTrack::spotifyTrackId,
                    ) { track ->
                        ShazamTrackCard(
                            track = track,
                            tab = state.selectedTab,
                            enabled = !state.isBusy,
                            onPrepare = { onPrepare(track) },
                            onIgnore = { onIgnore(track) },
                            onRestore = { onRestore(track) },
                        )
                    }
                }
            } else if (state.isLoading) {
                item { LoadingState() }
            }

            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun ShazamHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) { Text("Retour") }
        Column {
            Text(
                text = "SHAZAM",
                style = MaterialTheme.typography.labelLarge,
                color = RekordbotPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "Morceaux à préparer",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun SetupSection(
    state: ShazamUiState,
    onClientIdChange: (String) -> Unit,
    onPlaylistNameChange: (String) -> Unit,
    onSaveConfiguration: () -> Unit,
    onConnect: () -> Unit,
) {
    SectionCard {
        Text(
            text = "Connexion Spotify",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = "Renseigne l’application Spotify qui donnera accès à ta playlist synchronisée par Shazam.",
            style = MaterialTheme.typography.bodySmall,
            color = RekordbotMutedText,
        )
        OutlinedTextField(
            value = state.clientId,
            onValueChange = onClientIdChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Client ID Spotify") },
            singleLine = true,
            enabled = !state.isBusy,
        )
        OutlinedTextField(
            value = state.playlistName,
            onValueChange = onPlaylistNameChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Nom de la playlist Shazam") },
            singleLine = true,
            enabled = !state.isBusy,
        )
        OutlinedButton(
            onClick = onSaveConfiguration,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.hasUsableConfiguration && !state.isBusy,
            shape = MaterialTheme.shapes.medium,
        ) {
            if (state.isSavingConfiguration) {
                ButtonProgress()
            }
            Text(if (state.isSavingConfiguration) "Enregistrement…" else "Enregistrer")
        }
        RekordbotPrimaryButton(
            label = if (state.isConnecting) "Connexion…" else "Connecter Spotify",
            onClick = onConnect,
            modifier = Modifier.fillMaxWidth(),
            enabled = state.hasUsableConfiguration && !state.isBusy,
        )
    }
}

@Composable
private fun ConnectedSection(
    state: ShazamUiState,
    onSynchronize: () -> Unit,
) {
    SectionCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = state.accountName?.takeIf(String::isNotBlank) ?: "Compte Spotify",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = state.connectedPlaylistName?.takeIf(String::isNotBlank)
                        ?: state.playlistName,
                    style = MaterialTheme.typography.bodySmall,
                    color = RekordbotMutedText,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            RekordbotStatusBadge(label = "Connecté", isPositive = true)
        }
        RekordbotPrimaryButton(
            label = if (state.isSyncing) "Synchronisation…" else "Synchroniser",
            onClick = onSynchronize,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isBusy,
        )
    }
}

@Composable
private fun InboxTabs(
    selectedTab: ShazamInboxTab,
    counts: ShazamInboxCounts,
    enabled: Boolean,
    onTabSelected: (ShazamInboxTab) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        FilterChip(
            selected = selectedTab == ShazamInboxTab.PENDING,
            onClick = { onTabSelected(ShazamInboxTab.PENDING) },
            label = { Text("À traiter (${counts.pending})") },
            enabled = enabled,
        )
        FilterChip(
            selected = selectedTab == ShazamInboxTab.IGNORED,
            onClick = { onTabSelected(ShazamInboxTab.IGNORED) },
            label = { Text("Ignorés (${counts.ignored})") },
            enabled = enabled,
        )
    }
}

@Composable
private fun ShazamTrackCard(
    track: ShazamInboxTrack,
    tab: ShazamInboxTab,
    enabled: Boolean,
    onPrepare: () -> Unit,
    onIgnore: () -> Unit,
    onRestore: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, RekordbotBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtworkPlaceholder()
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp),
                ) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = track.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = RekordbotMutedText,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    track.albumName?.takeIf(String::isNotBlank)?.let { album ->
                        Text(
                            text = album,
                            style = MaterialTheme.typography.labelMedium,
                            color = RekordbotMutedText,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            when (tab) {
                ShazamInboxTab.PENDING -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        RekordbotPrimaryButton(
                            label = "Préparer",
                            onClick = onPrepare,
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                        )
                        OutlinedButton(
                            onClick = onIgnore,
                            modifier = Modifier.weight(1f),
                            enabled = enabled,
                            shape = MaterialTheme.shapes.medium,
                        ) {
                            Text("Ignorer")
                        }
                    }
                }

                ShazamInboxTab.IGNORED -> {
                    OutlinedButton(
                        onClick = onRestore,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text("Restaurer")
                    }
                }
            }
        }
    }
}

@Composable
private fun ArtworkPlaceholder() {
    Surface(
        modifier = Modifier.size(56.dp),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, RekordbotBorder),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = "♪",
                style = MaterialTheme.typography.headlineMedium,
                color = RekordbotPrimary,
            )
        }
    }
}

@Composable
private fun FeedbackCard(message: String, isError: Boolean) {
    val accent = if (isError) RekordbotError else RekordbotSuccess
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = accent.copy(alpha = 0.12f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(14.dp),
            style = MaterialTheme.typography.bodySmall,
            color = accent,
        )
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState(tab: ShazamInboxTab) {
    SectionCard {
        Text(
            text = when (tab) {
                ShazamInboxTab.PENDING -> "Aucun morceau à traiter."
                ShazamInboxTab.IGNORED -> "Aucun morceau ignoré."
            },
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = when (tab) {
                ShazamInboxTab.PENDING ->
                    "Synchronise ta playlist pour récupérer tes derniers Shazams."
                ShazamInboxTab.IGNORED ->
                    "Les morceaux que tu ignores pourront être restaurés ici."
            },
            style = MaterialTheme.typography.bodySmall,
            color = RekordbotMutedText,
        )
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, RekordbotBorder),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
    }
}

@Composable
private fun ButtonProgress() {
    CircularProgressIndicator(
        modifier = Modifier
            .padding(end = 8.dp)
            .size(16.dp),
        strokeWidth = 2.dp,
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun ShazamScreenPreview() {
    val track = ShazamInboxTrack(
        spotifyTrackId = "5lFNqg3eMNMuJsnFRKB460",
        title = "Open Eye Signal (under the fabric)",
        artist = "Jon Hopkins",
        spotifyUrl = "https://open.spotify.com/track/5lFNqg3eMNMuJsnFRKB460",
        albumName = "Immunity",
        artworkUrl = null,
        isrc = "GBCEL1300218",
        playlistAddedAtEpochMillis = null,
        playlistPosition = 0,
        decision = ShazamDecision.PENDING,
        decisionUpdatedAt = null,
        firstSeenAt = 0L,
        lastSeenAt = 0L,
    )

    RekordbotTheme {
        ShazamScreen(
            state = ShazamUiState(
                clientId = "spotify-client-id",
                isConnected = true,
                accountName = "Loëvan",
                connectedPlaylistName = "Mes titres Shazam",
                counts = ShazamInboxCounts(
                    pending = 1,
                    saved = 8,
                    ignored = 2,
                    alreadyPresent = 3,
                ),
                pendingTracks = listOf(track),
                message = "Playlist synchronisée.",
            ),
            onBack = {},
            onClientIdChange = {},
            onPlaylistNameChange = {},
            onSaveConfiguration = {},
            onConnect = {},
            onSynchronize = {},
            onTabSelected = {},
            onPrepare = {},
            onIgnore = {},
            onRestore = {},
        )
    }
}
