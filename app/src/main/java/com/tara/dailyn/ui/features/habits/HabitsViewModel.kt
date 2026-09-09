package com.tara.dailyn.ui.features.habits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tara.dailyn.data.local.model.FrequencyType
import com.tara.dailyn.data.repository.HabitRepository
import com.tara.dailyn.ui.features.habits.model.HabitListItemUi
import com.tara.dailyn.ui.features.habits.model.HabitTypeFilterUi
import com.tara.dailyn.ui.features.habits.model.HabitsUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class HabitsViewModel(
    private val repo: HabitRepository
) : ViewModel() {

    private val selectedCategoryId = MutableStateFlow<Long?>(null)
    private val selectedType = MutableStateFlow(HabitTypeFilterUi.ALL)

    private val _state = MutableStateFlow(HabitsUiState())
    val state: StateFlow<HabitsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repo.observeHabitCatalog(),
                repo.observeCategoryOptions(),
                selectedCategoryId,
                selectedType
            ) { habits, categories, categoryId, type ->
                val visibleCategoryIds = categories.map { it.id }.toSet()
                val effectiveCategoryId =
                    if (categoryId == null || categoryId in visibleCategoryIds) categoryId else null

                val filteredItems = habits
                    .filter { effectiveCategoryId == null || it.categoryId == effectiveCategoryId }
                    .filter { item -> matchesType(item.frequencyType, type) }
                    .map { item ->
                        HabitListItemUi(
                            id = item.id,
                            title = item.title,
                            description = item.description,
                            categoryId = item.categoryId,
                            categoryName = item.categoryName,
                            categoryIcon = item.categoryIcon,
                            typeLabel = typeLabel(item.frequencyType),
                            scheduleText = item.scheduleText
                        )
                    }

                HabitsUiState(
                    items = filteredItems,
                    categories = categories.map { it.id to it.name },
                    selectedCategoryId = effectiveCategoryId,
                    selectedType = type,
                    isLoading = false
                )
            }.collect { _state.value = it }
        }
    }

    fun selectCategory(categoryId: Long?) {
        selectedCategoryId.value = categoryId
    }

    fun selectType(type: HabitTypeFilterUi) {
        selectedType.value = type
    }

    private fun matchesType(
        frequencyType: FrequencyType,
        filter: HabitTypeFilterUi
    ): Boolean {
        return when (filter) {
            HabitTypeFilterUi.ALL -> true
            HabitTypeFilterUi.EVERY_DAY -> frequencyType == FrequencyType.EVERY_DAY
            HabitTypeFilterUi.SPECIFIC_DAYS_OF_WEEK -> frequencyType == FrequencyType.CUSTOM_WEEKLY
            HabitTypeFilterUi.SPECIFIC_DAY_OF_MONTH -> frequencyType == FrequencyType.SPECIFIC_DATES_OF_MONTH
            HabitTypeFilterUi.SOME_DAYS_PER_PERIOD -> frequencyType == FrequencyType.SOME_DAYS_PER_PERIOD
        }
    }

    private fun typeLabel(frequencyType: FrequencyType): String {
        return when (frequencyType) {
            FrequencyType.EVERY_DAY -> "Every day"
            FrequencyType.CUSTOM_WEEKLY -> "Weekly"
            FrequencyType.SPECIFIC_DATES_OF_MONTH -> "Monthly"
            FrequencyType.SOME_DAYS_PER_PERIOD -> "Per period"
        }
    }
}
