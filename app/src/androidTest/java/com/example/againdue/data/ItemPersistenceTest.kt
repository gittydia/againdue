package com.example.againdue.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.againdue.data.local.AppDatabase
import com.example.againdue.data.repository.ItemRepository
import com.example.againdue.domain.IntervalUnit
import com.example.againdue.domain.ReminderSynchronizer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Verifies Milestone 2 on a real device against a file-backed database: the six
 * core operations apply and survive a simulated process restart (close + reopen).
 */
@RunWith(AndroidJUnit4::class)
class ItemPersistenceTest {

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var repository: ItemRepository

    private val clock = Clock.fixed(Instant.parse("2026-11-16T09:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
        openDatabase()
    }

    @After
    fun tearDown() {
        database.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    private fun openDatabase() {
        database = Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME).build()
        repository = ItemRepository(database, clock)
    }

    private fun simulateRestart() {
        database.close()
        openDatabase()
    }

    @Test
    fun create_persistsAcrossRestart_withDerivedDueDate() = runBlocking {
        val id = repository.createItem(
            name = "Water plants",
            intervalValue = 7,
            intervalUnit = IntervalUnit.DAY,
            lastDoneAt = LocalDate.of(2026, 11, 10),
        )

        simulateRestart()

        val item = repository.getItem(id)
        assertNotNull(item)
        assertEquals("Water plants", item!!.name)
        assertEquals(LocalDate.of(2026, 11, 10), item.lastDoneAt)
        assertEquals(LocalDate.of(2026, 11, 17), item.nextDueAt)
        assertFalse(item.archived)
    }

    @Test
    fun edit_persistsAcrossRestart_andRecalculatesDueDate() = runBlocking {
        val id = repository.createItem(
            name = "Water plants",
            intervalValue = 7,
            intervalUnit = IntervalUnit.DAY,
            lastDoneAt = LocalDate.of(2026, 11, 10),
        )

        val updated = repository.updateItem(
            id = id,
            name = "Water all plants",
            intervalValue = 2,
            intervalUnit = IntervalUnit.WEEK,
            lastDoneAt = LocalDate.of(2026, 11, 12),
            note = "Balcony and hallway",
        )
        assertTrue(updated)

        simulateRestart()

        val item = repository.getItem(id)!!
        assertEquals("Water all plants", item.name)
        assertEquals(2, item.intervalValue)
        assertEquals(IntervalUnit.WEEK, item.intervalUnit)
        assertEquals(LocalDate.of(2026, 11, 12), item.lastDoneAt)
        assertEquals(LocalDate.of(2026, 11, 26), item.nextDueAt)
        assertEquals("Balcony and hallway", item.note)
    }

    @Test
    fun editLastDone_resetsReminderSchedule() = runBlocking {
        val id = repository.createItem(
            name = "Water plants",
            intervalValue = 7,
            intervalUnit = IntervalUnit.DAY,
            lastDoneAt = LocalDate.of(2026, 11, 10),
        )
        val reminders = RecordingReminderSynchronizer()
        repository = ItemRepository(database, clock, reminders)

        assertTrue(
            repository.updateItem(
                id = id,
                name = "Water plants",
                intervalValue = 7,
                intervalUnit = IntervalUnit.DAY,
                lastDoneAt = LocalDate.of(2026, 11, 11),
            ),
        )

        assertEquals(listOf(id), reminders.scheduleResets)
        assertTrue(reminders.syncedItems.isEmpty())
    }

    @Test
    fun editWithoutLastDoneChange_normallySyncsReminder() = runBlocking {
        val lastDoneAt = LocalDate.of(2026, 11, 10)
        val id = repository.createItem(
            name = "Water plants",
            intervalValue = 7,
            intervalUnit = IntervalUnit.DAY,
            lastDoneAt = lastDoneAt,
        )
        val reminders = RecordingReminderSynchronizer()
        repository = ItemRepository(database, clock, reminders)

        assertTrue(
            repository.updateItem(
                id = id,
                name = "Water all plants",
                intervalValue = 7,
                intervalUnit = IntervalUnit.DAY,
                lastDoneAt = lastDoneAt,
            ),
        )

        assertEquals(listOf(id), reminders.syncedItems)
        assertTrue(reminders.scheduleResets.isEmpty())
    }

    @Test
    fun reminderOverride_canBePersistedAndCleared() = runBlocking {
        val id = repository.createItem(
            name = "Water plants",
            intervalValue = 7,
            intervalUnit = IntervalUnit.DAY,
            lastDoneAt = LocalDate.of(2026, 11, 10),
            reminderMinuteOfDay = 20 * 60 + 15,
        )

        simulateRestart()
        assertEquals(20 * 60 + 15, repository.getItem(id)!!.reminderMinuteOfDay)

        val item = repository.getItem(id)!!
        assertTrue(
            repository.updateItem(
                id = id,
                name = item.name,
                intervalValue = item.intervalValue,
                intervalUnit = item.intervalUnit,
                lastDoneAt = item.lastDoneAt,
                note = item.note,
                startDate = item.startDate,
                pattern = item.pattern,
                endDate = item.endDate,
                endAfterCount = item.endAfterCount,
                reminderMinuteOfDay = null,
            ),
        )

        simulateRestart()
        assertNull(repository.getItem(id)!!.reminderMinuteOfDay)
    }

    @Test
    fun complete_recordsHistoryAndAdvancesSchedule_atomically() = runBlocking {
        val id = repository.createItem(
            name = "Water plants",
            intervalValue = 7,
            intervalUnit = IntervalUnit.DAY,
            lastDoneAt = LocalDate.of(2026, 11, 10),
        )

        val completion = repository.completeItem(id)
        assertNotNull(completion)
        assertEquals(LocalDate.of(2026, 11, 10), completion!!.previousLastDoneAt)

        val item = repository.getItem(id)!!
        assertEquals(LocalDate.of(2026, 11, 16), item.lastDoneAt)
        assertEquals(LocalDate.of(2026, 11, 23), item.nextDueAt)

        simulateRestart()

        assertEquals(1, database.completionDao().countForItem(id))
        assertEquals(LocalDate.of(2026, 11, 23), repository.getItem(id)!!.nextDueAt)
    }

    @Test
    fun undo_restoresPreviousCompletionState_andIsSingleUse() = runBlocking {
        val id = repository.createItem(
            name = "Water plants",
            intervalValue = 7,
            intervalUnit = IntervalUnit.DAY,
            lastDoneAt = LocalDate.of(2026, 11, 10),
        )
        repository.completeItem(id)

        assertTrue(repository.undoLastCompletion(id))

        val item = repository.getItem(id)!!
        assertEquals(LocalDate.of(2026, 11, 10), item.lastDoneAt)
        assertEquals(LocalDate.of(2026, 11, 17), item.nextDueAt)
        assertEquals(0, database.completionDao().countForItem(id))
        assertFalse(repository.undoLastCompletion(id))

        simulateRestart()

        assertEquals(LocalDate.of(2026, 11, 17), repository.getItem(id)!!.nextDueAt)
    }

    @Test
    fun archive_hidesItemFromActiveList_andPersists() = runBlocking {
        val id = repository.createItem(
            name = "Clean gutters",
            intervalValue = 1,
            intervalUnit = IntervalUnit.MONTH,
            lastDoneAt = LocalDate.of(2026, 10, 31),
        )
        assertEquals(1, repository.observeActiveItems().first().size)

        assertTrue(repository.setArchived(id, true))
        assertTrue(repository.observeActiveItems().first().isEmpty())

        simulateRestart()

        assertTrue(repository.observeActiveItems().first().isEmpty())
        assertTrue(repository.getItem(id)!!.archived)

        assertTrue(repository.setArchived(id, false))
        assertEquals(1, repository.observeActiveItems().first().size)
    }

    @Test
    fun delete_removesItemAndCompletionHistory_andPersists() = runBlocking {
        val id = repository.createItem(
            name = "Replace filter",
            intervalValue = 3,
            intervalUnit = IntervalUnit.MONTH,
            lastDoneAt = LocalDate.of(2026, 9, 1),
        )
        repository.completeItem(id)
        assertEquals(1, database.completionDao().countForItem(id))

        assertTrue(repository.deleteItem(id))
        assertNull(repository.getItem(id))
        assertEquals(0, database.completionDao().countForItem(id))

        simulateRestart()

        assertNull(repository.getItem(id))
    }

    @Test
    fun activeList_isSortedByUrgency() = runBlocking {
        val upcoming = repository.createItem(
            "Upcoming", 30, IntervalUnit.DAY, LocalDate.of(2026, 11, 10),
        )
        val overdue = repository.createItem(
            "Overdue", 7, IntervalUnit.DAY, LocalDate.of(2026, 11, 1),
        )
        val dueToday = repository.createItem(
            "Due today", 7, IntervalUnit.DAY, LocalDate.of(2026, 11, 9),
        )

        val orderedNames = repository.observeActiveItems().first().map { it.name }

        assertEquals(listOf("Overdue", "Due today", "Upcoming"), orderedNames)
        assertTrue(orderedNames.size == 3)
        assertEquals(setOf(upcoming, overdue, dueToday), repository.observeActiveItems().first().map { it.id }.toSet())
    }

    @Test
    fun invalidInput_isRejected_andNothingIsStored() = runBlocking {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.createItem(
                    name = "   ",
                    intervalValue = 7,
                    intervalUnit = IntervalUnit.DAY,
                    lastDoneAt = LocalDate.of(2026, 11, 10),
                )
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking {
                repository.createItem(
                    name = "Zero interval",
                    intervalValue = 0,
                    intervalUnit = IntervalUnit.DAY,
                    lastDoneAt = LocalDate.of(2026, 11, 10),
                )
            }
        }

        simulateRestart()

        assertTrue(repository.observeActiveItems().first().isEmpty())
    }

    private class RecordingReminderSynchronizer : ReminderSynchronizer {
        val syncedItems = mutableListOf<Long>()
        val scheduleResets = mutableListOf<Long>()

        override suspend fun syncItem(itemId: Long) {
            syncedItems += itemId
        }

        override suspend fun onItemScheduleReset(itemId: Long) {
            scheduleResets += itemId
        }

        override suspend fun onItemDeleted(itemId: Long) = Unit
    }

    private companion object {
        const val DATABASE_NAME = "againdue-persistence-test.db"
    }
}
