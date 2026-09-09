package com.tara.dailyn.ui.features.habits.model

data class HabitListItemUi(
    val id: String,
    val title: String,
    val description: String,
    val categoryId: Long?,
    val categoryName: String,
    val categoryIcon: String,
    val typeLabel: String,
    val scheduleText: String
)

enum class HabitTypeFilterUi(val label: String) {
    ALL("All"),
    EVERY_DAY("Every day"),
    SPECIFIC_DAYS_OF_WEEK("Weekly"),
    SPECIFIC_DAY_OF_MONTH("Monthly"),
    SOME_DAYS_PER_PERIOD("Per period")
}

data class HabitsUiState(
    val items: List<HabitListItemUi> = emptyList(),
    val categories: List<Pair<Long, String>> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedType: HabitTypeFilterUi = HabitTypeFilterUi.ALL,
    val isLoading: Boolean = true
)
