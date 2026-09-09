package com.tara.dailyn.ui.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tara.dailyn.data.local.db.AppDatabase
import com.tara.dailyn.data.repository.HabitRepository
import com.tara.dailyn.ui.features.home.model.HomeEvent
import androidx.compose.ui.platform.LocalContext
import com.tara.dailyn.data.preferences.AppSettings

@Composable
fun HomeRoute(
    onAddHabitClick: () -> Unit = {},
    onHabitClick: (String) -> Unit = {},
    onSettingsClick: () -> Unit = {}
) {
    val context = LocalContext.current.applicationContext
    val db = remember { AppDatabase.get(context) }
    val repo = remember { HabitRepository(db.categoryDao(), db.habitDao(), db.habitLogDao(), context) }
    val appSettings = remember { AppSettings(context) }

    val vm: HomeViewModel = viewModel(
        factory = remember(repo, appSettings) { HomeViewModelFactory(repo, appSettings) }
    )

    val state = vm.state.collectAsStateWithLifecycle().value

    HomeScreen(
        state = state,
        onEvent = { ev ->
            when (ev) {
                HomeEvent.AddHabit -> onAddHabitClick()
                else -> vm.onEvent(ev)
            }
        },
        onHabitClick = { habitId ->
            onHabitClick(habitId)
        },
        onSettingsClick = onSettingsClick
    )
}
