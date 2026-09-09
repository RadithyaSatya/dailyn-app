package com.tara.dailyn.data.local.model

data class HabitRowProjection(
    val id: String,
    val title: String,
    val description: String,
    val categoryName: String?,
    val categoryIcon: String?,
    val isCompleted: Boolean
)
