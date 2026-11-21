package com.example.againdue.data.repository

import androidx.room.withTransaction
import com.example.againdue.data.local.AppDatabase
import com.example.againdue.data.local.Completion
import com.example.againdue.data.local.Item
import com.example.againdue.domain.IntervalUnit
import com.example.againdue.domain.ItemSorter
import com.example.againdue.domain.ItemSummary
import com.example.againdue.domain.NextDueCalculator
import com.example.againdue.domain.RecurrenceConfig
import com.example.againdue.domain.RecurrencePattern
import com.example.againdue.domain.RecurrenceResult
import com.example.againdue.domain.ReminderSynchronizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.Instant
import java.time.LocalDate

/**
 * Single entry point for item data.
 *
 * Every write recomputes [Item.nextDueAt] from [Item.lastDoneAt] and the
 * interval, so a stored due date can never drift from its source fields.
 * Completion writes the history row and the item update in one transaction;
 * undo reverses that same pair.
 */
class ItemRepository(
    private val database: AppDatabase,
    private val clock: Clock = Clock.systemDefaultZone(),
    private val reminderSynchronizer: ReminderSynchronizer = ReminderSynchronizer.None,
) {

    private val itemDao = database.itemDao()
    private val completionDao = database.completionDao()

    fun observeActiveItems(): Flow<List<ItemSummary>> =
        itemDao.observeActive().map { items ->
            ItemSorter.sortActive(items.map { it.toSummary() })
        }

    /**
     * Full items for the home list, either active or archived, in urgency order.
     * Sorting still comes from [ItemSorter]; entities are re-ordered to match.
     */
    fun observeItems(archived: Boolean): Flow<List<Item>> =
        (if (archived) itemDao.observeArchived() else itemDao.observeActive())
            .map { items ->
                val byId = items.associateBy { it.id }
                ItemSorter.sortByUrgency(items.map { it.toSummary() }).mapNotNull { byId[it.id] }
            }

    fun observeItem(id: Long): Flow<Item?> = itemDao.observeById(id)

    suspend fun getItem(id: Long): Item? = itemDao.getById(id)

    suspend fun getLatestCompletion(itemId: Long): Completion? =
        completionDao.latestForItem(itemId)

    suspend fun createItem(
        name: String,
        intervalValue: Int,
        intervalUnit: IntervalUnit,
        lastDoneAt: LocalDate,
        note: String? = null,
        startDate: LocalDate? = null,
        pattern: RecurrencePattern? = null,
        endDate: LocalDate? = null,
        endAfterCount: Int? = null,
        reminderMinuteOfDay: Int? = null,
    ): Long {
        requireReminderMinuteOfDay(reminderMinuteOfDay)
        val now = Instant.now(clock)
        val recurrence = RecurrenceConfig(
            intervalValue = intervalValue,
            intervalUnit = intervalUnit,
            startDate = startDate ?: lastDoneAt,
            pattern = pattern,
            endDate = endDate,
            endAfterCount = endAfterCount,
        )
        val schedule = NextDueCalculator.nextRecurrence(lastDoneAt, recurrence)
        val item = Item(
            name = requireName(name),
            intervalValue = intervalValue,
            intervalUnit = intervalUnit,
            lastDoneAt = lastDoneAt,
            nextDueAt = schedule.nextDueAt,
            note = normalizeNote(note),
            createdAt = now,
            updatedAt = now,
            startDate = recurrence.startDate,
            pattern = pattern,
            endDate = endDate,
            endAfterCount = endAfterCount,
            recurrenceEnded = schedule.ended,
            reminderMinuteOfDay = reminderMinuteOfDay,
        )
        val id = itemDao.insert(item)
        reminderSynchronizer.syncItem(id)
        return id
    }

    /**
     * Updates an item and recomputes its due date from the supplied definition.
     *
     * The recurrence fields are replaced verbatim: passing a null pattern turns
     * the item back into a plain completion-based interval.
     */
    suspend fun updateItem(
        id: Long,
        name: String,
        intervalValue: Int,
        intervalUnit: IntervalUnit,
        lastDoneAt: LocalDate,
        note: String? = null,
        startDate: LocalDate? = null,
        pattern: RecurrencePattern? = null,
        endDate: LocalDate? = null,
        endAfterCount: Int? = null,
        reminderMinuteOfDay: Int? = null,
    ): Boolean {
        val existing = itemDao.getById(id) ?: return false
        requireReminderMinuteOfDay(reminderMinuteOfDay)
        val recurrence = RecurrenceConfig(
            intervalValue = intervalValue,
            intervalUnit = intervalUnit,
            startDate = startDate ?: lastDoneAt,
            pattern = pattern,
            endDate = endDate,
            endAfterCount = endAfterCount,
        )
        val schedule = NextDueCalculator.nextRecurrence(
            lastDoneAt = lastDoneAt,
            recurrence = recurrence,
            completedCount = existing.completedCount,
        )
        val updated = existing.copy(
            name = requireName(name),
            intervalValue = intervalValue,
            intervalUnit = intervalUnit,
            lastDoneAt = lastDoneAt,
            nextDueAt = schedule.nextDueAt,
            note = normalizeNote(note),
            updatedAt = Instant.now(clock),
            startDate = recurrence.startDate,
            pattern = pattern,
            endDate = endDate,
            endAfterCount = endAfterCount,
            recurrenceEnded = schedule.ended,
            reminderMinuteOfDay = reminderMinuteOfDay,
        )
        itemDao.update(updated)
        reminderSynchronizer.syncItem(id)
        return true
    }

    /**
     * Records a completion on [completedOn] (defaults to today) and advances the
     * schedule from that date. Returns the stored completion, or null when the
     * item no longer exists.
     */
    suspend fun completeItem(
        id: Long,
        completedOn: LocalDate = LocalDate.now(clock),
    ): Completion? {
        val completion = database.withTransaction {
            val item = itemDao.getById(id) ?: return@withTransaction null
            if (item.recurrenceEnded) return@withTransaction null
            val completion = Completion(
                itemId = id,
                completedAt = Instant.now(clock),
                previousLastDoneAt = item.lastDoneAt,
            )
            val completionId = completionDao.insert(completion)
            val completedCount = item.completedCount + 1
            val schedule = NextDueCalculator.nextRecurrence(
                lastDoneAt = completedOn,
                recurrence = item.toRecurrenceConfig(),
                completedCount = completedCount,
            )
            itemDao.update(
                item.copy(
                    lastDoneAt = completedOn,
                    nextDueAt = schedule.nextDueAt,
                    completedCount = completedCount,
                    recurrenceEnded = schedule.ended,
                    updatedAt = Instant.now(clock),
                ),
            )
            completion.copy(id = completionId)
        }
        if (completion != null) {
            reminderSynchronizer.syncItem(id)
        }
        return completion
    }

    /**
     * Reverses the most recent completion of [itemId]. Returns false when there
     * is nothing to undo.
     */
    suspend fun undoLastCompletion(itemId: Long): Boolean {
        val undone = database.withTransaction {
            val completion = completionDao.latestForItem(itemId) ?: return@withTransaction false
            val item = itemDao.getById(itemId) ?: return@withTransaction false
            val completedCount = (item.completedCount - 1).coerceAtLeast(0)
            val schedule = NextDueCalculator.nextRecurrence(
                lastDoneAt = completion.previousLastDoneAt,
                recurrence = item.toRecurrenceConfig(),
                completedCount = completedCount,
            )
            itemDao.update(
                item.copy(
                    lastDoneAt = completion.previousLastDoneAt,
                    nextDueAt = schedule.nextDueAt,
                    completedCount = completedCount,
                    recurrenceEnded = schedule.ended,
                    updatedAt = Instant.now(clock),
                ),
            )
            completionDao.deleteById(completion.id)
            true
        }
        if (undone) {
            reminderSynchronizer.syncItem(itemId)
        }
        return undone
    }

    suspend fun setArchived(id: Long, archived: Boolean): Boolean {
        val updated = itemDao.setArchived(id, archived, Instant.now(clock)) > 0
        if (updated) {
            reminderSynchronizer.syncItem(id)
        }
        return updated
    }

    /** Deletes the item; completion history is removed by the foreign-key cascade. */
    suspend fun deleteItem(id: Long): Boolean {
        val deleted = itemDao.deleteById(id) > 0
        if (deleted) {
            reminderSynchronizer.onItemDeleted(id)
        }
        return deleted
    }

    private fun requireName(name: String): String {
        val trimmed = name.trim()
        require(trimmed.isNotEmpty()) { "Item name must not be blank" }
        return trimmed
    }

    private fun normalizeNote(note: String?): String? =
        note?.trim()?.takeIf { it.isNotEmpty() }

    private fun requireReminderMinuteOfDay(reminderMinuteOfDay: Int?) {
        require(reminderMinuteOfDay == null || reminderMinuteOfDay in 0..1439) {
            "Reminder minute of day must be between 0 and 1439"
        }
    }

    private fun Item.toSummary(): ItemSummary = ItemSummary(
        id = id,
        name = name,
        nextDueAt = nextDueAt,
        archived = archived,
        ended = recurrenceEnded,
    )

    private fun Item.toRecurrenceConfig(): RecurrenceConfig = RecurrenceConfig(
        intervalValue = intervalValue,
        intervalUnit = intervalUnit,
        startDate = startDate ?: lastDoneAt,
        pattern = pattern,
        endDate = endDate,
        endAfterCount = endAfterCount,
    )
}
