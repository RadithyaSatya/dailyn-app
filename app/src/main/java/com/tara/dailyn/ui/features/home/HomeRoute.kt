package com.tara.dailyn.ui.features.home

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tara.dailyn.ui.features.home.model.HomeEvent

@Composable
fun HomeRoute(
    vm: HomeViewModel = viewModel()
) {
    val state = vm.state.collectAsStateWithLifecycle().value

    HomeScreen(
        state = state,
        onEvent = vm::onEvent
    )
}
