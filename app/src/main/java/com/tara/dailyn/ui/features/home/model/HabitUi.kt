package com.tara.dailyn.ui.features.home.model

data class HabitUi(
    val id: String,
    val title: String,
    val description: String,
    val categoryName: String,
    val categoryIcon: String,
    val isCompletedToday: Boolean
)
