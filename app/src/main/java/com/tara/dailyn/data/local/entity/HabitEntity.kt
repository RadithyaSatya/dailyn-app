package com.tara.dailyn.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.tara.dailyn.data.local.model.*
import java.time.*

@Entity(
    tableName = "habits",
    indices = [Index("title"), Index("isArchived")]
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String? = null,

    val frequencyType: FrequencyType,
    val periodType: PeriodType? = null,
    val someDaysCount: Int? = null,

    val overflowPolicy: OverflowPolicy? = OverflowPolicy.SHIFT_TO_LAST_DAY,

    val startDate: LocalDate,
    val endDate: LocalDate? = null,

    val defaultTimeOfDay: LocalTime? = null,
    val color: String? = null,
    val icon: String? = null,
    val isArchived: Boolean = false,

    val createdAt: Instant,
    val updatedAt: Instant
)
