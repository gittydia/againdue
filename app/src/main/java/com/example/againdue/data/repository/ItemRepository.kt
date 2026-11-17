package com.example.againdue.data.repository

import androidx.room.withTransaction
import com.example.againdue.data.local.AppDatabase
import com.example.againdue.data.local.Completion
import com.example.againdue.data.local.Item
import com.example.againdue.domain.IntervalUnit
import com.example.againdue.domain.ItemSorter
import com.example.againdue.domain.ItemSummary
import com.example.againdue.domain.NextDueCalculator
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
    ): Long {
        val now = Instant.now(clock)
        val item = Item(
            name = requireName(name),
            intervalValue = intervalValue,
            intervalUnit = intervalUnit,
            lastDoneAt = lastDoneAt,
            nextDueAt = NextDueCalculator.nextDue(lastDoneAt, intervalValue, intervalUnit),
            note = normalizeNote(note),
            createdAt = now,
            updatedAt = now,
        )
        val id = itemDao.insert(item)
        reminderSynchronizer.syncItem(id)
        return id
    }

    /** Updates an item and recomputes its due date. Returns false when it no longer exists. */
    suspend fun updateItem(
        id: Long,
        name: String,
        intervalValue: Int,
        intervalUnit: IntervalUnit,
        lastDoneAt: LocalDate,
        note: String? = null,
    ): Boolean {
        val existing = itemDao.getById(id) ?: return false
        val updated = existing.copy(
            name = requireName(name),
            intervalValue = intervalValue,
            intervalUnit = intervalUnit,
            lastDoneAt = lastDoneAt,
            nextDueAt = NextDueCalculator.nextDue(lastDoneAt, intervalValue, intervalUnit),
            note = normalizeNote(note),
            updatedAt = Instant.now(clock),
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
            val completion = Completion(
                itemId = id,
                completedAt = Instant.now(clock),
                previousLastDoneAt = item.lastDoneAt,
            )
            val completionId = completionDao.insert(completion)
            itemDao.update(
                item.copy(
                    lastDoneAt = completedOn,
                    nextDueAt = NextDueCalculator.nextDue(
                        completedOn,
                        item.intervalValue,
                        item.intervalUnit,
                    ),
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
            itemDao.update(
                item.copy(
                    lastDoneAt = completion.previousLastDoneAt,
                    nextDueAt = NextDueCalculator.nextDue(
                        completion.previousLastDoneAt,
                        item.intervalValue,
                        item.intervalUnit,
                    ),
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

    private fun Item.toSummary(): ItemSummary = ItemSummary(
        id = id,
        name = name,
        nextDueAt = nextDueAt,
        archived = archived,
    )
}
