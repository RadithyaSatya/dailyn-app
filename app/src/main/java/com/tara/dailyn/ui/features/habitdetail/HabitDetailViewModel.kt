// ui/features/habitdetail/HabitDetailViewModel.kt
package com.tara.dailyn.ui.features.habitdetail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tara.dailyn.data.local.model.FrequencyType
import com.tara.dailyn.data.local.model.OverflowPolicy
import com.tara.dailyn.data.local.model.PeriodType
import com.tara.dailyn.data.local.relation.HabitWithRules
import com.tara.dailyn.data.repository.HabitRepository
import com.tara.dailyn.data.repository.model.HabitLogForDetail
import com.tara.dailyn.ui.features.habitdetail.model.HabitCalendarDayState
import com.tara.dailyn.ui.features.habitdetail.model.HabitCalendarDayUi
import com.tara.dailyn.ui.features.habitdetail.model.HabitDetailUi
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

class HabitDetailViewModel(
    private val repo: HabitRepository,
    private val habitId: String
) : ViewModel() {

    private val _ui = MutableStateFlow(HabitDetailUi(isLoading = true))
    val ui: StateFlow<HabitDetailUi> = _ui.asStateFlow()
    private val visibleMonth = MutableStateFlow(YearMonth.now())

    init {
        viewModelScope.launch {
            combine(
                repo.observeHabitWithRules(habitId),
                repo.observeLogsUpToToday(habitId),
                visibleMonth
            ) { h, logs, month ->
                    val current = latestVersionForDisplay(h)
                    val streak = computeStreak(h, logs)
                    val firstHabitMonth = h.minOfOrNull { YearMonth.from(it.habit.startDate) } ?: month
                    val lastVisibleDate = minOf(LocalDate.now(), current.habit.endDate ?: LocalDate.now())
                    val lastHabitMonth = YearMonth.from(lastVisibleDate)
                    HabitDetailUi(
                        id = habitId,
                        title = current.habit.title,
                        description = current.habit.description.orEmpty(),
                        scheduleText = repo.buildScheduleText(current),
                        streak = streak,
                        visibleMonth = month,
                        canGoToPreviousMonth = month.isAfter(firstHabitMonth),
                        canGoToNextMonth = month.isBefore(lastHabitMonth),
                        calendarDays = buildCalendarDays(h, logs, month),
                        isLoading = false
                    )
                }
                .catch { e -> _ui.update { it.copy(isLoading = false, error = e.message) } }
                .collect { _ui.value = it }
        }
    }

    fun showPreviousMonth() {
        visibleMonth.update { it.minusMonths(1) }
    }

    fun showNextMonth() {
        visibleMonth.update { it.plusMonths(1) }
    }

    private fun computeStreak(
        versions: List<HabitWithRules>,
        logs: List<HabitLogForDetail>
    ): Int {
        if (versions.isEmpty()) return 0
        val doneDates = logs
            .filter { it.statusDone }
            .map { it.date }
            .toSet()

        val orderedVersions = versions.sortedBy { it.habit.startDate }
        val latest = latestVersionForDisplay(orderedVersions)
        val anchorDate = minOf(LocalDate.now(), latest.habit.endDate ?: LocalDate.now())
        return computeStreakEndingAt(
            date = anchorDate,
            versions = orderedVersions,
            doneDates = doneDates
        )
    }

    private fun computeStreakEndingAt(
        date: LocalDate,
        versions: List<HabitWithRules>,
        doneDates: Set<LocalDate>
    ): Int {
        val earliestStart = versions.minOf { it.habit.startDate }
        if (date.isBefore(earliestStart)) return 0

        val rule = ruleForDate(versions, date) ?: return computeStreakEndingAt(
            date = date.minusDays(1),
            versions = versions,
            doneDates = doneDates
        )

        return when (rule.habit.frequencyType) {
            FrequencyType.SOME_DAYS_PER_PERIOD -> evaluateQuotaPeriod(
                date = date,
                rule = rule,
                versions = versions,
                doneDates = doneDates
            )
            else -> evaluateScheduledOccurrence(
                date = date,
                rule = rule,
                versions = versions,
                doneDates = doneDates
            )
        }
    }

    private fun evaluateScheduledOccurrence(
        date: LocalDate,
        rule: HabitWithRules,
        versions: List<HabitWithRules>,
        doneDates: Set<LocalDate>
    ): Int {
        val occurrenceDate = latestScheduledOccurrenceOnOrBefore(rule, date)
            ?: return computeStreakEndingAt(
                date = rule.habit.startDate.minusDays(1),
                versions = versions,
                doneDates = doneDates
            )

        if (occurrenceDate !in doneDates) return 0

        return 1 + computeStreakEndingAt(
            date = occurrenceDate.minusDays(1),
            versions = versions,
            doneDates = doneDates
        )
    }

    private fun latestScheduledOccurrenceOnOrBefore(
        rule: HabitWithRules,
        date: LocalDate
    ): LocalDate? {
        var cursor = minOf(date, rule.habit.endDate ?: date)
        while (!cursor.isBefore(rule.habit.startDate)) {
            if (isScheduledOccurrence(rule, cursor)) return cursor
            cursor = cursor.minusDays(1)
        }
        return null
    }

    private fun evaluateQuotaPeriod(
        date: LocalDate,
        rule: HabitWithRules,
        versions: List<HabitWithRules>,
        doneDates: Set<LocalDate>
    ): Int {
        val target = rule.habit.someDaysCount ?: return 0
        val (periodStartRaw, periodEndRaw) = periodBounds(rule, date)
        val periodStart = maxOf(periodStartRaw, rule.habit.startDate)
        val periodEnd = minOf(periodEndRaw, rule.habit.endDate ?: periodEndRaw)
        val completedCount = countDoneInRange(doneDates, periodStart, periodEnd)

        val today = LocalDate.now()
        val periodIsOpen =
            periodEndRaw >= today &&
                (rule.habit.endDate == null || rule.habit.endDate!! >= today)

        if (completedCount >= target) {
            return 1 + computeStreakEndingAt(
                date = periodStart.minusDays(1),
                versions = versions,
                doneDates = doneDates
            )
        }

        if (periodIsOpen) {
            return computeStreakEndingAt(
                date = periodStart.minusDays(1),
                versions = versions,
                doneDates = doneDates
            )
        }

        return 0
    }

    private fun periodBounds(
        rule: HabitWithRules,
        date: LocalDate
    ): Pair<LocalDate, LocalDate> {
        return when (rule.habit.periodType) {
            PeriodType.WEEK -> {
                val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                start to start.plusDays(6)
            }
            PeriodType.MONTH -> {
                val month = YearMonth.from(date)
                month.atDay(1) to month.atEndOfMonth()
            }
            null -> date to date
        }
    }

    private fun countDoneInRange(
        doneDates: Set<LocalDate>,
        start: LocalDate,
        end: LocalDate
    ): Int {
        return doneDates.count { !it.isBefore(start) && !it.isAfter(end) }
    }

    private fun isScheduledOccurrence(
        relation: HabitWithRules,
        date: LocalDate
    ): Boolean {
        return when (relation.habit.frequencyType) {
            FrequencyType.EVERY_DAY -> true
            FrequencyType.CUSTOM_WEEKLY -> {
                relation.weeklyDays.any { it.dayOfWeek == date.dayOfWeek.value }
            }
            FrequencyType.SPECIFIC_DATES_OF_MONTH -> {
                relation.monthlyDays.any {
                    matchesDayOfMonth(relation.habit.overflowPolicy, it.dayOfMonth, date)
                }
            }
            FrequencyType.SOME_DAYS_PER_PERIOD -> false
        }
    }

    private fun ruleForDate(
        versions: List<HabitWithRules>,
        date: LocalDate
    ): HabitWithRules? {
        return versions.lastOrNull {
            !it.habit.startDate.isAfter(date) &&
                (it.habit.endDate == null || !it.habit.endDate!!.isBefore(date))
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

    private fun matchesDayOfMonth(
        overflowPolicy: OverflowPolicy?,
        requestedDay: Int,
        date: LocalDate
    ): Boolean {
        val lastDay = YearMonth.from(date).lengthOfMonth()
        return when {
            requestedDay <= lastDay -> date.dayOfMonth == requestedDay
            overflowPolicy == OverflowPolicy.SHIFT_TO_LAST_DAY -> date.dayOfMonth == lastDay
            overflowPolicy == OverflowPolicy.ROLL_TO_NEXT_MONTH -> false
            else -> false
        }
    }

    private fun buildCalendarDays(
        versions: List<HabitWithRules>,
        logs: List<HabitLogForDetail>,
        month: YearMonth
    ): List<HabitCalendarDayUi> {
        if (versions.isEmpty()) return emptyList()

        val logsByDate = logs.associateBy { it.date }
        val startPadding = month.atDay(1).dayOfWeek.value % 7
        val totalDays = month.lengthOfMonth()
        val days = mutableListOf<HabitCalendarDayUi>()

        repeat(startPadding) {
            days += HabitCalendarDayUi(
                dayOfMonth = 0,
                state = HabitCalendarDayState.OUTSIDE_MONTH
            )
        }

        for (day in 1..totalDays) {
            val date = month.atDay(day)
            val rule = ruleForDate(versions, date)
            val log = logsByDate[date]
            val state = when {
                rule == null -> HabitCalendarDayState.IDLE
                log?.statusDone == true -> HabitCalendarDayState.DONE
                log?.statusDone == false -> HabitCalendarDayState.MISSED
                date.isAfter(LocalDate.now()) -> HabitCalendarDayState.FUTURE
                rule.habit.frequencyType == FrequencyType.SOME_DAYS_PER_PERIOD -> HabitCalendarDayState.IDLE
                isScheduledOccurrence(rule, date) -> HabitCalendarDayState.SCHEDULED
                else -> HabitCalendarDayState.IDLE
            }
            days += HabitCalendarDayUi(dayOfMonth = day, state = state)
        }

        while (days.size % 7 != 0) {
            days += HabitCalendarDayUi(
                dayOfMonth = 0,
                state = HabitCalendarDayState.OUTSIDE_MONTH
            )
        }

        return days
    }

    suspend fun deleteHabit(): Boolean {
        repo.deleteHabit(habitId)
        return true
    }
}
