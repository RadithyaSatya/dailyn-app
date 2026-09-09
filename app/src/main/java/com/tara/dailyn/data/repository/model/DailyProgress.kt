package com.tara.dailyn.data.repository.model

import java.time.LocalDate

data class DailyProgress(
    val date: LocalDate,
    val completedCount: Int,
    val totalCount: Int
) {
    val progressFraction: Float
        get() = if (totalCount == 0) 0f else completedCount.toFloat() / totalCount.toFloat()

    val progressPercent: Int
        get() = (progressFraction * 100).toInt()
}
