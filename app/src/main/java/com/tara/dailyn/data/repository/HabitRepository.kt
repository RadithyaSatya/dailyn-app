package com.tara.dailyn.data.repository

import android.content.Context
import com.tara.dailyn.data.local.dao.CategoryDao
import com.tara.dailyn.data.local.dao.HabitDao
import com.tara.dailyn.data.local.dao.HabitLogDao
import com.tara.dailyn.data.local.entity.CategoryEntity
import com.tara.dailyn.data.local.entity.HabitCategoryCrossRef
import com.tara.dailyn.data.local.entity.HabitEntity
import com.tara.dailyn.data.local.entity.HabitLogEntity
import com.tara.dailyn.data.local.model.LogStatus
import com.tara.dailyn.data.local.model.OverflowPolicy
import com.tara.dailyn.data.local.model.PeriodType
import com.tara.dailyn.data.local.relation.HabitWithRules
import com.tara.dailyn.data.repository.model.HabitDetailObs
import com.tara.dailyn.data.repository.model.HabitCatalogItem
import com.tara.dailyn.data.repository.model.DailyProgress
import com.tara.dailyn.data.repository.model.HabitLogForDetail
import com.tara.dailyn.ui.features.addhabit.model.HabitCategoryOption
import com.tara.dailyn.ui.features.addhabit.model.HabitForEdit
import com.tara.dailyn.ui.features.home.model.HabitUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.*
import java.time.temporal.TemporalAdjusters
import java.util.UUID
import kotlin.collections.map
import com.tara.dailyn.data.local.model.FrequencyType as DbFreq
import com.tara.dailyn.reminder.HabitReminderScheduler
import com.tara.dailyn.ui.features.addhabit.model.FrequencyType as UiFreq
import com.tara.dailyn.ui.features.addhabit.model.PeriodType as UiPeriod

class HabitRepository(
    private val categoryDao: CategoryDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val appContext: Context? = null
) {
    companion object {
        private const val noneCategoryName = "None"
        private const val noneCategoryIcon = "•"

        private val defaultCategories = listOf(
            CategoryEntity(name = "Health", icon = "💧"),
            CategoryEntity(name = "Fitness", icon = "🏃"),
            CategoryEntity(name = "Learning", icon = "📚"),
            CategoryEntity(name = "Work", icon = "💼"),
            CategoryEntity(name = "Mindfulness", icon = "🧘"),
            CategoryEntity(name = "Finance", icon = "💰")
        )
    }

    suspend fun ensureDefaultCategories() = withContext(Dispatchers.IO) {
        ensureNoneCategory()
        defaultCategories.forEach { categoryDao.insertCategory(it) }
    }

    fun observeCategoryOptions(): Flow<List<HabitCategoryOption>> {
        return categoryDao.observeCategories().map { list ->
            list
                .filterNot(::isNoneCategory)
                .map { HabitCategoryOption(it.id, it.name, it.icon) }
        }
    }

    suspend fun createCustomCategory(name: String, icon: String): Long = withContext(Dispatchers.IO) {
        ensureDefaultCategories()
        val trimmedName = name.trim()
        val normalizedIcon = icon.trim().ifBlank { "✨" }.take(2)
        require(trimmedName.isNotBlank()) { "Category name is required." }
        require(!trimmedName.equals(noneCategoryName, ignoreCase = true)) {
            "Category name is reserved."
        }

        val insertedId = categoryDao.insertCategory(
            CategoryEntity(
                name = trimmedName,
                icon = normalizedIcon
            )
        )

        if (insertedId > 0) insertedId
        else categoryDao.getCategories().first { it.name == trimmedName }.id
    }

    fun observeManageableCategories(): Flow<List<CategoryEntity>> {
        return categoryDao.observeCategories().map { list ->
            list.filterNot(::isNoneCategory)
        }
    }

    suspend fun updateCategory(
        categoryId: Long,
        name: String,
        icon: String
    ) = withContext(Dispatchers.IO) {
        ensureDefaultCategories()
        val existing = categoryDao.getCategoryById(categoryId)
            ?: throw IllegalStateException("Category not found.")
        require(!isNoneCategory(existing)) { "Default category cannot be edited." }

        val trimmedName = name.trim()
        val normalizedIcon = icon.trim().ifBlank { existing.icon.ifBlank { "✨" } }.take(2)
        require(trimmedName.isNotBlank()) { "Category name is required." }
        require(!trimmedName.equals(noneCategoryName, ignoreCase = true)) {
            "Category name is reserved."
        }

        val conflict = categoryDao.getCategoryByName(trimmedName)
        require(conflict == null || conflict.id == existing.id) { "Category name already exists." }

        categoryDao.updateCategory(
            existing.copy(
                name = trimmedName,
                icon = normalizedIcon
            )
        )
    }

    suspend fun deleteCategory(categoryId: Long) = withContext(Dispatchers.IO) {
        ensureDefaultCategories()
        val category = categoryDao.getCategoryById(categoryId)
            ?: return@withContext
        require(!isNoneCategory(category)) { "Default category cannot be deleted." }

        val noneCategoryId = ensureNoneCategory()
        val affectedHabitIds = habitDao.getHabitIdsByCategory(categoryId).distinct()
        habitDao.deleteHabitCategoryRefsByCategory(categoryId)

        val fallbackRefs = affectedHabitIds
            .filter { habitDao.countHabitCategories(it) == 0 }
            .map { habitId ->
                HabitCategoryCrossRef(
                    habitId = habitId,
                    categoryId = noneCategoryId
                )
            }
        if (fallbackRefs.isNotEmpty()) {
            habitDao.insertHabitCategoryRefs(fallbackRefs)
        }

        categoryDao.deleteCategory(category)
    }

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
        categoryId: Long,
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
            validateSomeDaysCount(someDaysCount, dbPeriod)
        }


        val finalSomeDaysCount: Int? =
            if (dbFrequency == DbFreq.SOME_DAYS_PER_PERIOD) someDaysCount else null


        val habit = HabitEntity(
            id = id,
            habitGroupId = id,
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
            categoryIds = listOf(categoryId)
        )
        appContext?.let { HabitReminderScheduler.syncHabit(it, id) }
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
            rows
                .map { r ->
                    val isNoneCategory = r.categoryName == noneCategoryName
                    HabitUi(
                        id = r.id,
                        title = r.title,
                        description = r.description.orEmpty(),
                        categoryName = if (isNoneCategory) "" else r.categoryName.orEmpty(),
                        categoryIcon = if (isNoneCategory) "" else r.categoryIcon.orEmpty(),
                        isCompletedToday = r.isCompleted
                    )
                }
                .sortedBy { it.isCompletedToday }
        }
    }

    fun observeWeeklyProgress(weekStart: LocalDate): Flow<List<DailyProgress>> {
        val dayFlows = (0L..6L).map { offset ->
            val date = weekStart.plusDays(offset)
            observeHomeRows(date = date, occurIndex = 0).map { rows ->
                DailyProgress(
                    date = date,
                    completedCount = rows.count { it.isCompletedToday },
                    totalCount = rows.size
                )
            }
        }

        return combine(dayFlows) { it.toList() }
    }

    fun observeHabitCatalog(): Flow<List<HabitCatalogItem>> {
        return habitDao.observeAllHabitsWithRules().map { versions ->
            versions
                .groupBy { it.habit.habitGroupId ?: it.habit.id }
                .values
                .map { group -> latestVersionForDisplay(group) }
                .sortedBy { it.habit.title.lowercase() }
                .map { rel ->
                    val category = rel.categories.firstOrNull { !isNoneCategory(it) }
                    HabitCatalogItem(
                        id = rel.habit.habitGroupId ?: rel.habit.id,
                        title = rel.habit.title,
                        description = rel.habit.description.orEmpty(),
                        categoryId = category?.id,
                        categoryName = category?.name.orEmpty(),
                        categoryIcon = category?.icon.orEmpty(),
                        frequencyType = rel.habit.frequencyType,
                        scheduleText = buildScheduleText(rel)
                    )
                }
        }
    }

    fun observeAllHabitVersions(): Flow<List<HabitWithRules>> {
        return habitDao.observeAllHabitsWithRules()
    }

    fun observeAllLogsUpToToday(): Flow<List<HabitLogEntity>> {
        return habitLogDao.observeAllLogsUpTo(LocalDate.now())
    }

    suspend fun toggle(
        habitId: String,
        date: LocalDate,
        occurIndex: Int = 0,
        allowPreviousDayEdits: Boolean = false
    ) {
        val today = LocalDate.now()
        if (date.isAfter(today) || (date.isBefore(today) && !allowPreviousDayEdits)) return

        val version = habitDao.getHabitVersionForDate(habitId, date) ?: return

        val existing = habitLogDao.findOne(version.id, date, occurIndex)
        if (existing == null) {
            // belum ada, create DONE
            habitLogDao.insert(
                HabitLogEntity(
                    habitId = version.id,
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

    suspend fun getHabitForEdit(id: String): HabitForEdit = withContext(Dispatchers.IO) {
        val latestVersion = habitDao.getLatestHabitVersion(id)
            ?: throw IllegalStateException("Habit not found: $id")
        val rel = habitDao.getHabitWithRules(latestVersion.id)
            ?: throw IllegalStateException("Habit version not found: ${latestVersion.id}")

        // Map DB -> UI
        val uiFreq = when (rel.habit.frequencyType) {
            DbFreq.EVERY_DAY -> UiFreq.EVERY_DAY
            DbFreq.CUSTOM_WEEKLY -> UiFreq.SPECIFIC_DAYS_OF_WEEK
            DbFreq.SPECIFIC_DATES_OF_MONTH -> UiFreq.SPECIFIC_DAY_OF_MONTH
            DbFreq.SOME_DAYS_PER_PERIOD -> UiFreq.SOME_DAYS_PER_PERIOD
        }

        val uiPeriod = when (rel.habit.periodType) {
            com.tara.dailyn.data.local.model.PeriodType.WEEK -> UiPeriod.WEEK
            com.tara.dailyn.data.local.model.PeriodType.MONTH -> UiPeriod.MONTH
            null -> UiPeriod.WEEK // default aman; tidak dipakai kecuali SOME_DAYS_PER_PERIOD
        }

        HabitForEdit(
            id = rel.habit.id,
            title = rel.habit.title,
            description = rel.habit.description,
            selectedCategoryId = rel.categories.firstOrNull { !isNoneCategory(it) }?.id,
            uiFrequencyType = uiFreq,
            uiPeriodType = uiPeriod,
            selectedDaysOfWeek = rel.weeklyDays.map { DayOfWeek.of(it.dayOfWeek) },
            specificDaysOfMonth = rel.monthlyDays.map { it.dayOfMonth },
            someDaysCount = rel.habit.someDaysCount,
            reminderEnabled = rel.reminders.isNotEmpty(),
            reminderTime = rel.reminders.firstOrNull()?.timeOfDay
        )
    }

    suspend fun updateHabit(
        habitId: String,
        title: String,
        description: String?,
        uiFrequency: UiFreq,
        selectedDaysOfWeek: Set<DayOfWeek>,
        specificDaysOfMonth: Set<Int>,
        someDaysCount: Int?,
        uiPeriodType: UiPeriod,
        categoryId: Long,
        reminderEnabled: Boolean,
        reminderTime: LocalTime?
    ) = withContext(Dispatchers.IO) {
        val now = Instant.now()
        val effectiveDate = LocalDate.now()

        val dbFrequency: DbFreq = when (uiFrequency) {
            UiFreq.EVERY_DAY -> DbFreq.EVERY_DAY
            UiFreq.SPECIFIC_DAYS_OF_WEEK -> DbFreq.CUSTOM_WEEKLY
            UiFreq.SPECIFIC_DAY_OF_MONTH -> DbFreq.SPECIFIC_DATES_OF_MONTH
            UiFreq.SOME_DAYS_PER_PERIOD -> DbFreq.SOME_DAYS_PER_PERIOD
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
            validateSomeDaysCount(someDaysCount, dbPeriod)
        }

        val existing = habitDao.getLatestHabitVersion(habitId)
            ?: throw IllegalStateException("Habit not found")
        val groupId = existing.habitGroupId ?: existing.id
        val shouldVersion = existing.startDate.isBefore(effectiveDate)

        val weeklyDaysInts =
            if (dbFrequency == DbFreq.CUSTOM_WEEKLY) selectedDaysOfWeek.map { it.value } else emptyList()
        val monthlyDaysInts =
            if (dbFrequency == DbFreq.SPECIFIC_DATES_OF_MONTH) specificDaysOfMonth.sorted() else emptyList()
        val reminders = if (reminderEnabled && reminderTime != null) listOf(reminderTime) else emptyList()

        if (shouldVersion) {
            val closedVersion = existing.copy(
                endDate = effectiveDate.minusDays(1),
                updatedAt = now
            )
            habitDao.updateHabit(closedVersion)

            val newVersionId = UUID.randomUUID().toString()
            val newVersion = HabitEntity(
                id = newVersionId,
                habitGroupId = groupId,
                title = title.trim(),
                description = description?.trim().orEmpty(),
                frequencyType = dbFrequency,
                periodType = dbPeriod,
                someDaysCount = if (dbFrequency == DbFreq.SOME_DAYS_PER_PERIOD) someDaysCount else null,
            overflowPolicy = existing.overflowPolicy,
            startDate = effectiveDate,
            endDate = null,
            defaultTimeOfDay = existing.defaultTimeOfDay,
            color = existing.color,
            icon = existing.icon,
            homeSortOrder = existing.homeSortOrder,
            isArchived = false,
            createdAt = now,
            updatedAt = now
        )
            habitDao.upsertHabitWithRules(
                habit = newVersion,
                weeklyDays = weeklyDaysInts,
                monthlyDays = monthlyDaysInts,
                reminders = reminders,
                categoryIds = listOf(categoryId)
            )
            appContext?.let {
                HabitReminderScheduler.cancelHabit(it, existing.id)
                HabitReminderScheduler.syncHabit(it, newVersionId)
            }
        } else {
            val updated = existing.copy(
                title = title.trim(),
                description = description?.trim().orEmpty(),
                frequencyType = dbFrequency,
                periodType = dbPeriod,
                someDaysCount = if (dbFrequency == DbFreq.SOME_DAYS_PER_PERIOD) someDaysCount else null,
                updatedAt = now
            )
            habitDao.updateHabitWithRules(
                habit = updated,
                weeklyDays = weeklyDaysInts,
                monthlyDays = monthlyDaysInts,
                reminders = reminders,
                categoryIds = listOf(categoryId)
            )
            appContext?.let { HabitReminderScheduler.syncHabit(it, existing.id) }
        }
    }

    suspend fun deleteHabit(habitId: String) = withContext(Dispatchers.IO) {
        habitDao.getHabitVersions(habitId).forEach { version ->
            habitDao.deleteHabitCascade(version.id)
            appContext?.let { HabitReminderScheduler.cancelHabit(it, version.id) }
        }
    }

    suspend fun reorderHomeHabits(groupIdsInOrder: List<String>) = withContext(Dispatchers.IO) {
        val now = Instant.now()
        groupIdsInOrder.forEachIndexed { index, groupId ->
            habitDao.updateHomeSortOrderForGroup(
                groupId = groupId,
                homeSortOrder = index.toLong(),
                updatedAt = now
            )
        }
    }

    fun observeHabitDetail(habitId: String): Flow<HabitDetailObs> {
        return habitDao.observeHabitVersionsWithRules(habitId)
            .map { versions ->
                val rel = latestVersionForDisplay(versions)
                HabitDetailObs(
                    id = rel.habit.id,
                    title = rel.habit.title,
                    description = rel.habit.description ?: "",
                    scheduleText = buildScheduleText(rel)
                )
            }
    }

    fun observeHabitWithRules(habitId: String): Flow<List<HabitWithRules>> {
        return habitDao.observeHabitVersionsWithRules(habitId)
    }

    private fun validateSomeDaysCount(count: Int, period: PeriodType) {
        when (period) {
            PeriodType.WEEK -> require(count <= 7) {
                "Target per minggu tidak boleh lebih dari 7 hari."
            }
            PeriodType.MONTH -> require(count <= 31) {
                "Target per bulan tidak boleh lebih dari 31 hari."
            }
        }
    }

    private suspend fun ensureNoneCategory(): Long {
        val existing = categoryDao.getCategoryByName(noneCategoryName)
        if (existing != null) return existing.id
        return categoryDao.insertCategory(
            CategoryEntity(
                name = noneCategoryName,
                icon = noneCategoryIcon
            )
        )
    }

    private fun isNoneCategory(category: CategoryEntity): Boolean {
        return category.name == noneCategoryName
    }

    /**
     * Ambil log X hari ke belakang, urut ASC (biar .takeLast(...) di ViewModel kamu masuk akal)
     */
    fun observeLogs(habitId: String, daysBack: Int): Flow<List<HabitLogForDetail>> {
        val fromDate = LocalDate.now().minusDays(daysBack.toLong())
        val toDate = LocalDate.now()

        return habitLogDao.observeLogs(
            habitId = habitId,
            from = fromDate,
            to = toDate
        ).map { list ->
            list
                .sortedWith(compareBy<HabitLogEntity> { it.date }.thenBy { it.occurIndex })
                .map {
                    HabitLogForDetail(
                        date = it.date,
                        statusDone = it.status == LogStatus.DONE
                    )
                }
        }
    }

    fun observeLogsUpToToday(habitId: String): Flow<List<HabitLogForDetail>> {
        val toDate = LocalDate.now()

        return habitLogDao.observeLogsUpToForGroup(
            groupId = habitId,
            to = toDate
        ).map { list ->
            list
                .sortedWith(compareBy<HabitLogEntity> { it.date }.thenBy { it.occurIndex })
                .map {
                    HabitLogForDetail(
                        date = it.date,
                        statusDone = it.status == LogStatus.DONE
                    )
                }
        }
    }

    private fun latestVersionForDisplay(versions: List<HabitWithRules>): HabitWithRules {
        val today = LocalDate.now()
        return versions
            .lastOrNull {
                !it.habit.startDate.isAfter(today) &&
                    (it.habit.endDate == null || !it.habit.endDate!!.isBefore(today))
            }
            ?: versions.maxBy { it.habit.startDate }
    }

    fun buildScheduleText(rel: HabitWithRules): String {
        val habit = rel.habit
        return when (habit.frequencyType) {
            DbFreq.EVERY_DAY -> "Every day"

            DbFreq.CUSTOM_WEEKLY -> {
                val days = rel.weeklyDays
                    .map { it.dayOfWeek }
                    .sorted()
                    .map { DayOfWeek.of(it).name.lowercase().replaceFirstChar { c -> c.uppercase() } }
                if (days.isEmpty()) "Specific days of week" else "Every ${days.joinToString(", ")}"
            }

            DbFreq.SPECIFIC_DATES_OF_MONTH -> {
                val dates = rel.monthlyDays.map { it.dayOfMonth }.sorted()
                if (dates.isEmpty()) "Specific dates of month"
                else "On ${dates.joinToString(", ")} each month"
            }

            DbFreq.SOME_DAYS_PER_PERIOD -> {
                val count = habit.someDaysCount ?: 0
                when (habit.periodType) {
                    PeriodType.WEEK -> "$count day(s) per week"
                    PeriodType.MONTH -> "$count day(s) per month"
                    else -> "$count day(s)"
                }
            }
        }
    }
}
