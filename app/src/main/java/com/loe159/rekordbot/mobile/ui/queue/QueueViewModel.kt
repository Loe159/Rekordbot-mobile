package com.loe159.rekordbot.mobile.ui.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loe159.rekordbot.mobile.domain.queue.QueueWorkScheduler
import com.loe159.rekordbot.mobile.domain.model.TrackDraft
import com.loe159.rekordbot.mobile.domain.repository.PendingTrackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QueueViewModel(
    private val repository: PendingTrackRepository,
    private val workScheduler: QueueWorkScheduler,
) : ViewModel() {
    private val editor = MutableStateFlow<QueueEditorState?>(null)

    val state: StateFlow<QueueUiState> = combine(repository.observeAll(), editor) {
            operations,
            currentEditor,
        ->
        QueueUiState(operations = operations, isLoading = false, editor = currentEditor)
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = QueueUiState(),
        )

    fun retry(operationId: String) {
        viewModelScope.launch {
            if (repository.retry(operationId)) workScheduler.schedule()
        }
    }

    fun delete(operationId: String) {
        viewModelScope.launch { repository.delete(operationId) }
    }

    fun edit(operationId: String) {
        val operation = state.value.operations.firstOrNull { it.operationId == operationId }
            ?.takeIf { it.canEdit } ?: return
        editor.value = operation.toEditorState()
    }

    fun updateEditorDraft(draft: TrackDraft) {
        editor.update { it?.copy(draft = draft, error = null) }
    }

    fun dismissEditor() {
        editor.value = null
    }

    fun saveEditor() {
        val current = editor.value?.takeIf(QueueEditorState::canSave) ?: return
        viewModelScope.launch {
            if (repository.updateDraft(current.operationId, current.draft)) {
                editor.value = null
            } else {
                editor.update { it?.copy(error = "Ce morceau n’est plus modifiable.") }
            }
        }
    }

    fun sendDraft(operationId: String) {
        viewModelScope.launch {
            if (repository.sendDraft(operationId)) workScheduler.schedule()
        }
    }

    companion object {
        fun factory(
            repository: PendingTrackRepository,
            workScheduler: QueueWorkScheduler,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = QueueViewModel(
                repository,
                workScheduler,
            ) as T
        }
    }
}
