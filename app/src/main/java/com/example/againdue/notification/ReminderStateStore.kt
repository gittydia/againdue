package com.example.againdue.notification

import android.content.Context
import java.time.Instant

/** Per-item transient reminder state used for snoozing and overdue dedupe. */
interface ReminderState {

    fun snoozeUntil(itemId: Long): Instant?

    fun setSnoozeUntil(itemId: Long, instant: Instant)

    fun clearSnooze(itemId: Long)

    fun lastNotifiedDueEpochDay(itemId: Long): Long?

    fun markNotified(itemId: Long, dueEpochDay: Long)

    fun clear(itemId: Long)
}

/**
 * SharedPreferences-backed reminder state.
 *
 * This intentionally avoids a Room schema change: both values are local,
 * replaceable delivery state rather than user-owned item data.
 */
class SharedPreferencesReminderStateStore(context: Context) : ReminderState {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )

    override fun snoozeUntil(itemId: Long): Instant? {
        val key = snoozeKey(itemId)
        if (!preferences.contains(key)) return null
        return Instant.ofEpochMilli(preferences.getLong(key, 0L))
    }

    override fun setSnoozeUntil(itemId: Long, instant: Instant) {
        preferences.edit().putLong(snoozeKey(itemId), instant.toEpochMilli()).apply()
    }

    override fun clearSnooze(itemId: Long) {
        preferences.edit().remove(snoozeKey(itemId)).apply()
    }

    override fun lastNotifiedDueEpochDay(itemId: Long): Long? {
        val key = notifiedKey(itemId)
        if (!preferences.contains(key)) return null
        return preferences.getLong(key, 0L)
    }

    override fun markNotified(itemId: Long, dueEpochDay: Long) {
        preferences.edit().putLong(notifiedKey(itemId), dueEpochDay).apply()
    }

    override fun clear(itemId: Long) {
        preferences.edit()
            .remove(snoozeKey(itemId))
            .remove(notifiedKey(itemId))
            .apply()
    }

    private fun snoozeKey(itemId: Long): String = "$SNOOZE_PREFIX$itemId"

    private fun notifiedKey(itemId: Long): String = "$NOTIFIED_PREFIX$itemId"

    private companion object {
        const val PREFERENCES_NAME = "reminder_state"
        const val SNOOZE_PREFIX = "snooze_until_"
        const val NOTIFIED_PREFIX = "last_notified_due_"
    }
}
