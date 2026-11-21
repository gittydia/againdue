package com.example.againdue.ui.format

import com.example.againdue.domain.IntervalUnit
import com.example.againdue.domain.RecurrenceConfig
import com.example.againdue.domain.RecurrencePattern
import com.example.againdue.domain.WeekOrdinal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Locale

private val dateFormatter: DateTimeFormatter =
    DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)

fun intervalLabel(value: Int, unit: IntervalUnit): String {
    if (value == 1) {
        return when (unit) {
            IntervalUnit.DAY -> "Every day"
            IntervalUnit.WEEK -> "Every week"
            IntervalUnit.MONTH -> "Every month"
        }
    }
    val unitWord = when (unit) {
        IntervalUnit.DAY -> "days"
        IntervalUnit.WEEK -> "weeks"
        IntervalUnit.MONTH -> "months"
    }
    return "Every $value $unitWord"
}

fun intervalUnitLabel(unit: IntervalUnit): String = when (unit) {
    IntervalUnit.DAY -> "Days"
    IntervalUnit.WEEK -> "Weeks"
    IntervalUnit.MONTH -> "Months"
}

fun dateLabel(date: LocalDate): String = date.format(dateFormatter)

fun ordinalLabel(ordinal: WeekOrdinal): String = when (ordinal) {
    WeekOrdinal.FIRST -> "first"
    WeekOrdinal.SECOND -> "second"
    WeekOrdinal.THIRD -> "third"
    WeekOrdinal.FOURTH -> "fourth"
    WeekOrdinal.LAST -> "last"
}

fun weekdayLabel(dayOfWeek: DayOfWeek): String =
    dayOfWeek.getDisplayName(TextStyle.FULL, Locale.getDefault())

fun dayOfMonthLabel(date: LocalDate): String =
    "the ${date.dayOfMonth}${ordinalSuffix(date.dayOfMonth)}"

fun monthlyDateLabel(startDate: LocalDate): String = "on ${dayOfMonthLabel(startDate)}"

fun monthlyNthWeekdayLabel(ordinal: WeekOrdinal, dayOfWeek: DayOfWeek): String =
    "on the ${ordinalLabel(ordinal)} ${weekdayLabel(dayOfWeek)}"

fun scheduleLabel(
    intervalValue: Int,
    unit: IntervalUnit,
    pattern: RecurrencePattern? = null,
    startDate: LocalDate? = null,
): String {
    if (pattern == null) return intervalLabel(intervalValue, unit)
    val every = intervalLabel(intervalValue, IntervalUnit.MONTH)
    return when (pattern) {
        RecurrencePattern.MonthlyDate ->
            "$every ${monthlyDateLabel(startDate ?: LocalDate.now())}"

        is RecurrencePattern.MonthlyNthWeekday ->
            "$every ${monthlyNthWeekdayLabel(pattern.ordinal, pattern.dayOfWeek)}"
    }
}

fun endAfterLabel(count: Int): String =
    if (count == 1) "Ends after 1 completion" else "Ends after $count completions"

fun recurrencePreview(recurrence: RecurrenceConfig): String {
    val parts = mutableListOf(
        scheduleLabel(
            intervalValue = recurrence.intervalValue,
            unit = recurrence.intervalUnit,
            pattern = recurrence.pattern,
            startDate = recurrence.startDate,
        ),
        "Starting ${dateLabel(recurrence.startDate)}",
    )
    when {
        recurrence.endAfterCount != null -> parts += endAfterLabel(recurrence.endAfterCount)
        recurrence.endDate != null -> parts += "Ends ${dateLabel(recurrence.endDate)}"
    }
    return parts.joinToString(" \u00b7 ")
}

private fun ordinalSuffix(day: Int): String {
    val suffix = when {
        day % 100 in 11..13 -> "th"
        day % 10 == 1 -> "st"
        day % 10 == 2 -> "nd"
        day % 10 == 3 -> "rd"
        else -> "th"
    }
    return suffix
}
