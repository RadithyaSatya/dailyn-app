package com.tara.dailyn.ui.features.home.model

import java.time.LocalDate

sealed interface HomeEvent {
    data class SelectDate(val date: LocalDate) : HomeEvent
    data object AddHabit : HomeEvent
    data class ToggleHabit(val id: Long) : HomeEvent
}