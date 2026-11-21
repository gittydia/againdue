package com.example.againdue.ui.item

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun ItemReminderTimeSection(
    form: ItemFormState,
    onUseCustomTimeChange: (Boolean) -> Unit,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
) {
    val context = LocalContext.current
    val hour = form.reminderMinuteOfDay / 60
    val minute = form.reminderMinuteOfDay % 60

    Column {
        Text(
            text = "Reminder time",
            style = MaterialTheme.typography.labelLarge,
        )
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FilterChip(
                selected = !form.useCustomReminderTime,
                onClick = { onUseCustomTimeChange(false) },
                label = { Text("Use default") },
                modifier = Modifier.testTag("reminder_time_default"),
            )
            FilterChip(
                selected = form.useCustomReminderTime,
                onClick = { onUseCustomTimeChange(true) },
                label = { Text("Custom time") },
                modifier = Modifier.testTag("reminder_time_custom"),
            )
        }
        Spacer(Modifier.height(4.dp))
        if (form.useCustomReminderTime) {
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, selectedHour, selectedMinute ->
                            onTimeChange(selectedHour, selectedMinute)
                        },
                        hour,
                        minute,
                        true,
                    ).show()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("reminder_time_button"),
            ) {
                Text(formatReminderTime(form.reminderMinuteOfDay))
            }
        } else {
            Text(
                text = "Uses Settings: ${formatReminderTime(form.reminderMinuteOfDay)}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun formatReminderTime(minuteOfDay: Int): String =
    "%02d:%02d".format(minuteOfDay / 60, minuteOfDay % 60)
