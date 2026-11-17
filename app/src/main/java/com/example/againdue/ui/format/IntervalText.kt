package com.example.againdue.ui.format

import com.example.againdue.domain.IntervalUnit
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

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
