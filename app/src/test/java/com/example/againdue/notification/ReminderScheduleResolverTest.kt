package com.example.againdue.notification

import com.example.againdue.data.local.Item
import com.example.againdue.domain.IntervalUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ReminderScheduleResolverTest {
    private val zoneId = ZoneId.of("UTC")

    @Test
    fun itemOverrideReplacesGlobalTime() {
        val resolution = ReminderScheduleResolver.resolve(
            item = item(reminderMinuteOfDay = 20 * 60 + 15),
            settings = ReminderSettings(enabled = true, hour = 9, minute = 30),
            zoneId = zoneId,
        )

        assertEquals(
            ReminderScheduleResolution.Active(
                minuteOfDay = 20 * 60 + 15,
                dueAt = Instant.parse("2026-11-17T20:15:00Z"),
            ),
            resolution,
        )
    }

    @Test
    fun nullOverrideUsesGlobalTime() {
        val resolution = ReminderScheduleResolver.resolve(
            item = item(reminderMinuteOfDay = null),
            settings = ReminderSettings(enabled = true, hour = 9, minute = 30),
            zoneId = zoneId,
        )

        assertEquals(
            ReminderScheduleResolution.Active(
                minuteOfDay = 9 * 60 + 30,
                dueAt = Instant.parse("2026-11-17T09:30:00Z"),
            ),
            resolution,
        )
    }

    @Test
    fun globalDisableTakesPrecedenceOverItemOverride() {
        val resolution = ReminderScheduleResolver.resolve(
            item = item(reminderMinuteOfDay = 20 * 60 + 15, archived = true),
            settings = ReminderSettings(enabled = false, hour = 9, minute = 30),
            zoneId = zoneId,
        )

        assertEquals(ReminderScheduleResolution.GloballyDisabled, resolution)
    }

    @Test
    fun archivedOrEndedItemIsInactive() {
        val settings = ReminderSettings(enabled = true)

        assertEquals(
            ReminderScheduleResolution.InactiveItem,
            ReminderScheduleResolver.resolve(item(archived = true), settings, zoneId),
        )
        assertEquals(
            ReminderScheduleResolution.InactiveItem,
            ReminderScheduleResolver.resolve(item(recurrenceEnded = true), settings, zoneId),
        )
    }

    private fun item(
        reminderMinuteOfDay: Int? = null,
        archived: Boolean = false,
        recurrenceEnded: Boolean = false,
    ): Item = Item(
        id = 1,
        name = "Water plants",
        intervalValue = 1,
        intervalUnit = IntervalUnit.DAY,
        lastDoneAt = LocalDate.of(2026, 11, 16),
        nextDueAt = LocalDate.of(2026, 11, 17),
        archived = archived,
        recurrenceEnded = recurrenceEnded,
        createdAt = Instant.parse("2026-11-01T00:00:00Z"),
        updatedAt = Instant.parse("2026-11-16T00:00:00Z"),
        reminderMinuteOfDay = reminderMinuteOfDay,
    )
}
