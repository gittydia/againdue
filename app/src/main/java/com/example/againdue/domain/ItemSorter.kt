package com.example.againdue.domain

import java.util.Comparator

/**
 * Orders the active home list by urgency.
 *
 * Ordering is ascending by due date, which naturally puts the most overdue item
 * first, followed by items due today and then upcoming items. Ties fall back to
 * name (case-insensitive) so the list does not jump between recompositions.
 */
object ItemSorter {

    private val urgencyOrder: Comparator<ItemSummary> =
        compareBy<ItemSummary> { it.nextDueAt }
            .thenBy(String.CASE_INSENSITIVE_ORDER) { it.name }
            .thenBy { it.id }

    /** Returns every supplied item sorted by due date and then name. */
    fun sortByUrgency(items: List<ItemSummary>): List<ItemSummary> =
        items.sortedWith(urgencyOrder)

    /** Returns the non-archived items sorted by due date and then name. */
    fun sortActive(items: List<ItemSummary>): List<ItemSummary> =
        sortByUrgency(items.filterNot { it.archived })
}
