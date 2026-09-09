@file:OptIn(ExperimentalMaterial3Api::class)

package com.tara.dailyn.ui.features.home

import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.tara.dailyn.R
import com.tara.dailyn.ui.features.home.components.DateStrip
import com.tara.dailyn.ui.features.home.components.HabitRow
import com.tara.dailyn.ui.features.home.model.HabitUi
import com.tara.dailyn.ui.features.home.model.HomeEvent
import com.tara.dailyn.ui.features.home.model.HomeUiState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    onHabitClick: (String) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isToday = state.selectedDate == LocalDate.now()
    val canToggleHabits = isToday || (
        state.selectedDate.isBefore(LocalDate.now()) && state.allowPreviousDayEdits
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.home_title)) },
                actions = {
                    IconButton(onClick = { onEvent(HomeEvent.AddHabit) }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.fab_add_content_desc)
                        )
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.settings_title)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        val paddingValues = PaddingValues(
            top = innerPadding.calculateTopPadding(),
            end = innerPadding.calculateEndPadding(LayoutDirection.Ltr),
            bottom = innerPadding.calculateBottomPadding() / 2,
            start = innerPadding.calculateStartPadding(LayoutDirection.Ltr)
        )
        Column(
            modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            DateStrip(
                selectedDate = state.selectedDate,
                onSelect = { onEvent(HomeEvent.SelectDate(it)) },
                modifier = Modifier.fillMaxWidth()
            )

            val formatted = state.selectedDate.format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale.getDefault())
            )
            Text(
                text = formatted,
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )

            when {
                state.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                state.items.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(stringResource(R.string.empty_list_message))
                    }
                }
                else -> {
                    val pendingItems = state.items.filterNot { it.isCompletedToday }
                    val completedItems = state.items.filter { it.isCompletedToday }
                    var pendingOrder by remember(state.selectedDate) { mutableStateOf(pendingItems.map { it.id }) }
                    var awaitingPersistedOrder by remember(state.selectedDate) { mutableStateOf<List<String>?>(null) }
                    var draggingItemId by remember { mutableStateOf<String?>(null) }
                    var draggingOffsetY by remember { mutableFloatStateOf(0f) }
                    val itemHeights = remember { mutableStateMapOf<String, Int>() }

                    LaunchedEffect(state.selectedDate, pendingItems.map { it.id }, draggingItemId) {
                        if (draggingItemId != null) return@LaunchedEffect

                        val incomingIds = pendingItems.map { it.id }
                        val awaitedOrder = awaitingPersistedOrder

                        when {
                            awaitedOrder != null && incomingIds == awaitedOrder -> {
                                pendingOrder = incomingIds
                                awaitingPersistedOrder = null
                            }
                            awaitedOrder != null && incomingIds.toSet() == awaitedOrder.toSet() -> Unit
                            else -> {
                                awaitingPersistedOrder = null
                                if (pendingOrder != incomingIds) {
                                    pendingOrder = incomingIds
                                }
                            }
                        }
                    }

                    val pendingItemsById = pendingItems.associateBy { it.id }
                    val orderedPendingItems = pendingOrder.mapNotNull { pendingItemsById[it] }

                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        if (pendingItems.isNotEmpty()) {
                            item(key = "pending_header") {
                                HomeSectionHeader(
                                    title = if (isToday) {
                                        "To do"
                                    } else {
                                        "Not checked"
                                    },
                                    count = orderedPendingItems.size
                                )
                            }
                            itemsIndexed(orderedPendingItems, key = { _, item -> item.id }) { _, habit ->
                                HabitRow(
                                    habit = habit,
                                    onClick = { onHabitClick(habit.id) },
                                    onToggle = { onEvent(HomeEvent.ToggleHabit(habit.id)) },
                                    toggleEnabled = canToggleHabits,
                                    dragEnabled = isToday,
                                    dragHandle = {
                                        Text(
                                            text = "\u22EE",
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    },
                                    modifier = Modifier
                                        .graphicsLayer {
                                            translationY = if (draggingItemId == habit.id) draggingOffsetY else 0f
                                        }
                                        .zIndex(if (draggingItemId == habit.id) 1f else 0f)
                                        .pointerInput(habit.id, isToday) {
                                            if (!isToday) return@pointerInput
                                            detectDragGesturesAfterLongPress(
                                                onDragStart = {
                                                    draggingItemId = habit.id
                                                    draggingOffsetY = 0f
                                                },
                                                onDragCancel = {
                                                    draggingItemId = null
                                                    draggingOffsetY = 0f
                                                },
                                                onDragEnd = {
                                                    val finalOrder = pendingOrder
                                                    awaitingPersistedOrder = finalOrder
                                                    draggingItemId = null
                                                    draggingOffsetY = 0f
                                                    onEvent(HomeEvent.ReorderPendingHabits(finalOrder))
                                                },
                                                onDrag = { change, dragAmount ->
                                                    val activeId = draggingItemId ?: return@detectDragGesturesAfterLongPress
                                                    draggingOffsetY += dragAmount.y

                                                    val currentIndex = pendingOrder.indexOf(activeId)
                                                    if (currentIndex == -1) return@detectDragGesturesAfterLongPress

                                                    val nextId = pendingOrder.getOrNull(currentIndex + 1)
                                                    val prevId = pendingOrder.getOrNull(currentIndex - 1)
                                                    val nextHeight = nextId?.let { itemHeights[it] } ?: 0
                                                    val prevHeight = prevId?.let { itemHeights[it] } ?: 0

                                                    when {
                                                        draggingOffsetY > nextHeight / 2f && nextId != null -> {
                                                            pendingOrder = pendingOrder.toMutableList().apply {
                                                                removeAt(currentIndex)
                                                                add(currentIndex + 1, activeId)
                                                            }
                                                            draggingOffsetY -= nextHeight.toFloat()
                                                        }
                                                        draggingOffsetY < -(prevHeight / 2f) && prevId != null -> {
                                                            pendingOrder = pendingOrder.toMutableList().apply {
                                                                removeAt(currentIndex)
                                                                add(currentIndex - 1, activeId)
                                                            }
                                                            draggingOffsetY += prevHeight.toFloat()
                                                        }
                                                    }
                                                }
                                            )
                                        }
                                        .onSizeChanged { itemHeights[habit.id] = it.height }
                                )
                            }
                        }

                        if (completedItems.isNotEmpty()) {
                            item(key = "completed_header") {
                                HomeSectionHeader(
                                    title = "Completed",
                                    count = completedItems.size
                                )
                            }
                            items(completedItems, key = { it.id }) { habit ->
                                HabitRow(
                                    habit = habit,
                                    onClick = { onHabitClick(habit.id) },
                                    onToggle = { onEvent(HomeEvent.ToggleHabit(habit.id)) },
                                    toggleEnabled = canToggleHabits
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeSectionHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall
        )
        Spacer(Modifier.width(8.dp))
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    val sampleState = HomeUiState(
        items = listOf(
            HabitUi("1", "Minum Air 8 Gelas", "Hydration harian", "Health", "💧", false),
            HabitUi("2", "Olahraga 20 menit", "Jogging ringan", "Fitness", "🏃", true),
            HabitUi("3", "Baca 10 halaman", "Buku apa saja", "Learning", "📚", false),
            HabitUi("4", "Minum Air 8 Gelas", "Hydration harian", "Health", "💧", false),
            HabitUi("5", "Olahraga 20 menit", "Jogging ringan", "Fitness", "🏃", true),
            HabitUi("6", "Baca 10 halaman", "Buku apa saja", "Learning", "📚", false),
            HabitUi("7", "Minum Air 8 Gelas", "Hydration harian", "Health", "💧", false),
            HabitUi("8", "Olahraga 20 menit", "Jogging ringan", "Fitness", "🏃", true),
            HabitUi("9", "Baca 10 halaman", "Buku apa saja", "Learning", "📚", false),
        )
    )

    MaterialTheme {
        HomeScreen(
            state = sampleState,
            onEvent = {},
            onHabitClick = {},
            onSettingsClick = {}
        )
    }
}
