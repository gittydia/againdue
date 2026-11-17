package com.example.againdue

import android.content.Context
import com.example.againdue.data.local.AppDatabase
import com.example.againdue.data.repository.ItemRepository
import com.example.againdue.notification.AndroidReminderAlarmScheduler
import com.example.againdue.notification.AndroidReminderNotifications
import com.example.againdue.notification.ReminderScheduler
import com.example.againdue.notification.ReminderSettingsStore
import com.example.againdue.notification.RoomReminderItemSource
import com.example.againdue.notification.SharedPreferencesReminderStateStore
import java.time.Clock

open class AppContainer(context: Context) {

    private val appContext = context.applicationContext
    open val clock: Clock = Clock.systemDefaultZone()

    open val database: AppDatabase by lazy { AppDatabase.getInstance(appContext) }

    open val reminderSettings: ReminderSettingsStore by lazy {
        ReminderSettingsStore(appContext)
    }

    open val reminderState: SharedPreferencesReminderStateStore by lazy {
        SharedPreferencesReminderStateStore(appContext)
    }

    open val reminderNotifications: AndroidReminderNotifications by lazy {
        AndroidReminderNotifications(appContext)
    }

    open val reminderScheduler: ReminderScheduler by lazy {
        ReminderScheduler(
            items = RoomReminderItemSource(database),
            settings = reminderSettings,
            state = reminderState,
            alarms = AndroidReminderAlarmScheduler(appContext),
            notifications = reminderNotifications,
            clock = clock,
        )
    }

    open val itemRepository: ItemRepository by lazy {
        ItemRepository(database, clock, reminderScheduler)
    }
}
