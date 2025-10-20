package com.tara.dailyn.ui.features.addhabit.model

import java.time.DayOfWeek
import java.time.LocalTime

sealed interface AddHabitEvent {
    data class TitleChanged(val value: String) : AddHabitEvent
    data class DescriptionChanged(val value: String) : AddHabitEvent
    data class FrequencyChanged(val type: FrequencyType) : AddHabitEvent
    data class ToggleDayOfWeek(val day: DayOfWeek) : AddHabitEvent
    data class SomeDaysCountChanged(val count: Int?) : AddHabitEvent
    data class PeriodChanged(val period: PeriodType) : AddHabitEvent
    data class ReminderEnabledChanged(val enabled: Boolean) : AddHabitEvent
    data class ReminderTimeChanged(val time: LocalTime?) : AddHabitEvent
    data class ToggleSpecificDayOfMonth(val day: Int) : AddHabitEvent
    data object SaveClicked : AddHabitEvent
    data object CancelClicked : AddHabitEvent
}