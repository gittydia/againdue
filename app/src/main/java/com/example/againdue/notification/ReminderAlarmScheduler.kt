package com.example.againdue.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import java.time.Instant

/** Owns the single alarm slot for each item. */
interface ReminderAlarmScheduler {
    fun schedule(itemId: Long, triggerAt: Instant)
    fun cancel(itemId: Long)
}

/**
 * Inexact alarm implementation.
 *
 * The app never asks for exact-alarm access; [AlarmManager.setAndAllowWhileIdle]
 * keeps reminders working in Doze while respecting the platform's inexact
 * delivery policy.
 */
class AndroidReminderAlarmScheduler(context: Context) : ReminderAlarmScheduler {

    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(AlarmManager::class.java)

    override fun schedule(itemId: Long, triggerAt: Instant) {
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAt.toEpochMilli(),
            createPendingIntent(itemId),
        )
    }

    override fun cancel(itemId: Long) {
        val pendingIntent = existingPendingIntent(itemId) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun createPendingIntent(itemId: Long): PendingIntent =
        PendingIntent.getBroadcast(
            appContext,
            requestCode(itemId),
            alarmIntent(itemId),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun existingPendingIntent(itemId: Long): PendingIntent? =
        PendingIntent.getBroadcast(
            appContext,
            requestCode(itemId),
            alarmIntent(itemId),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun alarmIntent(itemId: Long): Intent =
        Intent(appContext, ReminderAlarmReceiver::class.java).apply {
            action = ReminderIntents.ACTION_REMIND
            data = ReminderIntents.alarmUri(itemId)
            putExtra(ReminderIntents.EXTRA_ITEM_ID, itemId)
        }

    private fun requestCode(itemId: Long): Int = itemId.toInt()
}
