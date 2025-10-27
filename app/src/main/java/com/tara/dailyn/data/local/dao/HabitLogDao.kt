package com.tara.dailyn.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.tara.dailyn.data.local.entity.HabitLogEntity
import com.tara.dailyn.data.local.model.LogStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate

@Dao
interface HabitLogDao {
    @Upsert
    suspend fun upsertLog(log: HabitLogEntity)

    @Query("""
        SELECT * FROM habit_logs
        WHERE habitId = :habitId AND date = :date AND occurIndex = :occurIndex
        LIMIT 1
    """)
    suspend fun findOne(
        habitId: String,
        date: LocalDate,
        occurIndex: Int
    ): HabitLogEntity?

    @Update
    suspend fun update(entity: HabitLogEntity)

    // cepat buat toggle tanpa fetch-full:
    @Query("""
        UPDATE habit_logs
        SET status = :status, completedAt = :completedAt
        WHERE id = :id
    """)
    suspend fun updateStatus(
        id: Long,
        status: LogStatus,
        completedAt: Instant?
    )

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(habitLog: HabitLogEntity): Long

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteByHabitAndDate(habitId: Long, date: LocalDate): Int

    @Query("SELECT COUNT(*) FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun countByHabitAndDate(habitId: Long, date: LocalDate): Int

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND date = :date")
    suspend fun deleteLogsForDate(habitId: String, date: LocalDate)

    @Query("""
        SELECT * FROM habit_logs
        WHERE habitId = :habitId
          AND date BETWEEN :from AND :to
        ORDER BY date ASC, occurIndex ASC
    """)
    fun observeLogs(habitId: String, from: LocalDate, to: LocalDate): Flow<List<HabitLogEntity>>

    @Query("""
        SELECT COUNT(*) FROM habit_logs
        WHERE habitId = :habitId AND status = :done AND date BETWEEN :from AND :to
    """)
    suspend fun countDoneInRange(
        habitId: String,
        done: LogStatus = LogStatus.DONE,
        from: LocalDate,
        to: LocalDate
    ): Int
}
