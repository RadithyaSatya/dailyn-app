package com.tara.dailyn.data.local.db

import android.content.Context
import androidx.room.migration.Migration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
    abstract fun categoryDao(): CategoryDao

    companion object {
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habits ADD COLUMN homeSortOrder INTEGER")
            }
        }

        @Volatile private var INSTANCE: AppDatabase? = null
        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "dailyn.db"
                ).addMigrations(MIGRATION_3_4)
                    .build()
                    .also { INSTANCE = it }
            }
    }

}
