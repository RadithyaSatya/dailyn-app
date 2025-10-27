@file:OptIn(ExperimentalMaterial3Api::class)

package com.tara.dailyn.ui.features.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.tara.dailyn.R
import com.tara.dailyn.ui.features.home.components.DateStrip
import com.tara.dailyn.ui.features.home.components.HabitRow
import com.tara.dailyn.ui.features.home.model.HabitUi
import com.tara.dailyn.ui.features.home.model.HomeEvent
import com.tara.dailyn.ui.features.home.model.HomeUiState
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HomeScreen(
    state: HomeUiState,
    onEvent: (HomeEvent) -> Unit,
    modifier: Modifier = Modifier
) {
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
                    IconButton(onClick = { }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.fab_add_content_desc)
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
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 16.dp)
                    ) {
                        items(state.items, key = { it.id }) { habit ->
                            HabitRow(
                                habit = habit,
                                onToggle = { onEvent(HomeEvent.ToggleHabit(habit.id)) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    val sampleState = HomeUiState(
        items = listOf(
            HabitUi("1", "Minum Air 8 Gelas", "Hydration harian", false),
            HabitUi("2", "Olahraga 20 menit", "Jogging ringan", true),
            HabitUi("3", "Baca 10 halaman", "Buku apa saja", false),
            HabitUi("4", "Minum Air 8 Gelas", "Hydration harian", false),
            HabitUi("5", "Olahraga 20 menit", "Jogging ringan", true),
            HabitUi("6", "Baca 10 halaman", "Buku apa saja", false),
            HabitUi("7", "Minum Air 8 Gelas", "Hydration harian", false),
            HabitUi("8", "Olahraga 20 menit", "Jogging ringan", true),
            HabitUi("9", "Baca 10 halaman", "Buku apa saja", false),
        )
    )

    MaterialTheme {
        HomeScreen(
            state = sampleState,
            onEvent = {}
        )
    }
}
