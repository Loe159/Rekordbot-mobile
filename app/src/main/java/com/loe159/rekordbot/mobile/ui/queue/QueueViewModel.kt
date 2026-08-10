package com.loe159.rekordbot.mobile.ui.queue

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loe159.rekordbot.mobile.domain.queue.QueueWorkScheduler
import com.loe159.rekordbot.mobile.domain.repository.PendingTrackRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class QueueViewModel(
    private val repository: PendingTrackRepository,
    private val workScheduler: QueueWorkScheduler,
) : ViewModel() {
    val state: StateFlow<QueueUiState> = repository.observeAll()
        .map { QueueUiState(operations = it, isLoading = false) }
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
