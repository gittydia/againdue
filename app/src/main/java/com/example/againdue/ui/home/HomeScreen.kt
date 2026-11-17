package com.example.againdue.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.againdue.R
import com.example.againdue.domain.ItemStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onAddItem: () -> Unit,
    onEditItem: (Long) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    var menuExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.Completed -> {
                    val result = snackbarHostState.showSnackbar(
                        message = "Marked done",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.undo(event.itemId)
                    }
                }

                is HomeEvent.Undone -> {
                    snackbarHostState.showSnackbar("Completion undone")
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Image(
                            painter = painterResource(R.drawable.app_logo),
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.app_name))
                    }
                },
                actions = {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(if (state.showArchived) "Hide archived" else "Show archived")
                            },
                            onClick = {
                                menuExpanded = false
                                viewModel.toggleArchived()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text("Settings") },
                            onClick = {
                                menuExpanded = false
                                onOpenSettings()
                            },
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onAddItem,
                icon = { Icon(Icons.Filled.Add, contentDescription = null) },
                text = { Text("Add item") },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }

                state.loadError != null -> {
                    LoadErrorState(
                        message = state.loadError.orEmpty(),
                        onRetry = viewModel::retry,
                    )
                }

                state.items.isEmpty() -> {
                    EmptyState(showArchived = state.showArchived, onAddItem = onAddItem)
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(start = 16.dp, top = 12.dp, end = 16.dp, bottom = 96.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.items, key = { it.id }) { item ->
                            ItemCard(
                                item = item,
                                onDone = { viewModel.complete(item.id) },
                                onEdit = { onEditItem(item.id) },
                                onRestore = { viewModel.restore(item.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(showArchived: Boolean, onAddItem: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Notifications,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = if (showArchived) "No archived items" else "Nothing to remember yet",
            style = MaterialTheme.typography.headlineSmall,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = if (showArchived) {
                "Items you archive will appear here so you can restore them later."
            } else {
                "Add something you want to keep up with. It comes back around when it's due."
            },
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!showArchived) {
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onAddItem,
                modifier = Modifier.testTag("empty_add_button"),
            ) {
                Text("Add your first item")
            }
        }
    }
}

@Composable
private fun LoadErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "Couldn't load your items",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRetry) {
            Text("Try again")
        }
    }
}

@Composable
private fun ItemCard(
    item: HomeItemUi,
    onDone: () -> Unit,
    onEdit: () -> Unit,
    onRestore: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item_card_${item.id}"),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${item.intervalText} · ${item.lastDoneText}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (item.archived) {
                    Text(
                        text = "Archived",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusLine(status = item.status)
                Spacer(Modifier.weight(1f))
                TextButton(
                    onClick = onEdit,
                    modifier = Modifier.testTag("edit_${item.id}"),
                ) {
                    Text("Edit")
                }
                if (item.archived) {
                    Spacer(Modifier.width(4.dp))
                    TextButton(
                        onClick = onRestore,
                        modifier = Modifier.testTag("restore_${item.id}"),
                    ) {
                        Text("Restore")
                    }
                } else {
                    Spacer(Modifier.width(4.dp))
                    Button(
                        onClick = onDone,
                        modifier = Modifier.testTag("done_${item.id}"),
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

@Composable
private fun StatusLine(status: ItemStatus) {
    val color = when (status) {
        is ItemStatus.Overdue -> MaterialTheme.colorScheme.error
        ItemStatus.DueToday -> MaterialTheme.colorScheme.primary
        is ItemStatus.DueIn -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = when (status) {
        is ItemStatus.Overdue -> Icons.Filled.Warning
        ItemStatus.DueToday -> Icons.Filled.DateRange
        is ItemStatus.DueIn -> Icons.Filled.Notifications
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = color,
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = status.label,
            style = MaterialTheme.typography.labelLarge,
            color = color,
        )
    }
}
