package com.loe159.rekordbot.mobile.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.loe159.rekordbot.mobile.ui.components.RekordbotPrimaryButton
import com.loe159.rekordbot.mobile.ui.components.RekordbotStatusBadge
import com.loe159.rekordbot.mobile.ui.theme.RekordbotBorder
import com.loe159.rekordbot.mobile.ui.theme.RekordbotMutedText
import com.loe159.rekordbot.mobile.ui.theme.RekordbotPrimary
import com.loe159.rekordbot.mobile.ui.theme.RekordbotTheme

@Composable
fun HomeRoute(
    isAirtableConfigured: Boolean,
    onConfigureAirtable: () -> Unit,
) {
    HomeScreen(
        state = HomeUiState(isAirtableConfigured = isAirtableConfigured),
        onConfigureAirtable = onConfigureAirtable,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onConfigureAirtable: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { contentPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header()
            FoundationCard(
                isAirtableConfigured = state.isAirtableConfigured,
                pendingTracks = state.pendingTracks,
                onConfigureAirtable = onConfigureAirtable,
            )
            WorkflowOverview()
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = "REKORDBOT",
            color = RekordbotPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "Prépare tes morceaux partout.",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = "Configure Airtable maintenant, puis le partage Spotify arrivera à l’étape suivante.",
            style = MaterialTheme.typography.bodyMedium,
            color = RekordbotMutedText,
        )
    }
}

@Composable
private fun FoundationCard(
    isAirtableConfigured: Boolean,
    pendingTracks: Int,
    onConfigureAirtable: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, RekordbotBorder),
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "Connexion Airtable",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "$pendingTracks morceau en attente",
                        style = MaterialTheme.typography.bodySmall,
                        color = RekordbotMutedText,
                    )
                }
                RekordbotStatusBadge(
                    label = if (isAirtableConfigured) "Connecté" else "À configurer",
                    isPositive = isAirtableConfigured,
                )
            }

            RekordbotPrimaryButton(
                label = "Configurer Airtable",
                onClick = onConfigureAirtable,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = if (isAirtableConfigured) {
                    "La connexion a été validée. Tu peux modifier ou retester les réglages."
                } else {
                    "Ajoute ton token, ta base, ta table et les noms de champs."
                },
                style = MaterialTheme.typography.labelMedium,
                color = RekordbotMutedText,
            )
        }
    }
}

@Composable
private fun WorkflowOverview() {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "FLUX CIBLE",
            style = MaterialTheme.typography.labelLarge,
            color = RekordbotMutedText,
            fontWeight = FontWeight.Bold,
        )
        WorkflowStep(index = "01", title = "Spotify", description = "Partager un morceau")
        WorkflowStep(index = "02", title = "Airtable", description = "Capturer et qualifier")
        WorkflowStep(index = "03", title = "Rekordbot PC", description = "Synchroniser vers Rekordbox")
    }
}

@Composable
private fun WorkflowStep(
    index: String,
    title: String,
    description: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
            ) {}
            Text(
                text = index,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.SemiBold,
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = RekordbotMutedText,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF111318)
@Composable
private fun HomeScreenPreview() {
    RekordbotTheme {
        HomeScreen(
            state = HomeUiState(),
            onConfigureAirtable = {},
        )
    }
}
