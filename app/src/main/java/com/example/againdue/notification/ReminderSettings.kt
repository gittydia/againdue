package com.example.againdue.notification

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Global reminder preferences shared by every item. */
data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = 9,
    val minute: Int = 0,
)

/** Read access used by scheduling code and unit tests. */
interface ReminderSettingsSource {
    fun current(): ReminderSettings
}

/**
 * Persists reminder preferences synchronously so broadcast receivers can read
 * the latest values without waiting for a coroutine-backed data store.
 */
class ReminderSettingsStore(context: Context) : ReminderSettingsSource {

    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val mutableSettings = MutableStateFlow(read())

    /** Observable settings for the Settings screen. */
    val settings: StateFlow<ReminderSettings> = mutableSettings.asStateFlow()

    override fun current(): ReminderSettings = mutableSettings.value

    fun setEnabled(enabled: Boolean) {
        if (mutableSettings.value.enabled == enabled) return
        preferences.edit().putBoolean(KEY_ENABLED, enabled).apply()
        mutableSettings.value = mutableSettings.value.copy(enabled = enabled)
    }

    fun setTime(hour: Int, minute: Int) {
        require(hour in 0..23) { "Hour must be between 0 and 23" }
        require(minute in 0..59) { "Minute must be between 0 and 59" }
        if (mutableSettings.value.hour == hour && mutableSettings.value.minute == minute) return
        preferences.edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()
        mutableSettings.value = mutableSettings.value.copy(hour = hour, minute = minute)
    }

    private fun read(): ReminderSettings = ReminderSettings(
        enabled = preferences.getBoolean(KEY_ENABLED, false),
        hour = preferences.getInt(KEY_HOUR, 9).coerceIn(0, 23),
        minute = preferences.getInt(KEY_MINUTE, 0).coerceIn(0, 59),
    )

    private companion object {
        const val PREFERENCES_NAME = "reminder_settings"
        const val KEY_ENABLED = "enabled"
        const val KEY_HOUR = "hour"
        const val KEY_MINUTE = "minute"
    }
}
