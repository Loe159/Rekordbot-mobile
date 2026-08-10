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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.ui.components.DjQualificationFields
import com.loe159.rekordbot.mobile.ui.theme.RekordbotWarning
import com.loe159.rekordbot.mobile.R

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
        onEdit = viewModel::edit,
        onSendDraft = viewModel::sendDraft,
        onEditorDraftChange = viewModel::updateEditorDraft,
        onSaveEditor = viewModel::saveEditor,
        onDismissEditor = viewModel::dismissEditor,
        onBack = onBack,
    )
}

@Composable
fun QueueScreen(
    state: QueueUiState,
    onRetry: (String) -> Unit,
    onDelete: (String) -> Unit,
    onEdit: (String) -> Unit,
    onSendDraft: (String) -> Unit,
    onEditorDraftChange: (TrackDraft) -> Unit,
    onSaveEditor: () -> Unit,
    onDismissEditor: () -> Unit,
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
            if (state.isLoading) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
            if (!state.isLoading && state.operations.isEmpty()) {
                item { EmptyQueue() }
            }
            items(state.operations, key = QueuedTrackOperation::operationId) { operation ->
                QueueCard(operation, onRetry, onDelete, onEdit, onSendDraft)
            }
            item { Spacer(modifier = Modifier.height(16.dp)) }
        }
    }

    state.editor?.let { editor ->
        QueueEditorDialog(
            editor = editor,
            onDraftChange = onEditorDraftChange,
            onSave = onSaveEditor,
            onDismiss = onDismissEditor,
        )
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
        TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
        Column {
            Text(
                text = stringResource(R.string.queue),
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
    onEdit: (String) -> Unit,
    onSendDraft: (String) -> Unit,
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
                        text = operation.draft.title.ifBlank { "Titre à compléter" },
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = operation.draft.artist.ifBlank { "Artiste à compléter" },
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
            QualificationSummary(operation.draft)
            if (
                operation.canRetry || operation.canDelete || operation.canEdit ||
                operation.canSendDraft
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (operation.canEdit) {
                        TextButton(onClick = { onEdit(operation.operationId) }) {
                            Text("Modifier")
                        }
                    }
                    if (operation.canSendDraft) {
                        TextButton(onClick = { onSendDraft(operation.operationId) }) {
                            Text("Envoyer")
                        }
                    }
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

@Composable
private fun QualificationSummary(draft: TrackDraft) {
    val tags = buildList {
        draft.energy?.let { add("$it ★") }
        addAll(draft.moods)
        addAll(draft.situations)
        addAll(draft.inspirationalDjs)
    }
    if (tags.isNotEmpty()) {
        Text(
            text = tags.joinToString(" · "),
            style = MaterialTheme.typography.bodySmall,
            color = RekordbotPrimary,
        )
    }
    draft.comment?.takeIf(String::isNotBlank)?.let {
        Text(it, style = MaterialTheme.typography.bodySmall, color = RekordbotMutedText)
    }
}

@Composable
private fun QueueEditorDialog(
    editor: QueueEditorState,
    onDraftChange: (TrackDraft) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                if (editor.originalStatus == QueueStatus.DRAFT) {
                    "Modifier le brouillon"
                } else {
                    "Modifier le morceau"
                },
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 560.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                QueueEditorField("Titre", editor.draft.title) {
                    onDraftChange(editor.draft.copy(title = it))
                }
                QueueEditorField("Artiste", editor.draft.artist) {
                    onDraftChange(editor.draft.copy(artist = it))
                }
                QueueEditorField("Lien Spotify", editor.draft.spotifyUrl) {
                    onDraftChange(editor.draft.copy(spotifyUrl = it))
                }
                QueueEditorField("Spotify Track ID", editor.draft.spotifyTrackId) {
                    onDraftChange(editor.draft.copy(spotifyTrackId = it))
                }
                QueueEditorField("ISRC (optionnel)", editor.draft.isrc.orEmpty()) {
                    onDraftChange(editor.draft.copy(isrc = it.ifBlank { null }))
                }
                DjQualificationFields(
                    draft = editor.draft,
                    onDraftChange = onDraftChange,
                    showContainer = false,
                )
                editor.error?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = RekordbotError)
                }
                if (!editor.canSave) {
                    Text(
                        "Complète les métadonnées obligatoires avant d’enregistrer.",
                        style = MaterialTheme.typography.bodySmall,
                        color = RekordbotWarning,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave, enabled = editor.canSave) { Text("Enregistrer") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Annuler") } },
    )
}

@Composable
private fun QueueEditorField(
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
    )
}

private fun QueueStatus.label(): String = when (this) {
    QueueStatus.DRAFT -> "Brouillon"
    QueueStatus.PENDING -> "En attente"
    QueueStatus.SENDING -> "Envoi…"
    QueueStatus.SENT -> "Envoyé"
    QueueStatus.FAILED -> "Échec"
}
