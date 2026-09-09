package com.tara.dailyn.ui.features.analysis

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tara.dailyn.data.local.db.AppDatabase
import com.tara.dailyn.data.repository.HabitRepository

@Composable
fun AnalysisRoute() {
    val context = LocalContext.current.applicationContext
    val db = remember { AppDatabase.get(context) }
    val repo = remember { HabitRepository(db.categoryDao(), db.habitDao(), db.habitLogDao(), context) }

    val vm: AnalysisViewModel = viewModel(
        factory = remember(repo) { AnalysisViewModelFactory(repo) }
    )

    val state = vm.state.collectAsStateWithLifecycle().value

    AnalysisScreen(
        state = state,
        onPreviousWeek = vm::showPreviousWeek,
        onNextWeek = vm::showNextWeek
    )
}
