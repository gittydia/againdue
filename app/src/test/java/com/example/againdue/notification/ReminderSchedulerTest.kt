package com.example.againdue.notification

import com.example.againdue.data.local.Item
import com.example.againdue.domain.IntervalUnit
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

class ReminderSchedulerTest {

    @Test
    fun syncItemSchedulesDueAlarmAndCancelsNotification() = runBlocking {
        val harness = harness(now = "2026-11-17T08:00:00Z")
        harness.state.snooze[ITEM_ID] = harness.now.plusSeconds(60)

        harness.scheduler.syncItem(ITEM_ID)

        assertEquals(
            Instant.parse("2026-11-17T09:00:00Z"),
            harness.alarms.scheduled[ITEM_ID],
        )
        assertTrue(harness.notifications.cancelled.contains(ITEM_ID))
        assertNull(harness.state.snooze[ITEM_ID])
    }

    @Test
    fun syncItemSchedulesItemOverrideInsteadOfGlobalTime() = runBlocking {
        val harness = harness(
            now = "2026-11-17T08:00:00Z",
            item = item(reminderMinuteOfDay = 18 * 60 + 45),
        )

        harness.scheduler.syncItem(ITEM_ID)

        assertEquals(
            Instant.parse("2026-11-17T18:45:00Z"),
            harness.alarms.scheduled[ITEM_ID],
        )
    }

    @Test
    fun syncItemForArchivedItemForgetsEverything() = runBlocking {
        val harness = harness(
            now = "2026-11-17T08:00:00Z",
            item = item(archived = true),
        )
        harness.state.snooze[ITEM_ID] = harness.now.plusSeconds(60)
        harness.state.notified[ITEM_ID] = 20_000

        harness.scheduler.syncItem(ITEM_ID)

        assertFalse(harness.alarms.scheduled.containsKey(ITEM_ID))
        assertTrue(harness.alarms.cancelled.contains(ITEM_ID))
        assertTrue(harness.notifications.cancelled.contains(ITEM_ID))
        assertNull(harness.state.snooze[ITEM_ID])
        assertNull(harness.state.notified[ITEM_ID])
    }

    @Test
    fun syncItemForEndedRecurrenceForgetsEverything() = runBlocking {
        val harness = harness(
            now = "2026-11-17T08:00:00Z",
            item = item(recurrenceEnded = true),
        )
        harness.state.snooze[ITEM_ID] = harness.now.plusSeconds(60)
        harness.state.notified[ITEM_ID] = 20_000

        harness.scheduler.syncItem(ITEM_ID)

        assertFalse(harness.alarms.scheduled.containsKey(ITEM_ID))
        assertTrue(harness.alarms.cancelled.contains(ITEM_ID))
        assertTrue(harness.notifications.cancelled.contains(ITEM_ID))
        assertNull(harness.state.snooze[ITEM_ID])
        assertNull(harness.state.notified[ITEM_ID])
    }

    @Test
    fun deliverShowsOnceAndMarksDueDate() = runBlocking {
        val harness = harness(now = "2026-11-17T10:00:00Z")

        assertTrue(harness.scheduler.deliver(ITEM_ID))
        assertEquals(listOf(harness.item), harness.notifications.shown)
        assertEquals(
            harness.item.nextDueAt.toEpochDay(),
            harness.state.notified[ITEM_ID],
        )

        assertFalse(harness.scheduler.deliver(ITEM_ID))
        assertEquals(1, harness.notifications.shown.size)
    }

    @Test
    fun deliverUsesItemOverrideInsteadOfOverdueGlobalTime() = runBlocking {
        val harness = harness(
            now = "2026-11-17T10:00:00Z",
            item = item(reminderMinuteOfDay = 18 * 60),
        )

        assertFalse(harness.scheduler.deliver(ITEM_ID))
        assertTrue(harness.notifications.shown.isEmpty())
    }

    @Test
    fun deliverDoesNotSuppressSnoozedReminder() = runBlocking {
        val harness = harness(now = "2026-11-17T10:00:00Z")
        harness.state.notified[ITEM_ID] = harness.item.nextDueAt.toEpochDay()
        harness.state.snooze[ITEM_ID] = harness.now

        assertTrue(harness.scheduler.deliver(ITEM_ID))
        assertEquals(1, harness.notifications.shown.size)
    }

    @Test
    fun deliverDoesNotShowBeforeSnoozeElapses() = runBlocking {
        val harness = harness(now = "2026-11-17T10:00:00Z")
        harness.state.notified[ITEM_ID] = harness.item.nextDueAt.toEpochDay()
        harness.state.snooze[ITEM_ID] = harness.now.plus(1, ChronoUnit.HOURS)

        assertFalse(harness.scheduler.deliver(ITEM_ID))
        assertTrue(harness.notifications.shown.isEmpty())
    }

    @Test
    fun deliverClearsExpiredSnoozeAfterShowing() = runBlocking {
        val harness = harness(now = "2026-11-17T10:00:00Z")
        harness.state.notified[ITEM_ID] = harness.item.nextDueAt.toEpochDay()
        harness.state.snooze[ITEM_ID] = harness.now.minus(2, ChronoUnit.DAYS)

        assertTrue(harness.scheduler.deliver(ITEM_ID))
        assertEquals(1, harness.notifications.shown.size)
        assertNull(harness.state.snooze[ITEM_ID])
        assertFalse(harness.scheduler.deliver(ITEM_ID))
        assertEquals(1, harness.notifications.shown.size)
    }

    @Test
    fun deliverLeavesStateUntouchedWhenNotificationRejected() = runBlocking {
        val harness = harness(now = "2026-11-17T10:00:00Z", notificationAccepted = false)

        assertFalse(harness.scheduler.deliver(ITEM_ID))
        assertNull(harness.state.notified[ITEM_ID])
    }

    @Test
    fun snoozeDelaysNotificationWithoutChangingDueDate() = runBlocking {
        val harness = harness(now = "2026-11-17T08:00:00Z")

        harness.scheduler.snooze(ITEM_ID)

        assertEquals(
            harness.now.plus(1, ChronoUnit.DAYS),
            harness.alarms.scheduled[ITEM_ID],
        )
        assertEquals(
            harness.now.plus(1, ChronoUnit.DAYS),
            harness.state.snooze[ITEM_ID],
        )
        assertTrue(harness.notifications.cancelled.contains(ITEM_ID))
        assertEquals(LocalDate.of(2026, 11, 17), harness.item.nextDueAt)
    }

    @Test
    fun syncAllPreservesActiveSnoozeAndShownNotification() = runBlocking {
        val harness = harness(now = "2026-11-17T08:00:00Z")
        val snoozedUntil = harness.now.plus(2, ChronoUnit.HOURS)
        harness.state.snooze[ITEM_ID] = snoozedUntil

        harness.scheduler.syncAll()

        assertEquals(snoozedUntil, harness.alarms.scheduled[ITEM_ID])
        assertFalse(harness.notifications.cancelled.contains(ITEM_ID))
    }

    @Test
    fun syncAllDisabledCancelsEverySchedule() = runBlocking {
        val harness = harness(
            now = "2026-11-17T08:00:00Z",
            settings = ReminderSettings(enabled = false),
        )

        harness.scheduler.syncAll()

        assertTrue(harness.alarms.cancelled.contains(ITEM_ID))
        assertTrue(harness.notifications.cancelled.contains(ITEM_ID))
        assertFalse(harness.alarms.scheduled.containsKey(ITEM_ID))
    }

    private fun harness(
        now: String,
        item: Item = item(),
        settings: ReminderSettings = ReminderSettings(enabled = true, hour = 9, minute = 0),
        notificationAccepted: Boolean = true,
    ): Harness {
        val instant = Instant.parse(now)
        val clock = Clock.fixed(instant, ZoneOffset.UTC)
        val state = FakeReminderState()
        val alarms = FakeAlarmScheduler()
        val notifications = FakeNotifications(notificationAccepted)
        return Harness(
            now = instant,
            item = item,
            state = state,
            alarms = alarms,
            notifications = notifications,
            scheduler = ReminderScheduler(
                items = FakeItemSource(item),
                settings = FakeSettings(settings),
                state = state,
                alarms = alarms,
                notifications = notifications,
                clock = clock,
                zoneId = { ZoneId.of("UTC") },
            ),
        )
    }

    private fun item(
        archived: Boolean = false,
        recurrenceEnded: Boolean = false,
        reminderMinuteOfDay: Int? = null,
    ): Item {
        val due = LocalDate.of(2026, 11, 17)
        return Item(
            id = ITEM_ID,
            name = "Water plants",
            intervalValue = 1,
            intervalUnit = IntervalUnit.DAY,
            lastDoneAt = due.minusDays(1),
            nextDueAt = due,
            archived = archived,
            recurrenceEnded = recurrenceEnded,
            reminderMinuteOfDay = reminderMinuteOfDay,
            createdAt = Instant.parse("2026-11-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-11-16T00:00:00Z"),
        )
    }

    private data class Harness(
        val now: Instant,
        val item: Item,
        val state: FakeReminderState,
        val alarms: FakeAlarmScheduler,
        val notifications: FakeNotifications,
        val scheduler: ReminderScheduler,
    )

    private class FakeItemSource(private val item: Item) : ReminderItemSource {
        override suspend fun find(id: Long): Item? = item.takeIf { it.id == id }
        override suspend fun all(): List<Item> = listOf(item)
    }

    private class FakeSettings(private var value: ReminderSettings) : ReminderSettingsSource {
        override fun current(): ReminderSettings = value
    }

    private class FakeReminderState : ReminderState {
        val snooze = mutableMapOf<Long, Instant>()
        val notified = mutableMapOf<Long, Long>()

        override fun snoozeUntil(itemId: Long): Instant? = snooze[itemId]

        override fun setSnoozeUntil(itemId: Long, instant: Instant) {
            snooze[itemId] = instant
        }

        override fun clearSnooze(itemId: Long) {
            snooze.remove(itemId)
        }

        override fun lastNotifiedDueEpochDay(itemId: Long): Long? = notified[itemId]

        override fun markNotified(itemId: Long, dueEpochDay: Long) {
            notified[itemId] = dueEpochDay
        }

        override fun clear(itemId: Long) {
            snooze.remove(itemId)
            notified.remove(itemId)
        }
    }

    private class FakeAlarmScheduler : ReminderAlarmScheduler {
        val scheduled = mutableMapOf<Long, Instant>()
        val cancelled = mutableSetOf<Long>()

        override fun schedule(itemId: Long, triggerAt: Instant) {
            scheduled[itemId] = triggerAt
        }

        override fun cancel(itemId: Long) {
            cancelled += itemId
            scheduled.remove(itemId)
        }
    }

    private class FakeNotifications(
        private val accepted: Boolean,
    ) : ReminderNotifications {
        val shown = mutableListOf<Item>()
        val cancelled = mutableSetOf<Long>()

        override fun ensureChannel() = Unit

        override fun show(item: Item): Boolean {
            if (!accepted) return false
            shown += item
            return true
        }

        override fun cancel(itemId: Long) {
            cancelled += itemId
        }
    }

    private companion object {
        const val ITEM_ID = 1L
    }
}
