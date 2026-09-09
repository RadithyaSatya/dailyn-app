package com.tara.dailyn.ui.features.settings.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tara.dailyn.data.repository.HabitRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ManageCategoriesViewModel(
    private val repo: HabitRepository
) : ViewModel() {
    private val _state = MutableStateFlow(ManageCategoriesUiState())
    val state: StateFlow<ManageCategoriesUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repo.ensureDefaultCategories()
            repo.observeManageableCategories().collect { categories ->
                _state.update {
                    it.copy(
                        items = categories.map { category ->
                            ManageCategoryItemUi(
                                id = category.id,
                                name = category.name,
                                icon = category.icon
                            )
                        },
                        isLoading = false,
                        error = null
                    )
                }
            }
        }
    }

    fun updateCategory(
        categoryId: Long,
        name: String,
        icon: String
    ) = viewModelScope.launch {
        runCatching {
            repo.updateCategory(categoryId, name, icon)
        }.onFailure { error ->
            _state.update { it.copy(error = error.message ?: "Failed to update category.") }
        }
    }

    fun deleteCategory(categoryId: Long) = viewModelScope.launch {
        runCatching {
            repo.deleteCategory(categoryId)
        }.onFailure { error ->
            _state.update { it.copy(error = error.message ?: "Failed to delete category.") }
        }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }
}
