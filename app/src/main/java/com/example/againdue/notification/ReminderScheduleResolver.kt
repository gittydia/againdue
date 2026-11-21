package com.example.againdue.notification

import com.example.againdue.data.local.Item
import java.time.Instant
import java.time.ZoneId

sealed interface ReminderScheduleResolution {
    data object GloballyDisabled : ReminderScheduleResolution
    data object InactiveItem : ReminderScheduleResolution
    data class Active(
        val minuteOfDay: Int,
        val dueAt: Instant,
    ) : ReminderScheduleResolution
}

object ReminderScheduleResolver {
    fun resolve(
        item: Item,
        settings: ReminderSettings,
        zoneId: ZoneId,
    ): ReminderScheduleResolution {
        if (!settings.enabled) return ReminderScheduleResolution.GloballyDisabled
        if (item.archived || item.recurrenceEnded) return ReminderScheduleResolution.InactiveItem

        val minuteOfDay = item.reminderMinuteOfDay ?: settings.hour * 60 + settings.minute
        return ReminderScheduleResolution.Active(
            minuteOfDay = minuteOfDay,
            dueAt = ReminderScheduleCalculator.dueAt(
                nextDueAt = item.nextDueAt,
                minuteOfDay = minuteOfDay,
                zoneId = zoneId,
            ),
        )
    }
}
