package com.tara.dailyn.ui.features.analysis.model

import java.time.LocalDate

data class DailyProgressUi(
    val date: LocalDate,
    val label: String,
    val completedCount: Int,
    val totalCount: Int,
    val progressFraction: Float,
    val progressPercent: Int
)

data class AnalysisUiState(
    val selectedWeekStart: LocalDate = LocalDate.now(),
    val weekProgress: List<DailyProgressUi> = emptyList(),
    val weekCompletedCount: Int = 0,
    val weekTotalCount: Int = 0,
    val weekProgressPercent: Int = 0,
    val isLoading: Boolean = true
)
