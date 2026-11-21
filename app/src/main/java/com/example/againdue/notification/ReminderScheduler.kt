package com.example.againdue.notification

import com.example.againdue.data.local.AppDatabase
import com.example.againdue.data.local.Item
import com.example.againdue.domain.ReminderSynchronizer
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/** Data access needed for reminder reconciliation. */
interface ReminderItemSource {
    suspend fun find(id: Long): Item?
    suspend fun all(): List<Item>
}

/** Room-backed item source used by the production scheduler. */
class RoomReminderItemSource(private val database: AppDatabase) : ReminderItemSource {
    override suspend fun find(id: Long): Item? = database.itemDao().getById(id)
    override suspend fun all(): List<Item> = database.itemDao().getAll()
}

/**
 * Single coordinator for alarms, notifications, and their persisted state.
 *
 * Every item mutation funnels through [syncItem]; startup, boot, and timezone
 * changes use [syncAll] to rebuild the schedule from the database.
 */
class ReminderScheduler(
    private val items: ReminderItemSource,
    private val settings: ReminderSettingsSource,
    private val state: ReminderState,
    private val alarms: ReminderAlarmScheduler,
    private val notifications: ReminderNotifications,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val zoneId: () -> ZoneId = { ZoneId.systemDefault() },
) : ReminderSynchronizer {

    override suspend fun syncItem(itemId: Long) {
        state.clearSnooze(itemId)
        val item = items.find(itemId)
        if (item == null) {
            forget(itemId)
            return
        }
        schedule(item, settings.current(), Instant.now(clock), cancelNotification = true)
    }

    override suspend fun onItemDeleted(itemId: Long) {
        forget(itemId)
    }

    /** Rebuilds every item's alarm, preserving active snoozes and shown notifications. */
    suspend fun syncAll() {
        val now = Instant.now(clock)
        val current = settings.current()
        val allItems = items.all()
        if (!current.enabled) {
            allItems.forEach { item ->
                alarms.cancel(item.id)
                notifications.cancel(item.id)
            }
            return
        }
        allItems.forEach { item ->
            schedule(item, current, now, cancelNotification = false)
        }
    }

    /** Shows a due notification once. Returns false when it was not delivered. */
    suspend fun deliver(itemId: Long): Boolean {
        val now = Instant.now(clock)
        val item = items.find(itemId)
        if (item == null) {
            forget(itemId)
            return false
        }
        val current = settings.current()
        if (!current.enabled || item.archived || item.recurrenceEnded) {
            alarms.cancel(itemId)
            notifications.cancel(itemId)
            return false
        }
        val dueAt = ReminderScheduleCalculator.dueAt(
            nextDueAt = item.nextDueAt,
            hour = current.hour,
            minute = current.minute,
            zoneId = zoneId(),
        )
        if (dueAt.isAfter(now.plusSeconds(60))) return false
        val snoozeUntil = state.snoozeUntil(itemId)
        if (snoozeUntil?.isAfter(now) == true) return false
        val hasActiveSnooze = snoozeUntil != null
        val alreadyNotified =
            state.lastNotifiedDueEpochDay(itemId) == item.nextDueAt.toEpochDay()
        if (alreadyNotified && !hasActiveSnooze) return false
        if (!notifications.show(item)) return false

        state.clearSnooze(itemId)
        state.markNotified(itemId, item.nextDueAt.toEpochDay())
        return true
    }

    /** Delays only the notification; the item's due date is unchanged. */
    suspend fun snooze(itemId: Long, days: Long = 1L) {
        require(days > 0) { "Snooze must move forward" }
        val item = items.find(itemId)
        if (item == null) {
            forget(itemId)
            return
        }
        val current = settings.current()
        if (!current.enabled || item.archived || item.recurrenceEnded) {
            alarms.cancel(itemId)
            notifications.cancel(itemId)
            return
        }

        val until = Instant.now(clock).plus(days, ChronoUnit.DAYS)
        state.setSnoozeUntil(itemId, until)
        notifications.cancel(itemId)
        alarms.schedule(itemId, until)
    }

    private fun schedule(
        item: Item,
        current: ReminderSettings,
        now: Instant,
        cancelNotification: Boolean,
    ) {
        if (!current.enabled) {
            alarms.cancel(item.id)
            notifications.cancel(item.id)
            return
        }
        if (item.archived || item.recurrenceEnded) {
            forget(item.id)
            return
        }

        val dueAt = ReminderScheduleCalculator.dueAt(
            nextDueAt = item.nextDueAt,
            hour = current.hour,
            minute = current.minute,
            zoneId = zoneId(),
        )
        val storedSnooze = state.snoozeUntil(item.id)
        val activeSnooze = storedSnooze?.takeIf { it.isAfter(now) }
        if (storedSnooze != null && activeSnooze == null) {
            state.clearSnooze(item.id)
        }
        val plan = ReminderScheduleCalculator.plan(
            now = now,
            dueAt = dueAt,
            snoozedUntil = activeSnooze,
            alreadyNotifiedForDue =
                state.lastNotifiedDueEpochDay(item.id) == item.nextDueAt.toEpochDay(),
        )

        if (cancelNotification) notifications.cancel(item.id)
        when (plan) {
            is ReminderPlan.Schedule -> alarms.schedule(item.id, plan.at)
            ReminderPlan.Cancel -> alarms.cancel(item.id)
        }
    }

    private fun forget(itemId: Long) {
        alarms.cancel(itemId)
        notifications.cancel(itemId)
        state.clear(itemId)
    }
}
