package com.example.againdue.notification

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import androidx.core.app.NotificationManagerCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.againdue.AgainDueApplication
import com.example.againdue.R
import com.example.againdue.data.local.Item
import com.example.againdue.domain.IntervalUnit
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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

    private val application: AgainDueApplication = ApplicationProvider.getApplicationContext()
    private val context: Context = application
    private val container = application.container
    private val notificationManager = context.getSystemService(NotificationManager::class.java)
    private val notificationManagerCompat = NotificationManagerCompat.from(context)
    private val notifier = AndroidReminderNotifications(context)
    private val createdItemIds = mutableSetOf<Long>()

    @Before
    fun setUp() {
        notifier.cancel(ITEM_ID)
        waitForNotification(active = false)
        notifier.ensureChannel()
        assertNotNull(notificationManager.getNotificationChannel("reminders"))
    }

    @After
    fun tearDown() {
        runBlocking {
            createdItemIds.forEach { container.itemRepository.deleteItem(it) }
            createdItemIds.clear()
        }
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
    fun show_whenNotificationsEnabled_postsDoneAndSnoozeActionsThenCancelsIt() {
        assumeTrue(notificationManagerCompat.areNotificationsEnabled())

        assertTrue(notifier.show(ITEM))
        assertTrue(waitForNotification(active = true))

        val actions = notificationManager.activeNotifications
            .single { it.id == NOTIFICATION_ID }
            .notification
            .actions
        assertEquals(
            listOf(
                context.getString(R.string.reminder_done),
                context.getString(R.string.reminder_snooze),
            ),
            actions.map { it.title.toString() },
        )
        actions.forEach { action ->
            assertEquals(context.packageName, action.actionIntent.creatorPackage)
            assertEquals(context.applicationInfo.uid, action.actionIntent.creatorUid)
            assertTrue(action.actionIntent.isImmutable)
        }

        notifier.cancel(ITEM_ID)
        assertTrue(waitForNotification(active = false))
    }

    @Test
    fun doneBroadcast_advancesTargetItemAndRecordsOneCompletion() = runBlocking {
        val itemId = container.itemRepository.createItem(
            name = "Receiver integration ${UUID.randomUUID()}",
            intervalValue = 1,
            intervalUnit = IntervalUnit.DAY,
            lastDoneAt = LocalDate.of(2000, 1, 1),
        )
        createdItemIds += itemId
        val original = container.itemRepository.getItem(itemId)!!

        context.sendBroadcast(
            Intent(context, ReminderActionReceiver::class.java).apply {
                action = ReminderIntents.ACTION_DONE
                data = ReminderIntents.actionUri("done", itemId)
                putExtra(ReminderIntents.EXTRA_ITEM_ID, itemId)
            },
        )

        assertTrue(waitForCompletion(itemId, original.lastDoneAt))
        val updated = container.itemRepository.getItem(itemId)!!
        assertTrue(updated.lastDoneAt.isAfter(original.lastDoneAt))
        assertTrue(updated.nextDueAt.isAfter(original.nextDueAt))
        assertEquals(1, updated.completedCount)
        assertEquals(1, container.database.completionDao().countForItem(itemId))
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

    private suspend fun waitForCompletion(itemId: Long, previousLastDoneAt: LocalDate): Boolean {
        val deadline = SystemClock.elapsedRealtime() + RECEIVER_WAIT_MILLIS
        while (SystemClock.elapsedRealtime() < deadline) {
            val itemAdvanced =
                container.itemRepository.getItem(itemId)?.lastDoneAt != previousLastDoneAt
            val completionRecorded = container.database.completionDao().countForItem(itemId) > 0
            if (itemAdvanced && completionRecorded) return true
            SystemClock.sleep(POLL_INTERVAL_MILLIS)
        }
        val itemAdvanced =
            container.itemRepository.getItem(itemId)?.lastDoneAt != previousLastDoneAt
        val completionRecorded = container.database.completionDao().countForItem(itemId) > 0
        return itemAdvanced && completionRecorded
    }

    private companion object {
        const val ITEM_ID = 73_021_117L
        const val NOTIFICATION_ID = ITEM_ID.toInt()
        const val NOTIFICATION_WAIT_MILLIS = 2_000L
        const val RECEIVER_WAIT_MILLIS = 5_000L
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
