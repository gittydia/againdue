package com.example.againdue.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.againdue.AgainDueApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ReminderActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        if (action != ReminderIntents.ACTION_DONE && action != ReminderIntents.ACTION_SNOOZE) {
            return
        }
        val itemId = intent.getLongExtra(ReminderIntents.EXTRA_ITEM_ID, -1L)
        if (itemId <= 0L) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (context.applicationContext as AgainDueApplication).container
                when (action) {
                    ReminderIntents.ACTION_DONE -> {
                        val completion = container.itemRepository.completeItem(itemId)
                        if (completion == null) {
                            container.reminderScheduler.syncItem(itemId)
                        } else {
                            container.reminderNotifications.cancel(itemId)
                        }
                    }

                    ReminderIntents.ACTION_SNOOZE -> container.reminderScheduler.snooze(itemId)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
