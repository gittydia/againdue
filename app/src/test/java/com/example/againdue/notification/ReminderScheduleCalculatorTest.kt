package com.example.againdue.notification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ReminderScheduleCalculatorTest {

    private val zone: ZoneId = ZoneId.of("Asia/Manila")
    private val now = Instant.parse("2026-11-17T00:00:00Z")

    @Test
    fun dueAtUsesLocalReminderTimeOnDueDate() {
        val dueAt = ReminderScheduleCalculator.dueAt(
            nextDueAt = LocalDate.of(2026, 11, 18),
            hour = 21,
            minute = 30,
            zoneId = zone,
        )

        assertEquals(Instant.parse("2026-11-18T13:30:00Z"), dueAt)
    }

    @Test
    fun dueAtRejectsInvalidTime() {
        assertThrows(IllegalArgumentException::class.java) {
            ReminderScheduleCalculator.dueAt(LocalDate.now(), 24, 0, zone)
        }
        assertThrows(IllegalArgumentException::class.java) {
            ReminderScheduleCalculator.dueAt(LocalDate.now(), 0, 60, zone)
        }
    }

    @Test
    fun futureDueSchedulesNormalReminder() {
        val dueAt = now.plus(Duration.ofHours(4))

        assertEquals(
            ReminderPlan.Schedule(dueAt),
            ReminderScheduleCalculator.plan(now, dueAt, null, alreadyNotifiedForDue = false),
        )
    }

    @Test
    fun activeSnoozeWinsOverFutureDue() {
        val dueAt = now.plus(Duration.ofHours(4))
        val snoozedUntil = now.plus(Duration.ofHours(1))

        assertEquals(
            ReminderPlan.Schedule(snoozedUntil),
            ReminderScheduleCalculator.plan(now, dueAt, snoozedUntil, alreadyNotifiedForDue = true),
        )
    }

    @Test
    fun overdueItemSchedulesImmediateRetryOnce() {
        val dueAt = now.minus(Duration.ofHours(1))

        assertEquals(
            ReminderPlan.Schedule(now.plusSeconds(5)),
            ReminderScheduleCalculator.plan(now, dueAt, null, alreadyNotifiedForDue = false),
        )
        assertEquals(
            ReminderPlan.Cancel,
            ReminderScheduleCalculator.plan(now, dueAt, null, alreadyNotifiedForDue = true),
        )
    }

    @Test
    fun expiredSnoozeDoesNotOverrideOverduePlan() {
        val dueAt = now.minus(Duration.ofHours(1))
        val expiredSnooze = now.minus(Duration.ofMinutes(1))

        assertEquals(
            ReminderPlan.Schedule(now.plusSeconds(5)),
            ReminderScheduleCalculator.plan(now, dueAt, expiredSnooze, alreadyNotifiedForDue = false),
        )
    }
}
