package com.tara.dailyn.data.local.dao

import androidx.room.*
import com.tara.dailyn.data.local.entity.*
import com.tara.dailyn.data.local.model.FrequencyType
import com.tara.dailyn.data.local.model.HabitRowProjection
import com.tara.dailyn.data.local.model.PeriodType
import com.tara.dailyn.data.local.relation.HabitWithRules
import kotlinx.coroutines.flow.Flow
import java.time.*

@Dao
interface HabitDao {

    // CRUD Habit
    @Upsert suspend fun upsertHabit(habit: HabitEntity)
    @Delete suspend fun deleteHabit(habit: HabitEntity)
    @Query("UPDATE habits SET isArchived = 1, updatedAt = :updatedAt WHERE id = :habitId")
    suspend fun archiveHabit(habitId: String, updatedAt: Instant)

    // Rules mutators
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWeeklyDays(list: List<HabitWeeklyDayEntity>)
    @Query("DELETE FROM habit_weekly_days WHERE habitId = :habitId")
    suspend fun clearWeeklyDays(habitId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlyDays(list: List<HabitMonthlyDayEntity>)
    @Query("DELETE FROM habit_monthly_days WHERE habitId = :habitId")
    suspend fun clearMonthlyDays(habitId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminders(list: List<HabitReminderEntity>)
    @Query("DELETE FROM habit_reminders WHERE habitId = :habitId")
    suspend fun clearReminders(habitId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitCategoryRefs(list: List<HabitCategoryCrossRef>)
    @Query("DELETE FROM habit_categories WHERE habitId = :habitId")
    suspend fun clearHabitCategoryRefs(habitId: String)

    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    suspend fun getHabitById(id: String): HabitEntity?
    // Transactional upsert (habit + rules)
    @Transaction
    suspend fun upsertHabitWithRules(
        habit: HabitEntity,
        weeklyDays: List<Int>,
        monthlyDays: List<Int>,
        reminders: List<LocalTime>,
        categoryIds: List<Long>
    ) {
        upsertHabit(habit)

        clearWeeklyDays(habit.id)
        if (weeklyDays.isNotEmpty())
            insertWeeklyDays(weeklyDays.map { HabitWeeklyDayEntity(habit.id, it) })

        clearMonthlyDays(habit.id)
        if (monthlyDays.isNotEmpty())
            insertMonthlyDays(monthlyDays.map { HabitMonthlyDayEntity(habit.id, it) })

        clearReminders(habit.id)
        if (reminders.isNotEmpty())
            insertReminders(reminders.map { HabitReminderEntity(habitId = habit.id, timeOfDay = it) })

        clearHabitCategoryRefs(habit.id)
        if (categoryIds.isNotEmpty())
            insertHabitCategoryRefs(categoryIds.map { HabitCategoryCrossRef(habit.id, it) })
    }

    // Reads
    @Transaction
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun observeAllHabitsWithRules(): Flow<List<HabitWithRules>>

    @Transaction
    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitWithRules(id: String): HabitWithRules?

    // DUE base queries
    @Query("""
        SELECT * FROM habits
        WHERE frequencyType = :everyDay
          AND isArchived = 0
          AND startDate <= :today
          AND (endDate IS NULL OR endDate >= :today)
    """)
    suspend fun getEveryDayDue(
        everyDay: FrequencyType = FrequencyType.EVERY_DAY,
        today: LocalDate
    ): List<HabitEntity>

    @Query("""
        SELECT h.* FROM habits h
        JOIN habit_weekly_days wd ON wd.habitId = h.id
        WHERE h.frequencyType = :customWeekly
          AND h.isArchived = 0
          AND wd.dayOfWeek = :dayOfWeek
          AND h.startDate <= :today
          AND (h.endDate IS NULL OR h.endDate >= :today)
    """)
    suspend fun getCustomWeeklyDue(
        customWeekly: FrequencyType = FrequencyType.CUSTOM_WEEKLY,
        dayOfWeek: Int,
        today: LocalDate
    ): List<HabitEntity>

    @Query("""
        SELECT h.* FROM habits h
        JOIN habit_monthly_days md ON md.habitId = h.id
        WHERE h.frequencyType = :specificDates
          AND h.isArchived = 0
          AND md.dayOfMonth = :dayOfMonth
          AND h.startDate <= :today
          AND (h.endDate IS NULL OR h.endDate >= :today) -- <— tambahkan h.
    """)
    suspend fun getSpecificDatesOfMonthDue(
        specificDates: FrequencyType = FrequencyType.SPECIFIC_DATES_OF_MONTH,
        dayOfMonth: Int,
        today: LocalDate
    ): List<HabitEntity>

    @Query("""
        SELECT 
            h.id AS id,
            h.title AS title,
            h.description AS description,
            EXISTS(
                SELECT 1 FROM habit_logs l
                WHERE l.habitId = h.id 
                  AND l.date = :date
                  AND l.occurIndex = :occurIndex
                  AND l.status = 'DONE'
            ) AS isCompleted
        FROM habits h
        WHERE h.isArchived = 0
        ORDER BY h.createdAt DESC
    """)
    fun observeHabitsWithStatus(
        date: LocalDate,
        occurIndex: Int = 0
    ): Flow<List<HabitRowProjection>>

    @Query("""
        SELECT 
            h.id AS id,
            h.title AS title,
            h.description AS description,
            EXISTS(
                SELECT 1 FROM habit_logs l
                WHERE l.habitId = h.id 
                  AND l.date = :date
                  AND l.occurIndex = :occurIndex
                  AND l.status = 'DONE'
            ) AS isCompleted
        FROM habits h
        WHERE h.isArchived = 0
          AND h.startDate <= :date
          AND (h.endDate IS NULL OR h.endDate >= :date)
        
          AND (
                -- 1️⃣ Every day: tampil setiap hari
                h.frequencyType = :everyDay
        
                -- 2️⃣ Custom weekly: tampil di hari sesuai rules
             OR (h.frequencyType = :customWeekly AND EXISTS (
                    SELECT 1 FROM habit_weekly_days wd
                    WHERE wd.habitId = h.id AND wd.dayOfWeek = :dow
                ))
        
                -- 3️⃣ Specific dates of month: tampil di tanggal tertentu
             OR (h.frequencyType = :specificDom AND EXISTS (
                    SELECT 1 FROM habit_monthly_days md
                    WHERE md.habitId = h.id AND md.dayOfMonth = :dom
                ))
        
                -- 4️⃣ Some days per period: kuota mingguan / bulanan
             OR (
                    h.frequencyType = :someDaysPerPeriod
                    AND h.someDaysCount IS NOT NULL
                    AND (
                          (
                              -- WEEK quota
                              h.periodType = :pWeek
                              AND (
                                    (SELECT COUNT(*) FROM habit_logs lw
                                     WHERE lw.habitId = h.id
                                       AND lw.status = 'DONE'
                                       AND lw.date BETWEEN :weekStart AND :weekEnd
                                    ) < h.someDaysCount
                                 OR EXISTS(
                                      SELECT 1 FROM habit_logs lw2
                                      WHERE lw2.habitId = h.id
                                        AND lw2.status = 'DONE'
                                        AND lw2.date = :date
                                 )
                              )
                          )
                          OR (
                              -- MONTH quota
                              h.periodType = :pMonth
                              AND (
                                    (SELECT COUNT(*) FROM habit_logs lm
                                     WHERE lm.habitId = h.id
                                       AND lm.status = 'DONE'
                                       AND lm.date BETWEEN :monthStart AND :monthEnd
                                    ) < h.someDaysCount
                                 OR EXISTS(
                                      SELECT 1 FROM habit_logs lm2
                                      WHERE lm2.habitId = h.id
                                        AND lm2.status = 'DONE'
                                        AND lm2.date = :date
                                 )
                              )
                          )
                    )
                )
          )
        ORDER BY h.createdAt DESC
    """)
    fun observeDueHabitsWithStatus(
        date: LocalDate,
        weekStart: LocalDate,
        weekEnd: LocalDate,
        monthStart: LocalDate,
        monthEnd: LocalDate,
        dow: Int,      // 1..7
        dom: Int,      // 1..31
        occurIndex: Int = 0,
        // Enum defaults
        everyDay: FrequencyType = FrequencyType.EVERY_DAY,
        customWeekly: FrequencyType = FrequencyType.CUSTOM_WEEKLY,
        specificDom: FrequencyType = FrequencyType.SPECIFIC_DATES_OF_MONTH,
        someDaysPerPeriod: FrequencyType = FrequencyType.SOME_DAYS_PER_PERIOD,
        pWeek: PeriodType = PeriodType.WEEK,
        pMonth: PeriodType = PeriodType.MONTH
    ): Flow<List<HabitRowProjection>>

    @Transaction
    suspend fun updateHabitWithRules(
        habit: HabitEntity,
        weeklyDays: List<Int>,
        monthlyDays: List<Int>,
        reminders: List<LocalTime>,
        categoryIds: List<String>
    ) {
        updateHabit(habit)
        deleteWeeklyDaysByHabit(habit.id)
        deleteMonthlyDaysByHabit(habit.id)
        deleteRemindersByHabit(habit.id)
        deleteHabitCategoriesByHabit(habit.id)

        insertWeeklyDays(habit.id, weeklyDays)       // helper @Insert(onConflict=REPLACE)
        insertMonthlyDays(habit.id, monthlyDays)
        insertReminders(habit.id, reminders)
        insertHabitCategories(habit.id, categoryIds)
    }

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabit(habitId: String): Int

    @Transaction
    suspend fun deleteHabitCascade(habitId: String) {
        deleteWeeklyDaysByHabit(habitId)
        deleteMonthlyDaysByHabit(habitId)
        deleteRemindersByHabit(habitId)
        deleteHabitCategoriesByHabit(habitId)
        // (opsional) juga hapus logs jika tidak ingin histori menggantung:
        // deleteLogsByHabit(habitId)
        deleteHabit(habitId)
    }

    @Query("DELETE FROM habit_weekly_days WHERE habitId = :habitId")
    suspend fun deleteWeeklyDaysByHabit(habitId: String)

    @Query("DELETE FROM habit_monthly_days WHERE habitId = :habitId")
    suspend fun deleteMonthlyDaysByHabit(habitId: String)

    @Query("DELETE FROM habit_reminders WHERE habitId = :habitId")
    suspend fun deleteRemindersByHabit(habitId: String)

    @Query("DELETE FROM habit_categories WHERE habitId = :habitId")
    suspend fun deleteHabitCategoriesByHabit(habitId: String)

    @Transaction
    suspend fun insertWeeklyDays(habitId: String, days: List<Int>) {
        if (days.isNotEmpty()) {
            insertWeeklyDays(
                days.map { day ->
                    HabitWeeklyDayEntity(
                        habitId = habitId,
                        dayOfWeek = day
                    )
                }
            )
        }
    }

    @Transaction
    suspend fun insertMonthlyDays(habitId: String, days: List<Int>) {
        if (days.isNotEmpty()) {
            insertMonthlyDays(
                days.map { dom ->
                    HabitMonthlyDayEntity(
                        habitId = habitId,
                        dayOfMonth = dom
                    )
                }
            )
        }
    }

    @Transaction
    suspend fun insertReminders(habitId: String, times: List<LocalTime>) {
        if (times.isNotEmpty()) {
            insertReminders(
                times.map { t ->
                    HabitReminderEntity(
                        habitId = habitId,
                        timeOfDay = t
                    )
                }
            )
        }
    }

    @Transaction
    suspend fun insertHabitCategories(habitId: String, categoryIds: List<String>) {
        if (categoryIds.isNotEmpty()) {
            insertHabitCategoryRefs(
                categoryIds.map { catId ->
                    HabitCategoryCrossRef(
                        habitId = habitId,
                        categoryId = catId.toLong()
                    )
                }
            )
        }
    }


    @Transaction
    @Query("SELECT * FROM habits WHERE id = :id LIMIT 1")
    fun observeHabitWithRules(id: String): Flow<HabitWithRules>

    @Query("""
    SELECT * FROM habit_logs 
    WHERE habitId = :habitId AND date BETWEEN :start AND :end
    ORDER BY date ASC
    """)

    fun observeRange(habitId: String, start: LocalDate, end: LocalDate): Flow<List<HabitLogEntity>>

}