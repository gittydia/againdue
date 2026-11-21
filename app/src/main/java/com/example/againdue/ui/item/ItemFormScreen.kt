package com.example.againdue.ui.item

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.againdue.domain.IntervalUnit
import com.example.againdue.domain.RecurrenceConfig
import com.example.againdue.domain.WeekOrdinal
import com.example.againdue.ui.format.dateLabel
import com.example.againdue.ui.format.intervalUnitLabel
import com.example.againdue.ui.format.monthlyDateLabel
import com.example.againdue.ui.format.ordinalLabel
import com.example.againdue.ui.format.recurrencePreview
import com.example.againdue.ui.format.weekdayLabel
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemFormScreen(
    viewModel: ItemFormViewModel,
    onClose: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var dateTarget by remember { mutableStateOf<DateTarget?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { onClose() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (viewModel.isEditing) "Edit item" else "New item") },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                if (state.isMissing) {
                    Text(
                        text = "This item no longer exists.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }

                OutlinedTextField(
                    value = state.form.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Name") },
                    singleLine = true,
                    isError = state.errors.name != null,
                    supportingText = state.errors.name?.let { message -> { Text(message) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("name_field"),
                )

                OutlinedTextField(
                    value = state.form.intervalText,
                    onValueChange = viewModel::onIntervalChange,
                    label = { Text("Repeat every") },
                    singleLine = true,
                    isError = state.errors.interval != null,
                    supportingText = state.errors.interval?.let { message -> { Text(message) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("interval_field"),
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    IntervalUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = state.form.unit == unit,
                            onClick = { viewModel.onUnitChange(unit) },
                            label = { Text(intervalUnitLabel(unit)) },
                        )
                    }
                }

                Column {
                    Text(
                        text = "Last done",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { dateTarget = DateTarget.LAST_DONE },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("last_done_button"),
                    ) {
                        Icon(Icons.Filled.DateRange, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(dateLabel(state.form.lastDoneAt))
                    }
                }

                Column {
                    Text(
                        text = "Starts",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { dateTarget = DateTarget.START },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("start_date_button"),
                    ) {
                        Icon(Icons.Filled.DateRange, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(dateLabel(state.form.startDate))
                    }
                }

                if (state.form.unit == IntervalUnit.MONTH) {
                    Column {
                        Text(
                            text = "Monthly pattern",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(
                                selected = state.form.monthlyMode == MonthlyMode.INTERVAL,
                                onClick = { viewModel.onMonthlyModeChange(MonthlyMode.INTERVAL) },
                                label = { Text("Every N months") },
                                modifier = Modifier.testTag("monthly_mode_interval"),
                            )
                            FilterChip(
                                selected = state.form.monthlyMode == MonthlyMode.CALENDAR_DATE,
                                onClick = { viewModel.onMonthlyModeChange(MonthlyMode.CALENDAR_DATE) },
                                label = { Text("Day of month") },
                                modifier = Modifier.testTag("monthly_mode_date"),
                            )
                            FilterChip(
                                selected = state.form.monthlyMode == MonthlyMode.NTH_WEEKDAY,
                                onClick = { viewModel.onMonthlyModeChange(MonthlyMode.NTH_WEEKDAY) },
                                label = { Text("Weekday") },
                                modifier = Modifier.testTag("monthly_mode_nth"),
                            )
                        }

                        when (state.form.monthlyMode) {
                            MonthlyMode.INTERVAL -> Unit

                            MonthlyMode.CALENDAR_DATE -> {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = "Repeats ${monthlyDateLabel(state.form.startDate)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            MonthlyMode.NTH_WEEKDAY -> {
                                Spacer(Modifier.height(8.dp))
                                WeekOrdinal.entries.chunked(3).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { ordinal ->
                                            FilterChip(
                                                selected = state.form.ordinal == ordinal,
                                                onClick = { viewModel.onOrdinalChange(ordinal) },
                                                label = {
                                                    Text(
                                                        ordinalLabel(ordinal)
                                                            .replaceFirstChar { it.uppercase() },
                                                    )
                                                },
                                            )
                                        }
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                DayOfWeek.entries.chunked(4).forEach { row ->
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        row.forEach { day ->
                                            FilterChip(
                                                selected = state.form.dayOfWeek == day,
                                                onClick = { viewModel.onDayOfWeekChange(day) },
                                                label = { Text(weekdayLabel(day).take(3)) },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Column {
                    Text(
                        text = "Ends",
                        style = MaterialTheme.typography.labelLarge,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = state.form.endMode == RecurrenceEndMode.NEVER,
                            onClick = { viewModel.onEndModeChange(RecurrenceEndMode.NEVER) },
                            label = { Text("Never") },
                            modifier = Modifier.testTag("end_mode_never"),
                        )
                        FilterChip(
                            selected = state.form.endMode == RecurrenceEndMode.ON_DATE,
                            onClick = { viewModel.onEndModeChange(RecurrenceEndMode.ON_DATE) },
                            label = { Text("On date") },
                            modifier = Modifier.testTag("end_mode_date"),
                        )
                        FilterChip(
                            selected = state.form.endMode == RecurrenceEndMode.AFTER_COUNT,
                            onClick = { viewModel.onEndModeChange(RecurrenceEndMode.AFTER_COUNT) },
                            label = { Text("After N times") },
                            modifier = Modifier.testTag("end_mode_count"),
                        )
                    }

                    when (state.form.endMode) {
                        RecurrenceEndMode.NEVER -> Unit

                        RecurrenceEndMode.ON_DATE -> {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { dateTarget = DateTarget.END },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("end_date_button"),
                            ) {
                                Icon(Icons.Filled.DateRange, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    state.form.endDate?.let(::dateLabel) ?: "Choose end date",
                                )
                            }
                        }

                        RecurrenceEndMode.AFTER_COUNT -> {
                            Spacer(Modifier.height(8.dp))
                            OutlinedTextField(
                                value = state.form.endAfterCountText,
                                onValueChange = viewModel::onEndAfterCountChange,
                                label = { Text("Completions") },
                                singleLine = true,
                                isError = state.errors.end != null,
                                supportingText = state.errors.end?.let { message ->
                                    { Text(message) }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("end_after_field"),
                            )
                        }
                    }

                    if (state.form.endMode == RecurrenceEndMode.ON_DATE &&
                        state.errors.end != null
                    ) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = state.errors.end.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }

                ItemReminderTimeSection(
                    form = state.form,
                    onUseCustomTimeChange = viewModel::onUseCustomReminderTimeChange,
                    onTimeChange = viewModel::onReminderTimeChange,
                )

                val preview = recurrencePreview(
                    RecurrenceConfig(
                        intervalValue = state.form.intervalValueOrNull() ?: 1,
                        intervalUnit = state.form.unit,
                        startDate = state.form.startDate,
                        pattern = state.form.pattern(),
                        endDate = if (state.form.endMode == RecurrenceEndMode.ON_DATE) {
                            state.form.endDate
                        } else {
                            null
                        },
                        endAfterCount = if (state.form.endMode == RecurrenceEndMode.AFTER_COUNT) {
                            state.form.endAfterCountOrNull()
                        } else {
                            null
                        },
                    ),
                )
                Text(
                    text = preview,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.testTag("recurrence_preview"),
                )

                OutlinedTextField(
                    value = state.form.note,
                    onValueChange = viewModel::onNoteChange,
                    label = { Text("Note (optional)") },
                    minLines = 3,
                    maxLines = 5,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("note_field"),
                )

                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_button"),
                ) {
                    Text(if (viewModel.isEditing) "Save changes" else "Add item")
                }

                if (viewModel.isEditing) {
                    HorizontalDivider()
                    OutlinedButton(
                        onClick = { viewModel.setArchived(!state.isArchived) },
                        enabled = !state.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("archive_button"),
                    ) {
                        Text(if (state.isArchived) "Restore item" else "Archive item")
                    }
                    TextButton(
                        onClick = { confirmDelete = true },
                        enabled = !state.isSaving,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("delete_button"),
                    ) {
                        Text("Delete item")
                    }
                }
            }
        }
    }

    val target = dateTarget
    if (target != null) {
        val initialDate = when (target) {
            DateTarget.LAST_DONE -> state.form.lastDoneAt
            DateTarget.START -> state.form.startDate
            DateTarget.END -> state.form.endDate ?: state.form.startDate
        }
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = initialDate.toUtcMillis(),
        )
        DatePickerDialog(
            onDismissRequest = { dateTarget = null },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { millis ->
                            val picked = millis.toUtcLocalDate()
                            when (target) {
                                DateTarget.LAST_DONE -> viewModel.onLastDoneChange(picked)
                                DateTarget.START -> viewModel.onStartDateChange(picked)
                                DateTarget.END -> viewModel.onEndDateChange(picked)
                            }
                        }
                        dateTarget = null
                    },
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { dateTarget = null }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete this item?") },
            text = { Text("Its completion history is removed too. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        viewModel.delete()
                    },
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) {
                    Text("Cancel")
                }
            },
        )
    }
}

private enum class DateTarget {
    LAST_DONE,
    START,
    END,
}

private fun LocalDate.toUtcMillis(): Long =
    atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun Long.toUtcLocalDate(): LocalDate =
    Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate()
