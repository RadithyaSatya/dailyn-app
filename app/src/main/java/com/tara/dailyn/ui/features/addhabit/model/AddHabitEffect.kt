package com.tara.dailyn.ui.features.addhabit.model

import java.time.DayOfWeek
import java.time.LocalTime

sealed interface AddHabitEffect {
    data object Cancelled : AddHabitEffect
    data object ValidationError : AddHabitEffect
    data class Saved(
        val id: String,
        val title: String,
        val description: String,
        val frequencyType: FrequencyType,
        val selectedDaysOfWeek: Set<DayOfWeek>,
        val someDaysCount: Int?,
        val periodType: PeriodType,
        val reminderEnabled: Boolean,
        val reminderTime: LocalTime?,
        val specificDaysOfMonth: Set<Int>,
    ) : AddHabitEffect
}