package com.tara.dailyn.ui.features.home.model

import java.time.LocalDate

data class HomeUiState(
    val selectedDate: LocalDate = LocalDate.now(),
    val items: List<HabitUi> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)