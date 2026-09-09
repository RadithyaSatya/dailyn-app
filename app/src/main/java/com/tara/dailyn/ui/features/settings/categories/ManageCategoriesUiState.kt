package com.tara.dailyn.ui.features.settings.categories

data class ManageCategoryItemUi(
    val id: Long,
    val name: String,
    val icon: String
)

data class ManageCategoriesUiState(
    val items: List<ManageCategoryItemUi> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
