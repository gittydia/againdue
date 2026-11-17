package com.example.againdue.domain

import java.time.LocalDate

/**
 * Derives an item's next due date from when it was actually completed.
 *
 * The calculation is always anchored to the real completion date, so early and
 * late completions shift the following due date. Months use calendar arithmetic
 * (for example, January 31 plus one month is February 28 or 29), never a
 * 30-day approximation.
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
}
