package com.tara.dailyn.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.tara.dailyn.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {
    @Insert suspend fun insertHabit(h: HabitEntity): Long
    @Update suspend fun updateHabit(h: HabitEntity)
    @Delete suspend fun deleteHabit(h: HabitEntity)

    @Query("SELECT * FROM habits WHERE archived = 0 ORDER BY id DESC")
    fun getActiveHabits(): Flow<List<HabitEntity>>


}