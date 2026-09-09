package com.tara.dailyn.ui.features.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tara.dailyn.data.repository.HabitRepository
import com.tara.dailyn.ui.features.analysis.model.AnalysisUiState
import com.tara.dailyn.ui.features.analysis.model.DailyProgressUi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class AnalysisViewModel(
    private val repo: HabitRepository
) : ViewModel() {

    private val selectedWeekStart = MutableStateFlow(currentWeekStart())

    val state: StateFlow<AnalysisUiState> =
        selectedWeekStart
            .flatMapLatest { weekStart ->
                repo.observeWeeklyProgress(weekStart).combine(selectedWeekStart) { progress, selected ->
                    val items = progress.map {
                        DailyProgressUi(
                            date = it.date,
                            label = it.date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            completedCount = it.completedCount,
                            totalCount = it.totalCount,
                            progressFraction = it.progressFraction,
                            progressPercent = it.progressPercent
                        )
                    }
                    val completed = items.sumOf { it.completedCount }
                    val total = items.sumOf { it.totalCount }

                    AnalysisUiState(
                        selectedWeekStart = selected,
                        weekProgress = items,
                        weekCompletedCount = completed,
                        weekTotalCount = total,
                        weekProgressPercent = if (total == 0) 0 else (completed * 100) / total,
                        isLoading = false
                    )
                }
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = AnalysisUiState(selectedWeekStart = currentWeekStart(), isLoading = true)
            )

    fun showPreviousWeek() {
        selectedWeekStart.update { it.minusWeeks(1) }
    }

    fun showNextWeek() {
        selectedWeekStart.update { current ->
            minOf(current.plusWeeks(1), currentWeekStart())
        }
    }

    companion object {
        fun currentWeekStart(today: LocalDate = LocalDate.now()): LocalDate =
            today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    }
}
