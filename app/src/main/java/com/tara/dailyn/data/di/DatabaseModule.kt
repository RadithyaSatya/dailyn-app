package com.tara.dailyn.data

import android.content.Context
import androidx.room.Room
import com.tara.dailyn.data.local.db.AppDatabase
import com.tara.dailyn.data.repository.HabitRepository

object DependencyProvider {

    @Volatile private var database: AppDatabase? = null

    fun provideDatabase(context: Context): AppDatabase {
        return database ?: synchronized(this) {
            database ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "dailyn.db"
            ).build().also { database = it }
        }
    }

    fun provideHabitRepository(context: Context): HabitRepository {
        val db = provideDatabase(context)
        return HabitRepository(db.categoryDao(), db.habitDao(), db.habitLogDao(), context.applicationContext)
    }
}
