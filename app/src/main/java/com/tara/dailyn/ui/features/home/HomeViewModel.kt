package com.tara.dailyn.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tara.dailyn.data.preferences.AppSettings
import com.tara.dailyn.ui.features.home.model.HabitUi
import com.tara.dailyn.ui.features.home.model.HomeEvent
import com.tara.dailyn.ui.features.home.model.HomeUiState
import com.tara.dailyn.data.repository.HabitRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(
    private val repo: HabitRepository,
    private val appSettings: AppSettings
) : ViewModel() {

    private val _state = MutableStateFlow(
        HomeUiState(
            items = emptyList(),
            selectedDate = LocalDate.now()
        )
    )
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            appSettings.allowPreviousDayEdits.collect { allowed ->
                _state.update { it.copy(allowPreviousDayEdits = allowed) }
            }
        }

        viewModelScope.launch {
            state
                .map { it.selectedDate }
                .distinctUntilChanged()
                .flatMapLatest { date ->
                    repo.observeHomeRows(date = date, occurIndex = 0) // ganti occurIndex kalau perlu
                }
                .collect { rows: List<HabitUi> ->
                    _state.update { it.copy(items = rows) }
                }
        }
    }

    fun onEvent(event: HomeEvent) {
        when (event) {
            is HomeEvent.SelectDate -> {
                _state.update { it.copy(selectedDate = event.date) }
            }
            HomeEvent.AddHabit -> {
            }
            is HomeEvent.ToggleHabit -> toggleHabit(event.id)
            is HomeEvent.ReorderPendingHabits -> reorderPendingHabits(event.orderedIds)
        }
    }

    private fun toggleHabit(id: String) = viewModelScope.launch {
        val date = _state.value.selectedDate
        repo.toggle(
            habitId = id,
            date = date,
            occurIndex = 0,
            allowPreviousDayEdits = _state.value.allowPreviousDayEdits
        )
    }

    private fun reorderPendingHabits(orderedIds: List<String>) = viewModelScope.launch {
        if (orderedIds.isEmpty()) return@launch
        repo.reorderHomeHabits(orderedIds)
    }
}
