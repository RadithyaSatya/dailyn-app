package com.tara.dailyn.ui.features.habitdetail.model

import java.time.YearMonth

enum class HabitCalendarDayState {
    OUTSIDE_MONTH,
    FUTURE,
    IDLE,
    SCHEDULED,
    DONE,
    MISSED
}

data class HabitCalendarDayUi(
    val dayOfMonth: Int,
    val state: HabitCalendarDayState
)

data class HabitDetailUi(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val scheduleText: String = "",
    val streak: Int = 0,
    val visibleMonth: YearMonth = YearMonth.now(),
    val canGoToPreviousMonth: Boolean = false,
    val canGoToNextMonth: Boolean = false,
    val calendarDays: List<HabitCalendarDayUi> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
