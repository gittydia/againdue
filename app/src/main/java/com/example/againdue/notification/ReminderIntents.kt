package com.example.againdue.notification

import android.net.Uri

/** Intent contract shared by alarms, notifications, receivers, and deep links. */
object ReminderIntents {

    const val ACTION_REMIND = "com.example.againdue.action.REMIND"
    const val ACTION_DONE = "com.example.againdue.action.DONE"
    const val ACTION_SNOOZE = "com.example.againdue.action.SNOOZE"

    const val EXTRA_ITEM_ID = "com.example.againdue.extra.ITEM_ID"

    fun alarmUri(itemId: Long): Uri = Uri.parse("againdue://reminder/$itemId")

    fun actionUri(action: String, itemId: Long): Uri =
        Uri.parse("againdue://action/$action/$itemId")

    fun itemUri(itemId: Long): Uri = Uri.parse("againdue://item/$itemId")
}
