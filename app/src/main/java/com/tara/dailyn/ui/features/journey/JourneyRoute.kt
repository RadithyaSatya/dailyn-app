package com.tara.dailyn.ui.features.journey

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tara.dailyn.data.local.db.AppDatabase
import com.tara.dailyn.data.repository.HabitRepository

@Composable
fun JourneyRoute() {
    val context = LocalContext.current.applicationContext
    val db = remember { AppDatabase.get(context) }
    val repo = remember { HabitRepository(db.categoryDao(), db.habitDao(), db.habitLogDao(), context) }

    val vm: JourneyViewModel = viewModel(
        factory = remember(repo) {
            object : androidx.lifecycle.ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(JourneyViewModel::class.java))
                    return JourneyViewModel(repo) as T
                }
            }
        }
    )

    val state = vm.state.collectAsStateWithLifecycle().value

    JourneyScreen(state = state)
}
