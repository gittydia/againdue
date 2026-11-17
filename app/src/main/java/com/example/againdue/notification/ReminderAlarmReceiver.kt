package com.example.againdue.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.againdue.AgainDueApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ReminderIntents.ACTION_REMIND) return
        val itemId = intent.getLongExtra(ReminderIntents.EXTRA_ITEM_ID, -1L)
        if (itemId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as AgainDueApplication).container
                container.reminderScheduler.deliver(itemId)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
