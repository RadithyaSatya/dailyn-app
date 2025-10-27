package com.tara.dailyn.data.repository

import com.tara.dailyn.data.local.dao.HabitDao
import com.tara.dailyn.data.local.dao.HabitLogDao
import com.tara.dailyn.data.local.entity.HabitEntity
import com.tara.dailyn.data.local.entity.HabitLogEntity
import com.tara.dailyn.data.local.model.LogStatus
import com.tara.dailyn.data.local.model.OverflowPolicy
import com.tara.dailyn.data.local.model.PeriodType
import com.tara.dailyn.ui.features.home.model.HabitUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.*
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlin.collections.map
import com.tara.dailyn.data.local.model.FrequencyType as DbFreq
import com.tara.dailyn.ui.features.addhabit.model.FrequencyType as UiFreq
import com.tara.dailyn.ui.features.addhabit.model.PeriodType as UiPeriod

class HabitRepository(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao
) {
    // === DUE LIST ===
    suspend fun getDueHabitsForDate(today: LocalDate): List<HabitEntity> = withContext(Dispatchers.IO) {
        val dow = DayOfWeek.from(today).value  // 1..7
        val dom = today.dayOfMonth

        val every = habitDao.getEveryDayDue(today = today)
        val weekly = habitDao.getCustomWeeklyDue(dayOfWeek = dow, today = today)
        val monthlyMulti = habitDao.getSpecificDatesOfMonthDue(dayOfMonth = dom, today = today)

        (every + weekly + monthlyMulti).distinctBy { it.id }
    }

    // === QUOTA helper for SOME_DAYS_PER_PERIOD ===
    suspend fun getRemainingQuota(
        habit: HabitEntity,
        refDate: LocalDate
    ): Int? = withContext(Dispatchers.IO) {
        val target = habit.someDaysCount ?: return@withContext null
        val (start, end) = when (habit.periodType) {
            PeriodType.WEEK -> {
                val monday = refDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                val sunday = refDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                monday to sunday
            }
            PeriodType.MONTH -> {
                val ym = YearMonth.from(refDate)
                ym.atDay(1) to ym.atEndOfMonth()
            }

            null -> {
                return@withContext null
            }
        }
        val done = habitLogDao.countDoneInRange(habit.id, LogStatus.DONE, start, end)
        (target - done).coerceAtLeast(0)
    }

    // === Overflow policy util (kalau nanti kamu butuh handle generate tanggal 29-31) ===
    fun isOverflowDayForMonth(day: Int, date: LocalDate): Boolean {
        return day > YearMonth.from(date).lengthOfMonth()
    }

    fun matchesOverflowPolicy(habit: HabitEntity, date: LocalDate, requestedDay: Int): Boolean {
        val lastDay = YearMonth.from(date).lengthOfMonth()
        if (requestedDay <= lastDay) return true
        return when (habit.overflowPolicy ?: OverflowPolicy.SHIFT_TO_LAST_DAY) {
            OverflowPolicy.SHIFT_TO_LAST_DAY -> date.dayOfMonth == lastDay
            OverflowPolicy.SKIP -> false
            OverflowPolicy.ROLL_TO_NEXT_MONTH -> false // jadwalkan bulan depan (di scheduler)
        }
    }

    suspend fun createHabit(
        title: String,
        description: String,
        uiFrequency: UiFreq,
        selectedDaysOfWeek: Set<DayOfWeek>,
        specificDaysOfMonth: Set<Int>,
        someDaysCount: Int?,
        uiPeriodType: UiPeriod,
        reminderEnabled: Boolean,
        reminderTime: LocalTime?
    ): String = withContext(Dispatchers.IO) {
        val now: Instant = Instant.now()
        val today: LocalDate = LocalDate.now()

        val id = UUID.randomUUID().toString()

        // Map UI → DB enums
        val dbFrequency: DbFreq = when (uiFrequency) {
            UiFreq.EVERY_DAY -> DbFreq.EVERY_DAY
            UiFreq.SPECIFIC_DAYS_OF_WEEK -> DbFreq.CUSTOM_WEEKLY
            UiFreq.SPECIFIC_DAY_OF_MONTH -> DbFreq.SPECIFIC_DATES_OF_MONTH
            UiFreq.SOME_DAYS_PER_PERIOD -> DbFreq.SOME_DAYS_PER_PERIOD
        }

        if (dbFrequency == DbFreq.SOME_DAYS_PER_PERIOD) {
            if (someDaysCount == null || someDaysCount <= 0) {
                throw IllegalStateException("Mohon isi jumlah target per periode (>0).")
            }
        }

        val dbPeriod: PeriodType? = when {
            dbFrequency == DbFreq.SOME_DAYS_PER_PERIOD -> when (uiPeriodType) {
                UiPeriod.WEEK -> PeriodType.WEEK
                UiPeriod.MONTH -> PeriodType.MONTH
            }
            else -> null
        }

        if (dbFrequency == DbFreq.SOME_DAYS_PER_PERIOD) {
            requireNotNull(someDaysCount) { "Mohon isi jumlah target per periode (>0)." }
            require(someDaysCount > 0) { "Mohon isi jumlah target per periode (>0)." }
            require(dbPeriod != null) { "Mohon pilih periode (Mingguan/Bulanan)." }
        }


        val finalSomeDaysCount: Int? =
            if (dbFrequency == DbFreq.SOME_DAYS_PER_PERIOD) someDaysCount else null


        val habit = HabitEntity(
            id = id,
            title = title.trim(),
            description = description.trim(),
            frequencyType = dbFrequency,
            periodType = dbPeriod,
            someDaysCount = finalSomeDaysCount,
            startDate = today,
            endDate = null,
            isArchived = false,
            createdAt = now,
            updatedAt = now
        )

        val weeklyDaysInts =
            if (dbFrequency == DbFreq.CUSTOM_WEEKLY)
                selectedDaysOfWeek.map { it.value }  // 1..7
            else emptyList()

        val monthlyDaysInts =
            if (dbFrequency == DbFreq.SPECIFIC_DATES_OF_MONTH)
                specificDaysOfMonth.sorted()         // 1..31
            else emptyList()

        val reminders =
            if (reminderEnabled && reminderTime != null) listOf(reminderTime) else emptyList()

        habitDao.upsertHabitWithRules(
            habit = habit,
            weeklyDays = weeklyDaysInts,
            monthlyDays = monthlyDaysInts,
            reminders = reminders,
            categoryIds = emptyList()
        )
        id
    }

    fun observeHomeRows(date: LocalDate, occurIndex: Int = 0): Flow<List<HabitUi>> {
        val dow = date.dayOfWeek.value                 // 1..7
        val dom = date.dayOfMonth                      // 1..31

        val weekStart = date.minusDays((dow - 1).toLong())
        val weekEnd = weekStart.plusDays(6)

        val ym = java.time.YearMonth.from(date)
        val monthStart = ym.atDay(1)
        val monthEnd = ym.atEndOfMonth()

        return habitDao.observeDueHabitsWithStatus(
            date = date,
            weekStart = weekStart,
            weekEnd = weekEnd,
            monthStart = monthStart,
            monthEnd = monthEnd,
            dow = dow,
            dom = dom,
            occurIndex = occurIndex
        ).map { rows ->
            rows.map { r ->
                HabitUi(
                    id = r.id,
                    title = r.title,
                    description = r.description.orEmpty(),
                    isCompletedToday = r.isCompleted
                )
            }
        }
    }

    suspend fun toggle(habitId: String, date: LocalDate, occurIndex: Int = 0) {
        val existing = habitLogDao.findOne(habitId, date, occurIndex)
        if (existing == null) {
            // belum ada, create DONE
            habitLogDao.insert(
                HabitLogEntity(
                    habitId = habitId,
                    date = date,
                    occurIndex = occurIndex,
                    status = LogStatus.DONE,
                    completedAt = Instant.now()
                )
            )
        } else {
            if (existing.status == LogStatus.DONE) {
                // balik ke PLANNED
                habitLogDao.updateStatus(existing.id, LogStatus.PLANNED, null)
            } else {
                // set DONE
                habitLogDao.updateStatus(existing.id, LogStatus.DONE, Instant.now())
            }
        }
    }
}
