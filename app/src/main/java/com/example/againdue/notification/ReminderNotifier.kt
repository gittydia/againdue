package com.example.againdue.notification

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.againdue.MainActivity
import com.example.againdue.R
import com.example.againdue.data.local.Item
import java.time.format.DateTimeFormatter

/** Shows and cancels due-item notifications. */
interface ReminderNotifications {
    fun ensureChannel()

    /** Returns false when the system would not accept the notification. */
    fun show(item: Item): Boolean

    fun cancel(itemId: Long)
}

/** NotificationCompat-based implementation with Done and Snooze actions. */
class AndroidReminderNotifications(context: Context) : ReminderNotifications {

    private val appContext = context.applicationContext
    private val notificationManager = NotificationManagerCompat.from(appContext)

    override fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = appContext.getString(R.string.reminder_channel_description)
        }
        appContext.getSystemService(NotificationManager::class.java)
            .createNotificationChannel(channel)
    }

    @SuppressLint("MissingPermission")
    override fun show(item: Item): Boolean {
        if (!notificationManager.areNotificationsEnabled()) return false

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(item.name)
            .setContentText(dueText(item))
            .setStyle(NotificationCompat.BigTextStyle().bigText(dueText(item)))
            .setContentIntent(contentIntent(item.id))
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .addAction(
                0,
                appContext.getString(R.string.reminder_done),
                actionIntent(ReminderIntents.ACTION_DONE, "done", item.id),
            )
            .addAction(
                0,
                appContext.getString(R.string.reminder_snooze),
                actionIntent(ReminderIntents.ACTION_SNOOZE, "snooze", item.id),
            )
            .build()

        notificationManager.notify(notificationId(item.id), notification)
        return true
    }

    override fun cancel(itemId: Long) {
        notificationManager.cancel(notificationId(itemId))
    }

    private fun contentIntent(itemId: Long): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = ReminderIntents.itemUri(itemId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            appContext,
            requestCode(itemId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun actionIntent(action: String, path: String, itemId: Long): PendingIntent {
        val intent = Intent(appContext, ReminderActionReceiver::class.java).apply {
            this.action = action
            data = ReminderIntents.actionUri(path, itemId)
            putExtra(ReminderIntents.EXTRA_ITEM_ID, itemId)
        }
        return PendingIntent.getBroadcast(
            appContext,
            requestCode(itemId),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    private fun dueText(item: Item): String {
        val date = item.nextDueAt.format(DATE_FORMAT)
        return "Due $date"
    }

    private fun notificationId(itemId: Long): Int = itemId.toInt()

    private fun requestCode(itemId: Long): Int = itemId.toInt()

    private companion object {
        const val CHANNEL_ID = "reminders"
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
    }
}
