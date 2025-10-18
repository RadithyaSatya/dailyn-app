package com.tara.dailyn.ui.features.analysis

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun AnalysisRoute(
    vm: AnalysisViewModel = viewModel()
) {
    val state = vm.state.collectAsStateWithLifecycle().value

    AnalysisScreen(state)
}