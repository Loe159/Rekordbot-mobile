package com.loe159.rekordbot.mobile.ui.queue

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loe159.rekordbot.mobile.domain.queue.QueueStatus
import com.loe159.rekordbot.mobile.domain.queue.QueuedTrackOperation
import com.loe159.rekordbot.mobile.domain.queue.QueueWorkScheduler
import com.loe159.rekordbot.mobile.domain.repository.PendingTrackRepository
import com.loe159.rekordbot.mobile.ui.components.RekordbotStatusBadge
import com.loe159.rekordbot.mobile.ui.theme.RekordbotBorder
import com.loe159.rekordbot.mobile.ui.theme.RekordbotError
import com.loe159.rekordbot.mobile.ui.theme.RekordbotMutedText
import com.loe159.rekordbot.mobile.ui.theme.RekordbotPrimary
import androidx.compose.runtime.collectAsState

@Composable
fun QueueRoute(
    repository: PendingTrackRepository,
    workScheduler: QueueWorkScheduler,
    onBack: () -> Unit,
) {
    val viewModel: QueueViewModel = viewModel(
        factory = QueueViewModel.factory(repository, workScheduler),
    )
    val state by viewModel.state.collectAsState()
    QueueScreen(
        state = state,
        onRetry = viewModel::retry,
        onDelete = viewModel::delete,
        onBack = onBack,
    )
}

@Composable
fun QueueScreen(
    state: QueueUiState,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    onBack: () -> Unit,
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
                QueueHeader(onBack)
                Spacer(modifier = Modifier.height(8.dp))
            }
            if (!state.isLoading && state.operations.isEmpty()) {
                item { EmptyQueue() }
            }
            items(state.operations, key = QueuedTrackOperation::operationId) { operation ->
                QueueCard(operation, onRetry, onDelete)
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun QueueHeader(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        TextButton(onClick = onBack) { Text("‹ Retour") }
        Column {
            Text(
                text = "FILE D’ATTENTE",
                style = MaterialTheme.typography.labelLarge,
                color = RekordbotPrimary,
                fontWeight = FontWeight.Bold,
            )
            Text("Envois Airtable", style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun EmptyQueue() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, RekordbotBorder),
    ) {
        Text(
            text = "Aucun morceau dans la file.",
            modifier = Modifier.padding(20.dp),
            color = RekordbotMutedText,
        )
    }
}

@Composable
private fun QueueCard(
    operation: QueuedTrackOperation,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, RekordbotBorder),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = operation.draft.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = operation.draft.artist,
                        style = MaterialTheme.typography.bodySmall,
                        color = RekordbotMutedText,
                    )
                }
                RekordbotStatusBadge(
                    label = operation.status.label(),
                    isPositive = operation.status == QueueStatus.SENT,
                )
            }
            Text(
                text = "Tentatives : ${operation.attemptCount}",
                style = MaterialTheme.typography.labelMedium,
                color = RekordbotMutedText,
            )
            operation.airtableRecordId?.let {
                Text("Airtable · $it", style = MaterialTheme.typography.labelMedium)
            }
            operation.lastError?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = RekordbotError)
            }
            if (operation.canRetry || operation.canDelete) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (operation.canRetry) {
                        TextButton(onClick = { onRetry(operation.operationId) }) {
                            Text("Réessayer")
                        }
                    }
                    if (operation.canDelete) {
                        TextButton(onClick = { onDelete(operation.operationId) }) {
                            Text("Supprimer")
                        }
                    }
                }
            }
        }
    }
}

private fun QueueStatus.label(): String = when (this) {
    QueueStatus.PENDING -> "En attente"
    QueueStatus.SENDING -> "Envoi…"
    QueueStatus.SENT -> "Envoyé"
    QueueStatus.FAILED -> "Échec"
}
