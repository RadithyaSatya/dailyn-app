package com.tara.dailyn.data.local.model

data class HabitRowProjection(
    val id: String,
    val title: String,
    val description: String,
    val isCompleted: Boolean
)