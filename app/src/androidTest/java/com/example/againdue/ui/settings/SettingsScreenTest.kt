package com.example.againdue.ui.settings

import android.content.Context
import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.example.againdue.data.local.Item
import com.example.againdue.notification.ReminderAlarmScheduler
import com.example.againdue.notification.ReminderItemSource
import com.example.againdue.notification.ReminderNotifications
import com.example.againdue.notification.ReminderScheduler
import com.example.againdue.notification.ReminderSettingsStore
import com.example.againdue.notification.SharedPreferencesReminderStateStore
import com.example.againdue.ui.theme.AgainDueTheme
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class SettingsScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val preferences = context.getSharedPreferences("reminder_settings", Context.MODE_PRIVATE)
    private val clock = Clock.fixed(Instant.parse("2026-11-17T09:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        preferences.edit().clear().commit()
    }

    @After
    fun tearDown() {
        preferences.edit().clear().commit()
    }

    @Test
    fun defaults_showDisabledRemindersAtNine() {
        val store = ReminderSettingsStore(context)
        setContent(store)

        composeRule.onNodeWithTag("reminders_switch").assertIsOff()
        composeRule.onNodeWithText("09:00").assertIsDisplayed()
        composeRule.onNodeWithText("Reminder time").assertIsDisplayed()
        composeRule.onNodeWithTag("notification_status").assertIsDisplayed()
    }

    @Test
    fun switchingOff_persistsDisabledSetting() {
        val store = ReminderSettingsStore(context)
        store.setEnabled(true)
        setContent(store)

        composeRule.onNodeWithTag("reminders_switch").assertIsOn()
        composeRule.onNodeWithTag("reminders_switch").performClick()
        composeRule.waitForIdle()

        assertFalse(store.current().enabled)
        composeRule.onNodeWithTag("reminders_switch").assertIsOff()
    }

    @Test
    fun storedTime_isDisplayed() {
        val store = ReminderSettingsStore(context)
        store.setTime(21, 5)
        setContent(store)

        composeRule.onNodeWithText("21:05").assertIsDisplayed()
    }

    @Test
    fun switchingOn_whenAlreadyAllowed_keepsEnabledState() {
        val store = ReminderSettingsStore(context)
        store.setEnabled(true)
        setContent(store)

        assertTrue(store.current().enabled)
        composeRule.onNodeWithTag("reminders_switch").assertIsOn()
    }

    private fun setContent(store: ReminderSettingsStore) {
        val scheduler = ReminderScheduler(
            items = object : ReminderItemSource {
                override suspend fun find(id: Long): Item? = null
                override suspend fun all(): List<Item> = emptyList()
            },
            settings = store,
            state = SharedPreferencesReminderStateStore(context),
            alarms = object : ReminderAlarmScheduler {
                override fun schedule(itemId: Long, triggerAt: Instant) = Unit
                override fun cancel(itemId: Long) = Unit
            },
            notifications = object : ReminderNotifications {
                override fun ensureChannel() = Unit
                override fun show(item: Item): Boolean = true
                override fun cancel(itemId: Long) = Unit
            },
            clock = clock,
            zoneId = { ZoneOffset.UTC },
        )

        composeRule.setContent {
            AgainDueTheme {
                SettingsScreen(
                    viewModel = remember { SettingsViewModel(store, scheduler) },
                    onBack = {},
                )
            }
        }
    }
}
