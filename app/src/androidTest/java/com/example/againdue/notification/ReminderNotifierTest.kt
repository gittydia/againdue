package com.example.againdue.notification

import android.app.NotificationManager
import android.content.Context
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.againdue.data.local.Item
import com.example.againdue.domain.IntervalUnit
import java.time.Instant
import java.time.LocalDate
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeFalse
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReminderNotifierTest {

    private val context: Context = ApplicationProvider.getApplicationContext()
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val notificationManagerCompat = NotificationManagerCompat.from(context)
    private val notifier = AndroidReminderNotifications(context)

    @Before
    fun setUp() {
        notifier.cancel(ITEM_ID)
        waitForNotification(active = false)
        notifier.ensureChannel()
        assertNotNull(notificationManager.getNotificationChannel("reminders"))
    }

    @After
    fun tearDown() {
        notifier.cancel(ITEM_ID)
        waitForNotification(active = false)
    }

    @Test
    fun show_whenNotificationsDisabled_returnsFalseAndDoesNotPost() {
        assumeFalse(notificationManagerCompat.areNotificationsEnabled())

        assertFalse(notifier.show(ITEM))
        assertFalse(hasActiveNotification())
    }

    @Test
    fun show_whenNotificationsEnabled_postsNotificationThenCancelsIt() {
        assumeTrue(notificationManagerCompat.areNotificationsEnabled())

        assertTrue(notifier.show(ITEM))
        assertTrue(waitForNotification(active = true))

        notifier.cancel(ITEM_ID)
        assertTrue(waitForNotification(active = false))
    }

    private fun waitForNotification(active: Boolean): Boolean {
        val deadline = SystemClock.elapsedRealtime() + NOTIFICATION_WAIT_MILLIS
        while (SystemClock.elapsedRealtime() < deadline) {
            if (hasActiveNotification() == active) return true
            SystemClock.sleep(POLL_INTERVAL_MILLIS)
        }
        return hasActiveNotification() == active
    }

    private fun hasActiveNotification(): Boolean =
        notificationManager.activeNotifications.any { it.id == NOTIFICATION_ID }

    private companion object {
        const val ITEM_ID = 73_021_117L
        const val NOTIFICATION_ID = ITEM_ID.toInt()
        const val NOTIFICATION_WAIT_MILLIS = 2_000L
        const val POLL_INTERVAL_MILLIS = 50L

        val ITEM = Item(
            id = ITEM_ID,
            name = "Instrumentation reminder",
            intervalValue = 1,
            intervalUnit = IntervalUnit.DAY,
            lastDoneAt = LocalDate.of(2026, 11, 16),
            nextDueAt = LocalDate.of(2026, 11, 17),
            createdAt = Instant.parse("2026-11-16T09:00:00Z"),
            updatedAt = Instant.parse("2026-11-16T09:00:00Z"),
        )
    }
}
