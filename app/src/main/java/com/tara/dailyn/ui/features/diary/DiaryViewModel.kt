package com.tara.dailyn.ui.features.diary

import androidx.lifecycle.ViewModel
import com.tara.dailyn.ui.features.diary.model.DiaryUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class DiaryViewModel : ViewModel() {
    private val _state = MutableStateFlow(
        DiaryUiState()
    )
    val state : StateFlow<DiaryUiState> = _state
}