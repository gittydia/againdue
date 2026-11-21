package com.example.againdue.data.local

import androidx.room.TypeConverter
import com.example.againdue.domain.IntervalUnit
import com.example.againdue.domain.RecurrencePattern
import com.example.againdue.domain.WeekOrdinal
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate

/**
 * Room type converters for the value types used by the domain.
 *
 * Dates are stored as epoch days and timestamps as epoch milliseconds so the
 * schema stays portable and does not depend on the device time zone.
 */
class Converters {

    @TypeConverter
    fun fromEpochDay(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun toEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun fromEpochMilli(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun toEpochMilli(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun fromIntervalUnit(value: String?): IntervalUnit? = value?.let(IntervalUnit::valueOf)

    @TypeConverter
    fun toIntervalUnit(value: IntervalUnit?): String? = value?.name

    /**
     * Patterns are stored as compact text so the column stays readable:
     * `monthlyDate`, `nth:FIRST:MONDAY`, or null for completion-based items.
     */
    @TypeConverter
    fun fromRecurrencePattern(value: String?): RecurrencePattern? = when {
        value == null -> null
        value == MONTHLY_DATE -> RecurrencePattern.MonthlyDate
        value.startsWith(NTH_PREFIX) -> {
            val parts = value.removePrefix(NTH_PREFIX).split(':')
            if (parts.size != 2) {
                throw IllegalArgumentException("Malformed recurrence pattern: $value")
            }
            RecurrencePattern.MonthlyNthWeekday(
                ordinal = WeekOrdinal.valueOf(parts[0]),
                dayOfWeek = DayOfWeek.valueOf(parts[1]),
            )
        }

        else -> throw IllegalArgumentException("Unknown recurrence pattern: $value")
    }

    @TypeConverter
    fun toRecurrencePattern(value: RecurrencePattern?): String? = when (value) {
        null -> null
        RecurrencePattern.MonthlyDate -> MONTHLY_DATE
        is RecurrencePattern.MonthlyNthWeekday -> "$NTH_PREFIX${value.ordinal.name}:${value.dayOfWeek.name}"
    }

    private companion object {
        const val MONTHLY_DATE = "monthlyDate"
        const val NTH_PREFIX = "nth:"
    }
}
