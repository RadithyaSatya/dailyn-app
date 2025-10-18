package com.tara.dailyn.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName= "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    val createdAt: Long = System.currentTimeMillis(),
    val archived: Boolean = false
)
