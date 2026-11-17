package com.example.againdue.ui.home

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

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
    fun emptyState_offersAdd() {
        var addClicks = 0
        composeRule.setContent {
            AgainDueTheme {
                HomeScreen(
                    viewModel = remember { HomeViewModel(repository, clock) },
                    onAddItem = { addClicks++ },
                    onEditItem = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Nothing to remember yet")
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
        composeRule.onNodeWithText("Nothing to remember yet").assertIsDisplayed()
        composeRule.onNodeWithTag("empty_add_button").performClick()

        composeRule.runOnIdle { assertEquals(1, addClicks) }
    }

    @Test
    fun done_recordsCompletionAndOffersUndo() {
        val id = runBlocking {
            repository.createItem(
                name = "Passport",
                intervalValue = 6,
                intervalUnit = IntervalUnit.MONTH,
                lastDoneAt = LocalDate.of(2026, 5, 16),
            )
        }
        composeRule.setContent {
            AgainDueTheme {
                HomeScreen(
                    viewModel = remember { HomeViewModel(repository, clock) },
                    onAddItem = {},
                    onEditItem = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("done_$id").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("done_$id").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithText("Marked done").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithText("Marked done").assertIsDisplayed()

        composeRule.waitUntil(5_000) {
            runBlocking { repository.getItem(id)?.lastDoneAt == LocalDate.of(2026, 11, 16) }
        }

        composeRule.onNodeWithText("Undo").performClick()

        composeRule.waitUntil(5_000) {
            runBlocking { repository.getItem(id)?.lastDoneAt == LocalDate.of(2026, 5, 16) }
        }
    }

    @Test
    fun archivedView_restoresItem() {
        val id = runBlocking {
            repository.createItem(
                name = "Smoke alarm",
                intervalValue = 1,
                intervalUnit = IntervalUnit.WEEK,
                lastDoneAt = LocalDate.of(2026, 11, 1),
            )
        }
        runBlocking { repository.setArchived(id, true) }

        composeRule.setContent {
            AgainDueTheme {
                HomeScreen(
                    viewModel = remember { HomeViewModel(repository, clock) },
                    onAddItem = {},
                    onEditItem = {},
                    onOpenSettings = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("More options").performClick()
        composeRule.onNodeWithText("Show archived").performClick()
        composeRule.waitUntil(5_000) {
            composeRule.onAllNodesWithTag("restore_$id").fetchSemanticsNodes().isNotEmpty()
        }
        composeRule.onNodeWithTag("restore_$id").performClick()

        composeRule.waitUntil(5_000) {
            runBlocking { repository.getItem(id)?.archived == false }
        }
    }
}
