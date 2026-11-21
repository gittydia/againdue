package com.example.againdue.ui.item

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.againdue.data.repository.ItemRepository
import com.example.againdue.domain.IntervalUnit
import com.example.againdue.domain.RecurrencePattern
import com.example.againdue.domain.WeekOrdinal
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.DayOfWeek
import java.time.LocalDate

data class ItemFormUiState(
    val form: ItemFormState,
    val errors: ItemFormErrors = ItemFormErrors(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isArchived: Boolean = false,
    val isMissing: Boolean = false,
)

sealed interface ItemFormEvent {
    data object Saved : ItemFormEvent
    data object Deleted : ItemFormEvent
    data object Archived : ItemFormEvent
    data object Restored : ItemFormEvent
    data object Missing : ItemFormEvent
}

class ItemFormViewModel(
    private val repository: ItemRepository,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val itemId: Long? = null,
) : ViewModel() {

    val isEditing: Boolean = itemId != null

    private val _uiState = MutableStateFlow(
        ItemFormUiState(
            form = ItemFormState(lastDoneAt = LocalDate.now(clock)),
            isLoading = itemId != null,
        ),
    )
    val uiState: StateFlow<ItemFormUiState> = _uiState.asStateFlow()

    private val eventsChannel = Channel<ItemFormEvent>(Channel.BUFFERED)
    val events: Flow<ItemFormEvent> = eventsChannel.receiveAsFlow()

    init {
        val id = itemId
        if (id != null) {
            viewModelScope.launch {
                val item = repository.getItem(id)
                if (item == null) {
                    _uiState.update { it.copy(isLoading = false, isMissing = true) }
                    eventsChannel.send(ItemFormEvent.Missing)
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isArchived = item.archived,
                            form = ItemFormState(
                                name = item.name,
                                intervalText = item.intervalValue.toString(),
                                unit = item.intervalUnit,
                                lastDoneAt = item.lastDoneAt,
                                note = item.note.orEmpty(),
                                startDate = item.startDate ?: item.lastDoneAt,
                                monthlyMode = when (val pattern = item.pattern) {
                                    null -> MonthlyMode.INTERVAL
                                    RecurrencePattern.MonthlyDate -> MonthlyMode.CALENDAR_DATE
                                    is RecurrencePattern.MonthlyNthWeekday -> MonthlyMode.NTH_WEEKDAY
                                },
                                ordinal = (item.pattern as? RecurrencePattern.MonthlyNthWeekday)
                                    ?.ordinal ?: WeekOrdinal.FIRST,
                                dayOfWeek = (item.pattern as? RecurrencePattern.MonthlyNthWeekday)
                                    ?.dayOfWeek ?: DayOfWeek.MONDAY,
                                endMode = when {
                                    item.endAfterCount != null -> RecurrenceEndMode.AFTER_COUNT
                                    item.endDate != null -> RecurrenceEndMode.ON_DATE
                                    else -> RecurrenceEndMode.NEVER
                                },
                                endDate = item.endDate,
                                endAfterCountText = item.endAfterCount?.toString().orEmpty(),
                            ),
                        )
                    }
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update {
            it.copy(
                form = it.form.copy(name = value),
                errors = it.errors.copy(name = null),
            )
        }
    }

    fun onIntervalChange(value: String) {
        _uiState.update {
            it.copy(
                form = it.form.copy(intervalText = value),
                errors = it.errors.copy(interval = null),
            )
        }
    }

    fun onUnitChange(unit: IntervalUnit) {
        _uiState.update { it.copy(form = it.form.copy(unit = unit)) }
    }

    fun onLastDoneChange(date: LocalDate) {
        _uiState.update { it.copy(form = it.form.copy(lastDoneAt = date)) }
    }

    fun onNoteChange(value: String) {
        _uiState.update { it.copy(form = it.form.copy(note = value)) }
    }

    fun onStartDateChange(date: LocalDate) {
        _uiState.update { it.copy(form = it.form.copy(startDate = date)) }
    }

    fun onMonthlyModeChange(mode: MonthlyMode) {
        _uiState.update { it.copy(form = it.form.copy(monthlyMode = mode)) }
    }

    fun onOrdinalChange(ordinal: WeekOrdinal) {
        _uiState.update { it.copy(form = it.form.copy(ordinal = ordinal)) }
    }

    fun onDayOfWeekChange(dayOfWeek: DayOfWeek) {
        _uiState.update { it.copy(form = it.form.copy(dayOfWeek = dayOfWeek)) }
    }

    fun onEndModeChange(mode: RecurrenceEndMode) {
        _uiState.update {
            it.copy(
                form = it.form.copy(endMode = mode),
                errors = it.errors.copy(end = null),
            )
        }
    }

    fun onEndDateChange(date: LocalDate) {
        _uiState.update {
            it.copy(
                form = it.form.copy(endDate = date),
                errors = it.errors.copy(end = null),
            )
        }
    }

    fun onEndAfterCountChange(value: String) {
        _uiState.update {
            it.copy(
                form = it.form.copy(endAfterCountText = value),
                errors = it.errors.copy(end = null),
            )
        }
    }

    fun save() {
        val form = _uiState.value.form
        val errors = form.errors()
        if (errors.hasErrors) {
            _uiState.update { it.copy(errors = errors) }
            return
        }
        val intervalValue = form.intervalValueOrNull() ?: return
        val pattern = form.pattern()
        val endDate = if (form.endMode == RecurrenceEndMode.ON_DATE) form.endDate else null
        val endAfterCount = if (form.endMode == RecurrenceEndMode.AFTER_COUNT) {
            form.endAfterCountOrNull()
        } else {
            null
        }
        val id = itemId

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            if (id == null) {
                repository.createItem(
                    name = form.name,
                    intervalValue = intervalValue,
                    intervalUnit = form.unit,
                    lastDoneAt = form.lastDoneAt,
                    note = form.note,
                    startDate = form.startDate,
                    pattern = pattern,
                    endDate = endDate,
                    endAfterCount = endAfterCount,
                )
                eventsChannel.send(ItemFormEvent.Saved)
            } else {
                val updated = repository.updateItem(
                    id = id,
                    name = form.name,
                    intervalValue = intervalValue,
                    intervalUnit = form.unit,
                    lastDoneAt = form.lastDoneAt,
                    note = form.note,
                    startDate = form.startDate,
                    pattern = pattern,
                    endDate = endDate,
                    endAfterCount = endAfterCount,
                )
                _uiState.update { it.copy(isSaving = false) }
                eventsChannel.send(if (updated) ItemFormEvent.Saved else ItemFormEvent.Missing)
            }
        }
    }

    fun setArchived(archived: Boolean) {
        val id = itemId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            val changed = repository.setArchived(id, archived)
            if (changed) {
                _uiState.update { it.copy(isSaving = false, isArchived = archived) }
                eventsChannel.send(if (archived) ItemFormEvent.Archived else ItemFormEvent.Restored)
            } else {
                _uiState.update { it.copy(isSaving = false, isMissing = true) }
                eventsChannel.send(ItemFormEvent.Missing)
            }
        }
    }

    fun delete() {
        val id = itemId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            repository.deleteItem(id)
            eventsChannel.send(ItemFormEvent.Deleted)
        }
    }
}
