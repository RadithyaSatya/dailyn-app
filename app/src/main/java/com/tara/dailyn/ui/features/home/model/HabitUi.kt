package com.tara.dailyn.ui.features.home.model

data class HabitUi(
    val id: Long,
    val title: String,
    val description: String,
    val isCompletedToday: Boolean
)