package com.example.againdue.notification

import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** The next action for one item's alarm. */
sealed interface ReminderPlan {
    data class Schedule(val at: Instant) : ReminderPlan
    data object Cancel : ReminderPlan
}

/** Pure time calculations shared by the scheduler and its tests. */
object ReminderScheduleCalculator {

    /** Delay used when an overdue item has not been notified for its due date. */
    val OVERDUE_DELAY: Duration = Duration.ofSeconds(5)

    fun dueAt(
        nextDueAt: LocalDate,
        minuteOfDay: Int,
        zoneId: ZoneId,
    ): Instant {
        require(minuteOfDay in 0..1439) { "Minute of day must be between 0 and 1439" }
        return nextDueAt
            .atStartOfDay()
            .plusMinutes(minuteOfDay.toLong())
            .atZone(zoneId)
            .toInstant()
    }

    /**
     * Chooses between a future snooze, the normal due alarm, an immediate
     * overdue retry, and no alarm when this due date was already delivered.
     */
    fun plan(
        now: Instant,
        dueAt: Instant,
        snoozedUntil: Instant?,
        alreadyNotifiedForDue: Boolean,
        overdueDelay: Duration = OVERDUE_DELAY,
    ): ReminderPlan {
        if (snoozedUntil != null && snoozedUntil.isAfter(now)) {
            return ReminderPlan.Schedule(snoozedUntil)
        }
        if (dueAt.isAfter(now)) {
            return ReminderPlan.Schedule(dueAt)
        }
        if (alreadyNotifiedForDue) {
            return ReminderPlan.Cancel
        }
        return ReminderPlan.Schedule(now.plus(overdueDelay))
    }
}
