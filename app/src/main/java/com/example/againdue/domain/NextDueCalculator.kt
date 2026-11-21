package com.example.againdue.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

/**
 * Derives an item's next due date from when it was actually completed.
 *
 * The default calculation is always anchored to the real completion date, so
 * early and late completions shift the following due date. Months use calendar
 * arithmetic (for example, January 31 plus one month is February 28 or 29),
 * never a 30-day approximation.
 *
 * A [RecurrencePattern] opts into calendar-anchored schedules instead: the
 * occurrence ladder is fixed by [RecurrenceConfig.startDate], so completing an
 * item early does not pull the following dates forward.
 */
object NextDueCalculator {

    /**
     * Returns the next due date for [lastDoneAt] plus [intervalValue] units.
     *
     * @throws IllegalArgumentException when [intervalValue] is not positive.
     */
    fun nextDue(
        lastDoneAt: LocalDate,
        intervalValue: Int,
        intervalUnit: IntervalUnit,
    ): LocalDate {
        require(intervalValue > 0) { "Interval value must be positive, was $intervalValue" }
        return when (intervalUnit) {
            IntervalUnit.DAY -> lastDoneAt.plusDays(intervalValue.toLong())
            IntervalUnit.WEEK -> lastDoneAt.plusWeeks(intervalValue.toLong())
            IntervalUnit.MONTH -> lastDoneAt.plusMonths(intervalValue.toLong())
        }
    }

    /**
     * Returns the next occurrence for a full recurrence definition.
     *
     * [completedCount] is the number of completions already recorded and only
     * matters when [RecurrenceConfig.endAfterCount] is set.
     *
     * When the next occurrence falls past the end condition it is still
     * returned, with [RecurrenceResult.ended] set, so callers can display a
     * final date instead of losing the item.
     *
     * @throws IllegalArgumentException when the interval is not positive, when
     *   [RecurrenceConfig.pattern] is used with a non-monthly unit, or when
     *   [RecurrenceConfig.endAfterCount] is not positive.
     */
    fun nextRecurrence(
        lastDoneAt: LocalDate,
        recurrence: RecurrenceConfig,
        completedCount: Int = 0,
    ): RecurrenceResult {
        val intervalValue = recurrence.intervalValue
        require(intervalValue > 0) { "Interval value must be positive, was $intervalValue" }
        recurrence.endAfterCount?.let {
            require(it > 0) { "End-after count must be positive, was $it" }
        }

        val pattern = recurrence.pattern
        if (pattern != null) {
            require(recurrence.intervalUnit == IntervalUnit.MONTH) {
                "Recurrence patterns repeat in months, was ${recurrence.intervalUnit}"
            }
        }

        val next = when (pattern) {
            null -> nextDue(lastDoneAt, intervalValue, recurrence.intervalUnit)
            RecurrencePattern.MonthlyDate -> nextMonthlyDate(lastDoneAt, recurrence)
            is RecurrencePattern.MonthlyNthWeekday -> nextMonthlyNthWeekday(lastDoneAt, recurrence, pattern)
        }

        val pastEndDate = recurrence.endDate?.let { next.isAfter(it) } == true
        val countReached = recurrence.endAfterCount?.let { completedCount >= it } == true

        return RecurrenceResult(
            nextDueAt = next,
            ended = pastEndDate || countReached,
        )
    }

    private fun nextMonthlyDate(
        lastDoneAt: LocalDate,
        recurrence: RecurrenceConfig,
    ): LocalDate {
        val startDate = recurrence.startDate
        val startMonth = YearMonth.from(startDate)
        var step = 0L
        while (true) {
            val month = startMonth.plusMonths(step * recurrence.intervalValue)
            val day = minOf(startDate.dayOfMonth, month.lengthOfMonth())
            val candidate = month.atDay(day)
            if (candidate.isAfter(lastDoneAt)) return candidate
            step += 1
        }
    }

    private fun nextMonthlyNthWeekday(
        lastDoneAt: LocalDate,
        recurrence: RecurrenceConfig,
        pattern: RecurrencePattern.MonthlyNthWeekday,
    ): LocalDate {
        val startDate = recurrence.startDate
        val startMonth = YearMonth.from(startDate)
        var step = 0L
        while (true) {
            val month = startMonth.plusMonths(step * recurrence.intervalValue)
            val candidate = nthWeekday(month, pattern.ordinal, pattern.dayOfWeek)
            if (!candidate.isBefore(startDate) && candidate.isAfter(lastDoneAt)) return candidate
            step += 1
        }
    }

    private fun nthWeekday(
        month: YearMonth,
        ordinal: WeekOrdinal,
        dayOfWeek: DayOfWeek,
    ): LocalDate {
        val first = month.atDay(1).with(TemporalAdjusters.nextOrSame(dayOfWeek))
        return when (ordinal) {
            WeekOrdinal.FIRST -> first
            WeekOrdinal.SECOND -> first.plusWeeks(1)
            WeekOrdinal.THIRD -> first.plusWeeks(2)
            WeekOrdinal.FOURTH -> first.plusWeeks(3)
            WeekOrdinal.LAST -> month.atEndOfMonth().with(TemporalAdjusters.previousOrSame(dayOfWeek))
        }
    }
}
