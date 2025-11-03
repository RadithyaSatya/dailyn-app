package com.tara.dailyn.ui.features.addhabit.model

import java.time.DayOfWeek
import java.time.LocalTime

enum class FrequencyType { EVERY_DAY, SPECIFIC_DAYS_OF_WEEK, SPECIFIC_DAY_OF_MONTH, SOME_DAYS_PER_PERIOD }
enum class PeriodType { WEEK, MONTH }
data class AddHabitUiState (
    val title: String = "",
    val description: String = "",
    val frequencyType: FrequencyType = FrequencyType.EVERY_DAY,
    val selectedDaysOfWeek: Set<DayOfWeek> = emptySet(),
    val specificDayOfMonth: Int? = null,
    val someDaysCount: Int? = 1,
    val specificDaysOfMonth: Set<Int> = emptySet(),
    val periodType: PeriodType = PeriodType.WEEK,
    val reminderEnabled: Boolean = true,
    val reminderTime: LocalTime? = null,
    val isSaving: Boolean = false,
    val titleError: String? = null
)