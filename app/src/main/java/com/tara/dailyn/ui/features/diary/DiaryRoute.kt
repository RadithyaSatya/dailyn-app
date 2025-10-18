package com.tara.dailyn.ui.features.diary

import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.tara.dailyn.ui.features.home.HomeScreen
import com.tara.dailyn.ui.features.home.HomeViewModel

@Composable
fun DiaryRoute(
    vm: DiaryViewModel = viewModel()
) {
    val state = vm.state.collectAsStateWithLifecycle().value

    DiaryScreen(
        state
    )
}
