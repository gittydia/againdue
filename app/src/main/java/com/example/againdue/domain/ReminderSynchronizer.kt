package com.example.againdue.domain

/**
 * Applies reminder side effects after item persistence has committed.
 *
 * The repository owns item state; this abstraction owns alarms and
 * notifications, keeping the data layer free of Android scheduling details.
 */
interface ReminderSynchronizer {

    /** Reconciles the reminder for one item after a create, update, or restore. */
    suspend fun syncItem(itemId: Long)

    /** Rebuilds an item's reminder after changing the date it was last completed. */
    suspend fun onItemScheduleReset(itemId: Long)

    /** Cancels every reminder artifact for a deleted item. */
    suspend fun onItemDeleted(itemId: Long)

    companion object {
        /** Default for tests and callers that do not need reminder side effects. */
        val None: ReminderSynchronizer = object : ReminderSynchronizer {
            override suspend fun syncItem(itemId: Long) = Unit
            override suspend fun onItemScheduleReset(itemId: Long) = Unit
            override suspend fun onItemDeleted(itemId: Long) = Unit
        }
    }
}
