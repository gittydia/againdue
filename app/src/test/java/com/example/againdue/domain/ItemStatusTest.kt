package com.example.againdue.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ItemStatusTest {

    private val today = LocalDate.of(2026, 11, 16)

    @Test
    fun `yesterday's due date is overdue by one day`() {
        val status = ItemStatus.from(nextDueAt = today.minusDays(1), today = today)

        assertEquals(ItemStatus.Overdue(1), status)
        assertEquals("Overdue by 1 day", status.label)
    }

    @Test
    fun `today's due date is due today`() {
        val status = ItemStatus.from(nextDueAt = today, today = today)

        assertEquals(ItemStatus.DueToday, status)
        assertEquals("Due today", status.label)
    }

    @Test
    fun `tomorrow's due date is due in one day`() {
        val status = ItemStatus.from(nextDueAt = today.plusDays(1), today = today)

        assertEquals(ItemStatus.DueIn(1), status)
        assertEquals("Due in 1 day", status.label)
    }

    @Test
    fun `multi-day overdue status uses plural label`() {
        val status = ItemStatus.from(nextDueAt = today.minusDays(3), today = today)

        assertEquals(ItemStatus.Overdue(3), status)
        assertEquals("Overdue by 3 days", status.label)
    }

    @Test
    fun `multi-day upcoming status uses plural label`() {
        val status = ItemStatus.from(nextDueAt = today.plusDays(5), today = today)

        assertEquals(ItemStatus.DueIn(5), status)
        assertEquals("Due in 5 days", status.label)
    }

    @Test
    fun `status crosses month and year boundaries correctly`() {
        val newYear = LocalDate.of(2027, 1, 1)
        val lastYear = LocalDate.of(2026, 12, 31)

        assertEquals(ItemStatus.Overdue(1), ItemStatus.from(lastYear, newYear))
        assertEquals(ItemStatus.DueIn(1), ItemStatus.from(LocalDate.of(2027, 1, 2), newYear))
    }
}
