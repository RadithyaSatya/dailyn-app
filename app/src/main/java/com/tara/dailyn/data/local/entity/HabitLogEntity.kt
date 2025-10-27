package com.tara.dailyn.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tara.dailyn.data.local.model.LogStatus
import java.time.*

@Entity(
    tableName = "habit_logs",
    indices = [
        Index(value = ["habitId", "date"]),
        Index(value = ["habitId", "date", "occurIndex"], unique = true)
    ]
)
data class HabitLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val habitId: String,
    val date: LocalDate,
    val occurIndex: Int = 0,
    val status: LogStatus = LogStatus.PLANNED,
    val completedAt: Instant? = null,
    val note: String? = null,
    val value: Double? = null,
    val score: Double? = null
)
