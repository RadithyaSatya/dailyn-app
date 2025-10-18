package com.tara.dailyn.ui.features.analysis

import androidx.lifecycle.ViewModel
import com.tara.dailyn.ui.features.analysis.model.AnalysisUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AnalysisViewModel : ViewModel() {
    private val _state = MutableStateFlow(
        AnalysisUiState()
    )
    val state : StateFlow<AnalysisUiState> = _state
}