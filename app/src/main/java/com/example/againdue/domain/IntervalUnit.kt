package com.example.againdue.domain

/**
 * The unit of an item's repeat interval.
 *
 * Month is calendar-aware: it is never approximated as 30 days.
 */
enum class IntervalUnit {
    DAY,
    WEEK,
    MONTH,
}
