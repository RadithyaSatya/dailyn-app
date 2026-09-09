package com.tara.dailyn.data.repository.model

import com.tara.dailyn.data.local.model.FrequencyType

data class HabitCatalogItem(
    val id: String,
    val title: String,
    val description: String,
    val categoryId: Long?,
    val categoryName: String,
    val categoryIcon: String,
    val frequencyType: FrequencyType,
    val scheduleText: String
)
