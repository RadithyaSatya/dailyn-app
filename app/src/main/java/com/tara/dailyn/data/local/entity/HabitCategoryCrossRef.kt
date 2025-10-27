package com.tara.dailyn.data.local.entity

import androidx.room.*

@Entity(
    tableName = "habit_categories",
    primaryKeys = ["habitId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("habitId"), Index("categoryId")]
)
data class HabitCategoryCrossRef(
    val habitId: String,
    val categoryId: Long
)
