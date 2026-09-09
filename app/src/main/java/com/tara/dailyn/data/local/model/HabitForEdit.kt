// ui/features/addhabit/model/HabitForEdit.kt (atau di repository package)
package com.tara.dailyn.ui.features.addhabit.model

import java.time.DayOfWeek
import java.time.LocalTime

data class HabitForEdit(
    val id: String,
    val title: String,
    val description: String?,
    val selectedCategoryId: Long?,
    val uiFrequencyType: FrequencyType,
    val uiPeriodType: PeriodType,            // WEEK/MONTH (dipakai kalau SOME_DAYS_PER_PERIOD)
    val selectedDaysOfWeek: List<DayOfWeek>, // 1..7
    val specificDaysOfMonth: List<Int>,      // 1..31
    val someDaysCount: Int?,
    val reminderEnabled: Boolean,
    val reminderTime: LocalTime?
)
