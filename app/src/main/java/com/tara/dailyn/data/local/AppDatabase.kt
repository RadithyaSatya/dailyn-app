package com.tara.dailyn.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tara.dailyn.data.local.dao.HabitDao
import com.tara.dailyn.data.local.entity.HabitCompletionEntity
import com.tara.dailyn.data.local.entity.HabitEntity

@Database(
    entities = [HabitEntity::class, HabitCompletionEntity::class],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
}