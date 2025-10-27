package com.tara.dailyn.data.local.entity

import androidx.room.*
import java.time.LocalTime

@Entity(
    tableName = "habit_reminders",
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("habitId"), Index("timeOfDay")]
)
data class HabitReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: String,
    val timeOfDay: LocalTime,
    val enabled: Boolean = true,
    val daysMask: Int? = null,
    val notificationChannel: String? = null
)
