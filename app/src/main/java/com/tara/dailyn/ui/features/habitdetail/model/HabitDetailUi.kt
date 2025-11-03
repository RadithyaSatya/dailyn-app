package com.tara.dailyn.ui.features.habitdetail.model

import com.tara.dailyn.ui.features.habitdetail.RecentLogUi

data class HabitDetailUi(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val scheduleText: String = "",
    val streak: Int = 0,
    val completion7d: Int = 0,
    val recentLogs: List<RecentLogUi> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)