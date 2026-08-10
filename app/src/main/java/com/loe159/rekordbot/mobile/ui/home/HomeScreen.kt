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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.loe159.rekordbot.mobile.ui.components.RekordbotPrimaryButton
import com.loe159.rekordbot.mobile.ui.components.RekordbotStatusBadge
import com.loe159.rekordbot.mobile.ui.theme.RekordbotBorder
import com.loe159.rekordbot.mobile.ui.theme.RekordbotMutedText
import com.loe159.rekordbot.mobile.ui.theme.RekordbotPrimary
import com.loe159.rekordbot.mobile.ui.theme.RekordbotTheme
import com.loe159.rekordbot.mobile.R

@Composable
fun HomeRoute(
    isAirtableConfigured: Boolean,
    pendingTracks: Int,
    onConfigureAirtable: () -> Unit,
    onOpenQueue: () -> Unit,
) {
    HomeScreen(
        state = HomeUiState(
            isAirtableConfigured = isAirtableConfigured,
            pendingTracks = pendingTracks,
        ),
        onConfigureAirtable = onConfigureAirtable,
        onOpenQueue = onOpenQueue,
    )
}

@Composable
fun HomeScreen(
    state: HomeUiState,
    onConfigureAirtable: () -> Unit,
    onOpenQueue: () -> Unit,
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            Header()
            FoundationCard(
                isAirtableConfigured = state.isAirtableConfigured,
                pendingTracks = state.pendingTracks,
                onConfigureAirtable = onConfigureAirtable,
                onOpenQueue = onOpenQueue,
            )
            WorkflowOverview()
        }
    }
}

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = stringResource(R.string.brand_name),
            color = RekordbotPrimary,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = stringResource(R.string.home_headline),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.home_description),
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
    onOpenQueue: () -> Unit,
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
                        text = stringResource(R.string.airtable_connection),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = pluralStringResource(
                            R.plurals.pending_tracks,
                            pendingTracks,
                            pendingTracks,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = RekordbotMutedText,
                    )
                }
                RekordbotStatusBadge(
                    label = if (isAirtableConfigured) {
                        stringResource(R.string.connected)
                    } else {
                        stringResource(R.string.to_configure)
                    },
                    isPositive = isAirtableConfigured,
                )
            }

            RekordbotPrimaryButton(
                label = stringResource(R.string.queue_with_count, pendingTracks),
                onClick = onOpenQueue,
                modifier = Modifier.fillMaxWidth(),
            )
            RekordbotPrimaryButton(
                label = stringResource(R.string.configure_airtable),
                onClick = onConfigureAirtable,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = if (isAirtableConfigured) {
                    stringResource(R.string.airtable_configured_hint)
                } else {
                    stringResource(R.string.airtable_setup_hint)
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
            text = stringResource(R.string.how_it_works),
            style = MaterialTheme.typography.labelLarge,
            color = RekordbotMutedText,
            fontWeight = FontWeight.Bold,
        )
        WorkflowStep(
            index = "01",
            title = stringResource(R.string.spotify),
            description = stringResource(R.string.workflow_share),
        )
        WorkflowStep(
            index = "02",
            title = stringResource(R.string.airtable),
            description = stringResource(R.string.workflow_airtable),
        )
        WorkflowStep(
            index = "03",
            title = stringResource(R.string.rekordbot_pc),
            description = stringResource(R.string.workflow_pc),
        )
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
            onOpenQueue = {},
        )
    }
}
