package com.tara.dailyn.data.local.entity

import androidx.room.*

@Entity(
    tableName = "habit_monthly_days",
    primaryKeys = ["habitId", "dayOfMonth"],
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId")]
)
data class HabitMonthlyDayEntity(
    val habitId: String,
    val dayOfMonth: Int
)
