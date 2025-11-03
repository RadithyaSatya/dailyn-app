package com.tara.dailyn.ui.features.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tara.dailyn.data.local.db.AppDatabase
import com.tara.dailyn.data.repository.HabitRepository
import com.tara.dailyn.ui.features.home.model.HomeEvent
import androidx.compose.ui.platform.LocalContext

@Composable
fun HomeRoute(
    onAddHabitClick: () -> Unit = {},
    onHabitClick: (String) -> Unit = {}   // <--- tambahin ini
) {
    val context = LocalContext.current.applicationContext
    val db = remember { AppDatabase.get(context) }
    val repo = remember { HabitRepository(db.habitDao(), db.habitLogDao()) }

    val vm: HomeViewModel = viewModel(
        factory = remember(repo) { HomeViewModelFactory(repo) }
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
        }
    )
}
