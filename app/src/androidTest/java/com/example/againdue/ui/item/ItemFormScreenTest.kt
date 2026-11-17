package com.example.againdue.ui.item

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.againdue.data.local.AppDatabase
import com.example.againdue.data.repository.ItemRepository
import com.example.againdue.domain.IntervalUnit
import com.example.againdue.ui.theme.AgainDueTheme
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class ItemFormScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var database: AppDatabase
    private lateinit var repository: ItemRepository

    private val clock = Clock.fixed(Instant.parse("2026-11-16T09:00:00Z"), ZoneOffset.UTC)

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java,
        ).build()
        repository = ItemRepository(database, clock)
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun save_withBlankFields_showsErrorsAndStaysOpen() {
        val closed = AtomicBoolean(false)
        val viewModel = ItemFormViewModel(repository, clock)

        composeRule.setContent {
            AgainDueTheme {
                ItemFormScreen(viewModel = viewModel, onClose = { closed.set(true) })
            }
        }

        composeRule.onNodeWithTag("save_button").performClick()

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Enter a name").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Enter a name").assertIsDisplayed()
        composeRule.onNodeWithText("Enter how often this repeats").assertIsDisplayed()
        composeRule.runOnIdle { assertFalse(closed.get()) }
        assertEquals(0, runBlocking { database.itemDao().observeActive().first() }.size)
    }

    @Test
    fun save_withValidInput_createsItemAndCloses() {
        val closed = AtomicBoolean(false)
        val viewModel = ItemFormViewModel(repository, clock)

        composeRule.setContent {
            AgainDueTheme {
                ItemFormScreen(viewModel = viewModel, onClose = { closed.set(true) })
            }
        }

        composeRule.onNodeWithTag("name_field").performTextInput("Water filter")
        composeRule.onNodeWithTag("interval_field").performTextInput("3")
        composeRule.onNodeWithTag("save_button").performClick()

        composeRule.waitUntil(5_000) { closed.get() }

        val items = runBlocking { database.itemDao().observeActive().first() }
        assertEquals(1, items.size)
        assertEquals("Water filter", items.single().name)
        assertEquals(3, items.single().intervalValue)
        assertEquals(IntervalUnit.DAY, items.single().intervalUnit)
    }

    @Test
    fun edit_loadsExistingItemAndSavesChanges() {
        val id = runBlocking {
            repository.createItem(
                name = "Passport",
                intervalValue = 6,
                intervalUnit = IntervalUnit.MONTH,
                lastDoneAt = LocalDate.of(2026, 5, 16),
            )
        }
        val closed = AtomicBoolean(false)
        val viewModel = ItemFormViewModel(repository, clock, id)

        composeRule.setContent {
            AgainDueTheme {
                ItemFormScreen(viewModel = viewModel, onClose = { closed.set(true) })
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("name_field").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("name_field").assertTextContains("Passport")
        composeRule.onNodeWithTag("name_field").performTextClearance()
        composeRule.onNodeWithTag("name_field").performTextInput("Renewed passport")
        composeRule.onNodeWithTag("save_button").performClick()

        composeRule.waitUntil(5_000) { closed.get() }
        composeRule.waitUntil(5_000) {
            runBlocking { repository.getItem(id)?.name == "Renewed passport" }
        }
    }

    @Test
    fun archive_closesAndArchivesItem() {
        val id = runBlocking {
            repository.createItem(
                name = "Old cables",
                intervalValue = 1,
                intervalUnit = IntervalUnit.DAY,
                lastDoneAt = LocalDate.of(2026, 11, 1),
            )
        }
        val closed = AtomicBoolean(false)
        val viewModel = ItemFormViewModel(repository, clock, id)

        composeRule.setContent {
            AgainDueTheme {
                ItemFormScreen(viewModel = viewModel, onClose = { closed.set(true) })
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("archive_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("archive_button").performClick()

        composeRule.waitUntil(5_000) { closed.get() }
        composeRule.waitUntil(5_000) {
            runBlocking { repository.getItem(id)?.archived == true }
        }
    }

    @Test
    fun delete_withConfirmation_removesItemAndCloses() {
        val id = runBlocking {
            repository.createItem(
                name = "Old charger",
                intervalValue = 1,
                intervalUnit = IntervalUnit.DAY,
                lastDoneAt = LocalDate.of(2026, 11, 1),
            )
        }
        val closed = AtomicBoolean(false)
        val viewModel = ItemFormViewModel(repository, clock, id)

        composeRule.setContent {
            AgainDueTheme {
                ItemFormScreen(viewModel = viewModel, onClose = { closed.set(true) })
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("delete_button").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("delete_button").performClick()
        composeRule.onNodeWithText("Delete this item?").assertIsDisplayed()
        composeRule.onNodeWithText("Delete").performClick()

        composeRule.waitUntil(5_000) { closed.get() }
        composeRule.waitUntil(5_000) {
            runBlocking { repository.getItem(id) == null }
        }
    }
}
