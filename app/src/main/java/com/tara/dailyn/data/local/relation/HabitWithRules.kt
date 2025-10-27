package com.tara.dailyn.data.local.relation

import androidx.room.*
import com.tara.dailyn.data.local.entity.*

data class HabitWithRules(
    @Embedded val habit: HabitEntity,

    @Relation(parentColumn = "id", entityColumn = "habitId")
    val weeklyDays: List<HabitWeeklyDayEntity> = emptyList(),

    @Relation(parentColumn = "id", entityColumn = "habitId")
    val monthlyDays: List<HabitMonthlyDayEntity> = emptyList(),

    @Relation(parentColumn = "id", entityColumn = "habitId")
    val reminders: List<HabitReminderEntity> = emptyList(),

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = HabitCategoryCrossRef::class,
            parentColumn = "habitId",
            entityColumn = "categoryId"
        )
    )
    val categories: List<CategoryEntity> = emptyList()
)
