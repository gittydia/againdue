package com.example.againdue.domain

import java.time.DayOfWeek
import java.time.LocalDate

/** Which occurrence of a weekday inside a month a monthly pattern targets. */
enum class WeekOrdinal {
    FIRST,
    SECOND,
    THIRD,
    FOURTH,
    LAST,
}

/**
 * Extra structure layered on top of a monthly interval.
 *
 * A `null` pattern keeps the original behavior: the next date counts forward
 * from the day the item was actually completed. A non-null pattern switches to
 * calendar dates anchored to [RecurrenceConfig.startDate].
 */
sealed interface RecurrencePattern {

    /** The same day of the month as the start date, clamped in short months. */
    data object MonthlyDate : RecurrencePattern

    /** For example, the first Monday or the last Friday of the month. */
    data class MonthlyNthWeekday(
        val ordinal: WeekOrdinal,
        val dayOfWeek: DayOfWeek,
    ) : RecurrencePattern
}

/**
 * Everything needed to advance a recurring item.
 *
 * [startDate] is the first date the recurrence may land on. [endDate] is the
 * last allowed date, and [endAfterCount] caps the number of completions.
 */
data class RecurrenceConfig(
    val intervalValue: Int,
    val intervalUnit: IntervalUnit,
    val startDate: LocalDate,
    val pattern: RecurrencePattern? = null,
    val endDate: LocalDate? = null,
    val endAfterCount: Int? = null,
)

/**
 * The next occurrence plus whether the recurrence has run its course.
 *
 * [nextDueAt] is still populated when [ended] is true so the item keeps a
 * stable position in lists; it is simply the occurrence that crossed the end
 * condition and must never be completed.
 */
data class RecurrenceResult(
    val nextDueAt: LocalDate,
    val ended: Boolean,
)
