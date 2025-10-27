package com.tara.dailyn.data.local.entity

import androidx.room.*

@Entity(
    tableName = "habit_weekly_days",
    primaryKeys = ["habitId", "dayOfWeek"],
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId")]
)
data class HabitWeeklyDayEntity(
    val habitId: String,
    val dayOfWeek: Int
)
