package com.tara.dailyn.ui.features.addhabit

import AddHabitViewModel
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AddHabitRoute(
    vm: AddHabitViewModel = viewModel()
) {
    val state = vm.state.collectAsStateWithLifecycle().value

    AddHabitScreen(
        state,
        onEvent = vm::onEvent
    )
}