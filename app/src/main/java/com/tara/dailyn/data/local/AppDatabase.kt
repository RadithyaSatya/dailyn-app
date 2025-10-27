package com.tara.dailyn.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.tara.dailyn.data.local.dao.*
import com.tara.dailyn.data.local.entity.*
import com.tara.dailyn.data.local.typeconverters.Converters

@Database(
    entities = [
        HabitEntity::class,
        HabitWeeklyDayEntity::class,
        HabitMonthlyDayEntity::class,
        HabitReminderEntity::class,
        HabitLogEntity::class,
        CategoryEntity::class,
        HabitCategoryCrossRef::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun categoryDao(): CategoryDao

    // di class AppDatabase (file yang sama)
    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dailyn.db"
                ).build().also { INSTANCE = it }
            }
    }

}

