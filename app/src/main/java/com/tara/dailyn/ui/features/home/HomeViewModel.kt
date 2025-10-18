package com.tara.dailyn.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tara.dailyn.ui.features.home.model.HabitUi
import com.tara.dailyn.ui.features.home.model.HomeEvent
import com.tara.dailyn.ui.features.home.model.HomeUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HomeViewModel: ViewModel() {
    private val _state = MutableStateFlow(
        HomeUiState(
            items = listOf(
                HabitUi(1, "Minum Air 8 Gelas", "Hydration harian", false),
                HabitUi(2, "Olahraga 20 menit", "Jogging ringan", true),
                HabitUi(3, "Baca 10 halaman", "Buku apa saja", false),
                HabitUi(4, "Minum Air 8 Gelas", "Hydration harian", false),
                HabitUi(5, "Olahraga 20 menit", "Jogging ringan", true),
                HabitUi(6, "Baca 10 halaman", "Buku apa saja", false),
                HabitUi(7, "Minum Air 8 Gelas", "Hydration harian", false),
                HabitUi(8, "Olahraga 20 menit", "Jogging ringan", true),
                HabitUi(9, "Baca 10 halaman", "Buku apa saja", false),
            )
        )
    )

    val state: StateFlow<HomeUiState> = _state

    private var nextId = (_state.value.items.maxOfOrNull { it.id } ?: 0L) + 1

    fun onEvent(event: HomeEvent) {
        when(event){
            is HomeEvent.SelectDate -> _state.update { it.copy(selectedDate = event.date) }
            HomeEvent.AddHabit -> addHabit()
            is HomeEvent.ToggleHabit -> toggleHabit(event.id)
        }
    }

    private fun addHabit() = viewModelScope.launch {
        _state.update { s ->
            val newItem = HabitUi(
                id = nextId++,
                title = "Kebiasaan Baru #${nextId - 1}",
                description = "Deskripsi default",
                isCompletedToday = false
            )
            s.copy(items = s.items + newItem)
        }
    }

    private fun toggleHabit(id : Long) = viewModelScope.launch {
        _state.update { s ->
            s.copy(items = s.items.map { if(it.id == id) it.copy(isCompletedToday = !it.isCompletedToday) else it })
        }
    }
}