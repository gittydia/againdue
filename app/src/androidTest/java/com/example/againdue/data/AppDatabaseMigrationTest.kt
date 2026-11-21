package com.example.againdue.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.againdue.data.local.AppDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
    private lateinit var context: Context
    private var database: AppDatabase? = null

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        context.deleteDatabase(DATABASE_NAME)
    }

    @After
    fun tearDown() {
        database?.close()
        context.deleteDatabase(DATABASE_NAME)
    }

    @Test
    fun migration2To3AddsNullableReminderOverrideWithoutChangingItems() = runBlocking {
        context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null).use { sqlite ->
            sqlite.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `items` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `intervalValue` INTEGER NOT NULL,
                    `intervalUnit` TEXT NOT NULL,
                    `lastDoneAt` INTEGER NOT NULL,
                    `nextDueAt` INTEGER NOT NULL,
                    `note` TEXT,
                    `archived` INTEGER NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `updatedAt` INTEGER NOT NULL,
                    `startDate` INTEGER,
                    `pattern` TEXT,
                    `endDate` INTEGER,
                    `endAfterCount` INTEGER,
                    `recurrenceEnded` INTEGER NOT NULL DEFAULT 0,
                    `completedCount` INTEGER NOT NULL DEFAULT 0
                )
                """.trimIndent(),
            )
            sqlite.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `completions` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `itemId` INTEGER NOT NULL,
                    `completedAt` INTEGER NOT NULL,
                    `previousLastDoneAt` INTEGER NOT NULL,
                    FOREIGN KEY(`itemId`) REFERENCES `items`(`id`) ON UPDATE NO ACTION ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            sqlite.execSQL("CREATE INDEX IF NOT EXISTS `index_completions_itemId` ON `completions` (`itemId`)")
            sqlite.execSQL(
                """
                INSERT INTO items (
                    id, name, intervalValue, intervalUnit, lastDoneAt, nextDueAt,
                    note, archived, createdAt, updatedAt, startDate, pattern,
                    endDate, endAfterCount, recurrenceEnded, completedCount
                ) VALUES (1, 'Water plants', 1, 'DAY', 20773, 20774, NULL, 0,
                    1794787200000, 1794787200000, 20773, NULL, NULL, NULL, 0, 0)
                """.trimIndent(),
            )
            sqlite.version = 2
        }

        database = Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()

        val item = database!!.itemDao().getById(1)
        assertEquals("Water plants", item!!.name)
        assertNull(item.reminderMinuteOfDay)
    }

    private companion object {
        const val DATABASE_NAME = "againdue-migration-test.db"
    }
}
