package com.example.againdue.domain

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class ItemSorterTest {

    private val today = LocalDate.of(2026, 11, 16)

    @Test
    fun `most overdue item sorts first`() {
        val twoDaysOverdue = ItemSummary(1, "Two days overdue", today.minusDays(2))
        val oneDayOverdue = ItemSummary(2, "One day overdue", today.minusDays(1))
        val upcoming = ItemSummary(3, "Upcoming", today.plusDays(3))

        val sorted = ItemSorter.sortActive(listOf(upcoming, oneDayOverdue, twoDaysOverdue))

        assertEquals(
            listOf("Two days overdue", "One day overdue", "Upcoming"),
            sorted.map { it.name },
        )
    }

    @Test
    fun `upcoming items sort in ascending due-date order`() {
        val inTenDays = ItemSummary(1, "Ten", today.plusDays(10))
        val inOneDay = ItemSummary(2, "One", today.plusDays(1))
        val inFiveDays = ItemSummary(3, "Five", today.plusDays(5))

        val sorted = ItemSorter.sortActive(listOf(inTenDays, inOneDay, inFiveDays))

        assertEquals(listOf("One", "Five", "Ten"), sorted.map { it.name })
    }

    @Test
    fun `due today sits between overdue and upcoming items`() {
        val overdue = ItemSummary(1, "Overdue", today.minusDays(1))
        val dueToday = ItemSummary(2, "Due today", today)
        val upcoming = ItemSummary(3, "Upcoming", today.plusDays(1))

        val sorted = ItemSorter.sortActive(listOf(upcoming, dueToday, overdue))

        assertEquals(listOf("Overdue", "Due today", "Upcoming"), sorted.map { it.name })
    }

    @Test
    fun `archived items do not appear in the active list`() {
        val archived = ItemSummary(1, "Archived", today.minusDays(5), archived = true)
        val active = ItemSummary(2, "Active", today.plusDays(2))

        val sorted = ItemSorter.sortActive(listOf(archived, active))

        assertEquals(listOf("Active"), sorted.map { it.name })
    }

    @Test
    fun `equal due dates fall back to case-insensitive name order`() {
        val banana = ItemSummary(1, "banana", today.plusDays(2))
        val apple = ItemSummary(2, "Apple", today.plusDays(2))

        val sorted = ItemSorter.sortActive(listOf(banana, apple))

        assertEquals(listOf("Apple", "banana"), sorted.map { it.name })
    }

    @Test
    fun `identical names keep a stable order by id`() {
        val second = ItemSummary(2, "Same", today.plusDays(2))
        val first = ItemSummary(1, "Same", today.plusDays(2))

        val sorted = ItemSorter.sortActive(listOf(second, first))

        assertEquals(listOf(1L, 2L), sorted.map { it.id })
    }

    @Test
    fun `empty list stays empty`() {
        assertEquals(emptyList<ItemSummary>(), ItemSorter.sortActive(emptyList()))
    }
}
