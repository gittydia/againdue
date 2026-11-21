package com.example.againdue.ui.item

import com.example.againdue.domain.IntervalUnit
import com.example.againdue.domain.RecurrencePattern
import com.example.againdue.domain.WeekOrdinal
import java.time.DayOfWeek
import java.time.LocalDate

enum class MonthlyMode {
    INTERVAL,
    CALENDAR_DATE,
    NTH_WEEKDAY,
}

enum class RecurrenceEndMode {
    NEVER,
    ON_DATE,
    AFTER_COUNT,
}

data class ItemFormErrors(
    val name: String? = null,
    val interval: String? = null,
    val end: String? = null,
) {
    val hasErrors: Boolean
        get() = name != null || interval != null || end != null
}

data class ItemFormState(
    val name: String = "",
    val intervalText: String = "",
    val unit: IntervalUnit = IntervalUnit.DAY,
    val lastDoneAt: LocalDate = LocalDate.now(),
    val note: String = "",
    val startDate: LocalDate = lastDoneAt,
    val monthlyMode: MonthlyMode = MonthlyMode.INTERVAL,
    val ordinal: WeekOrdinal = WeekOrdinal.FIRST,
    val dayOfWeek: DayOfWeek = DayOfWeek.MONDAY,
    val endMode: RecurrenceEndMode = RecurrenceEndMode.NEVER,
    val endDate: LocalDate? = null,
    val endAfterCountText: String = "",
) {
    fun intervalValueOrNull(): Int? = intervalText.trim().toIntOrNull()

    fun endAfterCountOrNull(): Int? = endAfterCountText.trim().toIntOrNull()

    fun pattern(): RecurrencePattern? {
        if (unit != IntervalUnit.MONTH) return null
        return when (monthlyMode) {
            MonthlyMode.INTERVAL -> null
            MonthlyMode.CALENDAR_DATE -> RecurrencePattern.MonthlyDate
            MonthlyMode.NTH_WEEKDAY -> RecurrencePattern.MonthlyNthWeekday(ordinal, dayOfWeek)
        }
    }

    fun errors(): ItemFormErrors = ItemFormErrors(
        name = if (name.isBlank()) "Enter a name" else null,
        interval = when (val value = intervalValueOrNull()) {
            null -> if (intervalText.isBlank()) {
                "Enter how often this repeats"
            } else {
                "Use a whole number"
            }

            else -> if (value <= 0) "Interval must be at least 1" else null
        },
        end = endError(),
    )

    private fun endError(): String? = when (endMode) {
        RecurrenceEndMode.NEVER -> null
        RecurrenceEndMode.ON_DATE -> when {
            endDate == null -> "Choose an end date"
            endDate.isBefore(startDate) -> "End date must be on or after the start date"
            else -> null
        }

        RecurrenceEndMode.AFTER_COUNT -> when (val count = endAfterCountOrNull()) {
            null -> if (endAfterCountText.isBlank()) {
                "Enter how many completions it should take"
            } else {
                "Use a whole number"
            }

            else -> if (count <= 0) "Must be at least 1" else null
        }
    }
}
