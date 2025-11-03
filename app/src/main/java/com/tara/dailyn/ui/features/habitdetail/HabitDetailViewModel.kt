// ui/features/habitdetail/HabitDetailViewModel.kt
package com.tara.dailyn.ui.features.habitdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tara.dailyn.data.repository.HabitRepository
import com.tara.dailyn.ui.features.habitdetail.model.HabitDetailUi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate


data class RecentLogUi(val dateLabel: String, val done: Boolean)

class HabitDetailViewModel(
    private val repo: HabitRepository,
    private val habitId: String
) : ViewModel() {

    private val _ui = MutableStateFlow(HabitDetailUi(isLoading = true))
    val ui: StateFlow<HabitDetailUi> = _ui.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeHabitDetail(habitId)
                .combine(repo.observeLogs(habitId, daysBack = 30)) { h, logs ->
                    val streak = computeStreak(logs.map { it.date to (it.statusDone) })
                    val last7 = logs.takeLast(7)
                    HabitDetailUi(
                        id = h.id,
                        title = h.title,
                        description = h.description.orEmpty(),
                        scheduleText = h.scheduleText,
                        streak = streak,
                        completion7d = last7.count { it.statusDone },
                        recentLogs = logs.takeLast(10).map {
                            RecentLogUi(dateLabel = it.date.toString(), done = it.statusDone)
                        },
                        isLoading = false
                    )
                }
                .catch { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
                .collect { _ui.value = it }
        }
    }

    private fun computeStreak(pairs: List<Pair<LocalDate, Boolean>>): Int {
        if (pairs.isEmpty()) return 0
        val today = LocalDate.now()
        var d = today
        var count = 0
        val map = pairs.toMap()
        while (map[d] == true) { count++; d = d.minusDays(1) }
        return count
    }

    suspend fun deleteHabit(): Boolean {
        repo.deleteHabit(habitId)
        return true
    }
}
