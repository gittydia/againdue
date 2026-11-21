package com.example.againdue.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate

class NextDueCalculatorTest {

    @Test
    fun `seven days after a known date`() {
        val lastDone = LocalDate.of(2026, 11, 10)

        val nextDue = NextDueCalculator.nextDue(lastDone, 7, IntervalUnit.DAY)

        assertEquals(LocalDate.of(2026, 11, 17), nextDue)
    }

    @Test
    fun `two weeks after a known date`() {
        val lastDone = LocalDate.of(2026, 11, 10)

        val nextDue = NextDueCalculator.nextDue(lastDone, 2, IntervalUnit.WEEK)

        assertEquals(LocalDate.of(2026, 11, 24), nextDue)
    }

    @Test
    fun `one month after January 31 in a non-leap year clamps to February 28`() {
        val lastDone = LocalDate.of(2026, 1, 31)

        val nextDue = NextDueCalculator.nextDue(lastDone, 1, IntervalUnit.MONTH)

        assertEquals(LocalDate.of(2026, 2, 28), nextDue)
    }

    @Test
    fun `one month after January 31 in a leap year clamps to February 29`() {
        val lastDone = LocalDate.of(2028, 1, 31)

        val nextDue = NextDueCalculator.nextDue(lastDone, 1, IntervalUnit.MONTH)

        assertEquals(LocalDate.of(2028, 2, 29), nextDue)
    }

    @Test
    fun `multiple months from a month-end date stay calendar-aware`() {
        assertEquals(
            LocalDate.of(2026, 3, 31),
            NextDueCalculator.nextDue(LocalDate.of(2026, 1, 31), 2, IntervalUnit.MONTH),
        )
        assertEquals(
            LocalDate.of(2027, 2, 28),
            NextDueCalculator.nextDue(LocalDate.of(2026, 8, 31), 6, IntervalUnit.MONTH),
        )
    }

    @Test
    fun `early completion shifts the next due date forward from today`() {
        // Item was due on November 17 but was completed on November 15.
        val completedEarly = LocalDate.of(2026, 11, 15)

        val nextDue = NextDueCalculator.nextDue(completedEarly, 7, IntervalUnit.DAY)

        assertEquals(LocalDate.of(2026, 11, 22), nextDue)
    }

    @Test
    fun `late completion shifts the next due date forward from the actual completion date`() {
        // Item was due on November 17 but was completed on November 20.
        val completedLate = LocalDate.of(2026, 11, 20)

        val nextDue = NextDueCalculator.nextDue(completedLate, 7, IntervalUnit.DAY)

        assertEquals(LocalDate.of(2026, 11, 27), nextDue)
    }

    @Test
    fun `editing an interval recalculates from the last-completed date`() {
        val lastDone = LocalDate.of(2026, 11, 10)
        val originalDue = NextDueCalculator.nextDue(lastDone, 7, IntervalUnit.DAY)
        assertEquals(LocalDate.of(2026, 11, 17), originalDue)

        val editedDue = NextDueCalculator.nextDue(lastDone, 14, IntervalUnit.DAY)

        assertEquals(LocalDate.of(2026, 11, 24), editedDue)
    }

    @Test
    fun `zero interval is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            NextDueCalculator.nextDue(LocalDate.of(2026, 11, 10), 0, IntervalUnit.DAY)
        }
    }

    @Test
    fun `negative interval is rejected`() {
        assertThrows(IllegalArgumentException::class.java) {
            NextDueCalculator.nextDue(LocalDate.of(2026, 11, 10), -3, IntervalUnit.WEEK)
        }
    }

    @Test
    fun `legacy monthly interval remains completion based`() {
        val result = NextDueCalculator.nextRecurrence(
            lastDoneAt = LocalDate.of(2026, 2, 28),
            recurrence = RecurrenceConfig(
                intervalValue = 1,
                intervalUnit = IntervalUnit.MONTH,
                startDate = LocalDate.of(2026, 1, 31),
            ),
        )

        assertEquals(LocalDate.of(2026, 3, 28), result.nextDueAt)
        assertEquals(false, result.ended)
    }

    @Test
    fun `monthly calendar date stays anchored to start day after a short month`() {
        val recurrence = RecurrenceConfig(
            intervalValue = 1,
            intervalUnit = IntervalUnit.MONTH,
            startDate = LocalDate.of(2028, 1, 31),
            pattern = RecurrencePattern.MonthlyDate,
        )

        val february = NextDueCalculator.nextRecurrence(
            lastDoneAt = LocalDate.of(2028, 1, 31),
            recurrence = recurrence,
        )
        val march = NextDueCalculator.nextRecurrence(
            lastDoneAt = february.nextDueAt,
            recurrence = recurrence,
        )

        assertEquals(LocalDate.of(2028, 2, 29), february.nextDueAt)
        assertEquals(LocalDate.of(2028, 3, 31), march.nextDueAt)
    }

    @Test
    fun `monthly calendar date is never before a future start date`() {
        val result = NextDueCalculator.nextRecurrence(
            lastDoneAt = LocalDate.of(2026, 11, 1),
            recurrence = RecurrenceConfig(
                intervalValue = 1,
                intervalUnit = IntervalUnit.MONTH,
                startDate = LocalDate.of(2026, 11, 20),
                pattern = RecurrencePattern.MonthlyDate,
            ),
        )

        assertEquals(LocalDate.of(2026, 11, 20), result.nextDueAt)
    }

    @Test
    fun `first Monday strictly after completion skips a match before the start date`() {
        val result = NextDueCalculator.nextRecurrence(
            lastDoneAt = LocalDate.of(2026, 11, 20),
            recurrence = RecurrenceConfig(
                intervalValue = 1,
                intervalUnit = IntervalUnit.MONTH,
                startDate = LocalDate.of(2026, 11, 20),
                pattern = RecurrencePattern.MonthlyNthWeekday(
                    ordinal = WeekOrdinal.FIRST,
                    dayOfWeek = DayOfWeek.MONDAY,
                ),
            ),
        )

        assertEquals(LocalDate.of(2026, 12, 7), result.nextDueAt)
    }

    @Test
    fun `last Friday supports multi-month intervals anchored to the start month`() {
        val result = NextDueCalculator.nextRecurrence(
            lastDoneAt = LocalDate.of(2026, 11, 28),
            recurrence = RecurrenceConfig(
                intervalValue = 2,
                intervalUnit = IntervalUnit.MONTH,
                startDate = LocalDate.of(2026, 11, 20),
                pattern = RecurrencePattern.MonthlyNthWeekday(
                    ordinal = WeekOrdinal.LAST,
                    dayOfWeek = DayOfWeek.FRIDAY,
                ),
            ),
        )

        assertEquals(LocalDate.of(2027, 1, 29), result.nextDueAt)
    }

    @Test
    fun `fourth Sunday is calculated within the matching month`() {
        val result = NextDueCalculator.nextRecurrence(
            lastDoneAt = LocalDate.of(2026, 10, 31),
            recurrence = RecurrenceConfig(
                intervalValue = 1,
                intervalUnit = IntervalUnit.MONTH,
                startDate = LocalDate.of(2026, 10, 1),
                pattern = RecurrencePattern.MonthlyNthWeekday(
                    ordinal = WeekOrdinal.FOURTH,
                    dayOfWeek = DayOfWeek.SUNDAY,
                ),
            ),
        )

        assertEquals(LocalDate.of(2026, 11, 22), result.nextDueAt)
    }

    @Test
    fun `end date allows its matching occurrence and ends afterward`() {
        val recurrence = RecurrenceConfig(
            intervalValue = 1,
            intervalUnit = IntervalUnit.MONTH,
            startDate = LocalDate.of(2026, 11, 20),
            pattern = RecurrencePattern.MonthlyDate,
            endDate = LocalDate.of(2026, 12, 20),
        )

        val finalOccurrence = NextDueCalculator.nextRecurrence(
            lastDoneAt = LocalDate.of(2026, 11, 20),
            recurrence = recurrence,
        )
        val ended = NextDueCalculator.nextRecurrence(
            lastDoneAt = finalOccurrence.nextDueAt,
            recurrence = recurrence,
        )

        assertEquals(LocalDate.of(2026, 12, 20), finalOccurrence.nextDueAt)
        assertEquals(false, finalOccurrence.ended)
        assertEquals(LocalDate.of(2027, 1, 20), ended.nextDueAt)
        assertEquals(true, ended.ended)
    }
}
