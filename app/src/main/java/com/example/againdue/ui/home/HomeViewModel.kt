package com.example.againdue.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.againdue.data.local.Item
import com.example.againdue.data.repository.ItemRepository
import com.example.againdue.domain.ItemStatus
import com.example.againdue.ui.format.dateLabel
import com.example.againdue.ui.format.intervalLabel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate

data class HomeItemUi(
    val id: Long,
    val name: String,
    val intervalText: String,
    val lastDoneText: String,
    val status: ItemStatus,
    val archived: Boolean,
)

data class HomeUiState(
    val items: List<HomeItemUi> = emptyList(),
    val isLoading: Boolean = true,
    val showArchived: Boolean = false,
    val loadError: String? = null,
)

sealed interface HomeEvent {
    data class Completed(val itemId: Long) : HomeEvent
    data class Undone(val itemId: Long) : HomeEvent
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val repository: ItemRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
) : ViewModel() {

    private val showArchived = MutableStateFlow(false)
    private val refreshCount = MutableStateFlow(0)

    private val eventsChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events: Flow<HomeEvent> = eventsChannel.receiveAsFlow()

    val uiState: StateFlow<HomeUiState> = combine(showArchived, refreshCount) { archived, _ -> archived }
        .flatMapLatest { archived ->
            repository.observeItems(archived).map { items ->
                val today = LocalDate.now(clock)
                HomeUiState(
                    items = items.map { it.toUi(today) },
                    isLoading = false,
                    showArchived = archived,
                )
            }
        }
        .catch { error ->
            emit(
                HomeUiState(
                    isLoading = false,
                    showArchived = showArchived.value,
                    loadError = error.message ?: "Something went wrong while reading your items.",
                ),
            )
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun toggleArchived() {
        showArchived.value = !showArchived.value
    }

    fun retry() {
        refreshCount.value += 1
    }

    fun complete(itemId: Long) {
        viewModelScope.launch {
            if (repository.completeItem(itemId) != null) {
                eventsChannel.send(HomeEvent.Completed(itemId))
            }
        }
    }

    fun undo(itemId: Long) {
        viewModelScope.launch {
            if (repository.undoLastCompletion(itemId)) {
                eventsChannel.send(HomeEvent.Undone(itemId))
            }
        }
    }

    fun restore(itemId: Long) {
        viewModelScope.launch {
            repository.setArchived(itemId, false)
        }
    }
}

private fun Item.toUi(today: LocalDate): HomeItemUi = HomeItemUi(
    id = id,
    name = name,
    intervalText = intervalLabel(intervalValue, intervalUnit),
    lastDoneText = "Last done ${dateLabel(lastDoneAt)}",
    status = ItemStatus.from(nextDueAt, today),
    archived = archived,
)
