package com.example.againdue.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.againdue.AppContainer
import com.example.againdue.ui.home.HomeScreen
import com.example.againdue.ui.home.HomeViewModel
import com.example.againdue.ui.item.ItemFormScreen
import com.example.againdue.ui.item.ItemFormViewModel
import com.example.againdue.ui.settings.SettingsScreen
import com.example.againdue.ui.settings.SettingsViewModel

sealed interface AppScreen {
    data object Home : AppScreen
    data object Add : AppScreen
    data class Edit(val id: Long) : AppScreen
    data object Settings : AppScreen

    companion object {
        val Saver: Saver<AppScreen, String> = Saver(
            save = { screen ->
                when (screen) {
                    AppScreen.Home -> "home"
                    AppScreen.Add -> "add"
                    AppScreen.Settings -> "settings"
                    is AppScreen.Edit -> "edit:${screen.id}"
                }
            },
            restore = { value ->
                when {
                    value == "home" -> AppScreen.Home
                    value == "add" -> AppScreen.Add
                    value == "settings" -> AppScreen.Settings
                    value.startsWith("edit:") ->
                        value.removePrefix("edit:").toLongOrNull()?.let(AppScreen::Edit)

                    else -> null
                }
            },
        )
    }
}

@Composable
fun AgainDueApp(
    container: AppContainer,
    openedItemId: Long? = null,
    onOpenedItemHandled: () -> Unit = {},
) {
    var screen by rememberSaveable(stateSaver = AppScreen.Saver) {
        mutableStateOf<AppScreen>(AppScreen.Home)
    }
    var formVisit by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(openedItemId) {
        if (openedItemId != null) {
            screen = AppScreen.Edit(openedItemId)
            onOpenedItemHandled()
        }
    }

    if (screen != AppScreen.Home) {
        BackHandler { screen = AppScreen.Home }
    }

    when (val current = screen) {
        AppScreen.Home -> {
            val homeViewModel: HomeViewModel = viewModel(
                factory = remember(container) { homeFactory(container) },
            )
            HomeScreen(
                viewModel = homeViewModel,
                onAddItem = {
                    formVisit++
                    screen = AppScreen.Add
                },
                onEditItem = { id -> screen = AppScreen.Edit(id) },
                onOpenSettings = { screen = AppScreen.Settings },
            )
        }

        AppScreen.Add -> {
            val formViewModel: ItemFormViewModel = viewModel(
                key = "form-$formVisit",
                factory = remember(container, formVisit) { formFactory(container, null) },
            )
            ItemFormScreen(
                viewModel = formViewModel,
                onClose = { screen = AppScreen.Home },
            )
        }

        is AppScreen.Edit -> {
            val formViewModel: ItemFormViewModel = viewModel(
                key = "form-edit-${current.id}",
                factory = remember(container, current.id) { formFactory(container, current.id) },
            )
            ItemFormScreen(
                viewModel = formViewModel,
                onClose = { screen = AppScreen.Home },
            )
        }

        AppScreen.Settings -> {
            val settingsViewModel: SettingsViewModel = viewModel(
                factory = remember(container) { settingsFactory(container) },
            )
            SettingsScreen(
                viewModel = settingsViewModel,
                onBack = { screen = AppScreen.Home },
            )
        }
    }
}

private fun homeFactory(container: AppContainer) = viewModelFactory {
    initializer { HomeViewModel(container.itemRepository, container.clock) }
}

private fun formFactory(container: AppContainer, itemId: Long?) = viewModelFactory {
    initializer {
        val settings = container.reminderSettings.current()
        ItemFormViewModel(
            repository = container.itemRepository,
            clock = container.clock,
            itemId = itemId,
            defaultReminderMinuteOfDay = settings.hour * 60 + settings.minute,
        )
    }
}

private fun settingsFactory(container: AppContainer) = viewModelFactory {
    initializer {
        SettingsViewModel(container.reminderSettings, container.reminderScheduler)
    }
}
