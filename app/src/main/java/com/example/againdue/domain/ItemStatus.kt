package com.example.againdue.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * The reminder state of an item, derived from its next due date and today.
 *
 * Status is never stored; it is recalculated from dates so a stale value can
 * never become the source of truth.
 */
sealed interface ItemStatus {

    /** Human-readable status label used on the home list. */
    val label: String

    /** The item was due at least one day before today. [days] is always positive. */
    data class Overdue(val days: Long) : ItemStatus {
        init {
            require(days > 0) { "Overdue days must be positive, was $days" }
        }

        override val label: String
            get() = if (days == 1L) "Overdue by 1 day" else "Overdue by $days days"
    }

    /** The item is due exactly today. */
    data object DueToday : ItemStatus {
        override val label: String = "Due today"
    }

    /** The item is due after today. [days] is always positive. */
    data class DueIn(val days: Long) : ItemStatus {
        init {
            require(days > 0) { "Due-in days must be positive, was $days" }
        }

        override val label: String
            get() = if (days == 1L) "Due in 1 day" else "Due in $days days"
    }

    /** The item's recurrence has run its course and no longer needs action. */
    data object Ended : ItemStatus {
        override val label: String = "Ended"
    }

    companion object {
        /**
         * Derives the status of an item due at [nextDueAt] relative to [today].
         *
         * An [ended] recurrence takes precedence over date buckets: the stored
         * due date is only the last occurrence that was still allowed.
         */
        fun from(nextDueAt: LocalDate, today: LocalDate, ended: Boolean = false): ItemStatus {
            if (ended) return Ended
            val daysUntilDue = ChronoUnit.DAYS.between(today, nextDueAt)
            return when {
                daysUntilDue < 0L -> Overdue(-daysUntilDue)
                daysUntilDue == 0L -> DueToday
                else -> DueIn(daysUntilDue)
            }
        }
    }
}
