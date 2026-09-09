package com.tara.dailyn.ui.features.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tara.dailyn.data.local.entity.HabitLogEntity
import com.tara.dailyn.data.local.model.FrequencyType
import com.tara.dailyn.data.local.model.LogStatus
import com.tara.dailyn.data.local.model.OverflowPolicy
import com.tara.dailyn.data.local.model.PeriodType
import com.tara.dailyn.data.local.relation.HabitWithRules
import com.tara.dailyn.data.repository.HabitRepository
import com.tara.dailyn.ui.features.journey.model.JourneyDateSectionUi
import com.tara.dailyn.ui.features.journey.model.JourneyEventKind
import com.tara.dailyn.ui.features.journey.model.JourneyFilterUi
import com.tara.dailyn.ui.features.journey.model.JourneyTimelineItemUi
import com.tara.dailyn.ui.features.journey.model.JourneyUiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

class JourneyViewModel(
    private val repo: HabitRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(JourneyFilterUi.ALL)

    private val _state = MutableStateFlow(JourneyUiState())
    val state: StateFlow<JourneyUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repo.observeAllHabitVersions(),
                repo.observeAllLogsUpToToday(),
                selectedFilter
            ) { versions, logs, filter ->
                JourneyUiState(
                    selectedFilter = filter,
                    sections = buildSections(versions, logs, filter),
                    isLoading = false
                )
            }.collect { _state.value = it }
        }
    }

    fun selectFilter(filter: JourneyFilterUi) {
        selectedFilter.value = filter
    }

    private fun buildSections(
        versions: List<HabitWithRules>,
        logs: List<HabitLogEntity>,
        filter: JourneyFilterUi
    ): List<JourneyDateSectionUi> {
        val grouped = buildTimelineEvents(versions, logs)
            .filter { matchesFilter(it.kind, filter) }
            .groupBy { it.date }
            .toSortedMap(compareByDescending { it })

        return grouped.map { (date, items) ->
            val completed = items.count { it.kind == JourneyEventKind.COMPLETED }
            val highlights = items.count {
                it.kind == JourneyEventKind.STREAK || it.kind == JourneyEventKind.ACHIEVEMENT
            }

            JourneyDateSectionUi(
                date = date,
                summaryTitle = summaryTitle(completed, highlights),
                summarySubtitle = summarySubtitle(completed, highlights),
                items = items
                    .sortedWith(compareBy<TimelineEvent> { it.occurredAt }.thenBy { it.priority })
                    .map { it.toUi() }
            )
        }
    }

    private fun buildTimelineEvents(
        versions: List<HabitWithRules>,
        logs: List<HabitLogEntity>
    ): List<TimelineEvent> {
        if (versions.isEmpty()) return emptyList()

        val logsByVersion = logs.groupBy { it.habitId }

        return versions
            .groupBy { it.habit.habitGroupId ?: it.habit.id }
            .values
            .flatMap { group ->
                buildHabitTimeline(
                    versions = group.sortedBy { it.habit.startDate },
                    logsByVersion = logsByVersion
                )
            }
            .sortedByDescending { it.occurredAt }
    }

    private fun buildHabitTimeline(
        versions: List<HabitWithRules>,
        logsByVersion: Map<String, List<HabitLogEntity>>
    ): List<TimelineEvent> {
        val title = versions.last().habit.title
        val formatter = DateTimeFormatter.ofPattern("hh:mm a", Locale.getDefault())
        val unitResults = mutableListOf<UnitResult>()
        val events = mutableListOf<TimelineEvent>()

        versions.forEachIndexed { index, relation ->
            val habit = relation.habit
            val doneLogs = logsByVersion[habit.id].orEmpty()
                .filter { it.status == LogStatus.DONE }
                .sortedBy { it.date }
            val missedLogs = logsByVersion[habit.id].orEmpty()
                .filter { it.status == LogStatus.SKIPPED }
                .sortedBy { it.date }
            val doneDates = doneLogs.map { it.date }.toSet()

            events += TimelineEvent(
                id = "change-${habit.id}",
                date = habit.startDate,
                occurredAt = if (index == 0) habit.createdAt else habit.updatedAt,
                kind = JourneyEventKind.CHANGE,
                title = if (index == 0) "$title was created" else "$title was updated",
                subtitle = repo.buildScheduleText(relation),
                accentLabel = if (index == 0) "New habit" else "Rule changed",
                priority = if (index == 0) 2 else 3,
                isHighlighted = false
            )

            doneLogs.forEach { log ->
                events += TimelineEvent(
                    id = "completed-${log.id}",
                    date = log.date,
                    occurredAt = log.completedAt ?: atStartOfDay(log.date),
                    kind = JourneyEventKind.COMPLETED,
                    title = "$title completed",
                    subtitle = log.completedAt
                        ?.atZone(ZoneId.systemDefault())
                        ?.toLocalTime()
                        ?.format(formatter)
                        ?.let { "Completed at $it" }
                        ?: "Completed",
                    accentLabel = "",
                    priority = 1,
                    isHighlighted = false
                )
            }

            missedLogs.forEach { log ->
                events += TimelineEvent(
                    id = "missed-${log.id}",
                    date = log.date,
                    occurredAt = atStartOfDay(log.date),
                    kind = JourneyEventKind.MISSED,
                    title = "$title missed",
                    subtitle = when (habit.frequencyType) {
                        FrequencyType.SOME_DAYS_PER_PERIOD -> "Target period was not completed"
                        else -> "Not completed on schedule"
                    },
                    accentLabel = "Missed",
                    priority = 4,
                    isHighlighted = false
                )
            }

            when (habit.frequencyType) {
                FrequencyType.SOME_DAYS_PER_PERIOD -> unitResults += buildPeriodUnits(relation, doneLogs, title)
                else -> unitResults += buildScheduledUnits(relation, doneLogs, title)
            }
        }

        var streak = 0
        var bestStreak = 0
        unitResults.sortedBy { it.date }.forEach { result ->
            if (result.success) {
                streak += 1
                if (streak > bestStreak) {
                    bestStreak = streak
                    if (bestStreak > 1) {
                        events += TimelineEvent(
                            id = "record-${result.habitId}-${result.date}",
                            date = result.date,
                            occurredAt = result.occurredAt,
                            kind = JourneyEventKind.ACHIEVEMENT,
                            title = "New personal record for ${result.title}",
                            subtitle = "$bestStreak successful periods in a row",
                            accentLabel = "Personal best",
                            priority = 5,
                            isHighlighted = true
                        )
                    }
                }

                if (streak in streakMilestones) {
                    events += TimelineEvent(
                        id = "streak-${result.habitId}-${result.date}",
                        date = result.date,
                        occurredAt = result.occurredAt,
                        kind = JourneyEventKind.STREAK,
                        title = "${result.title} reached $streak streak",
                        subtitle = result.context,
                        accentLabel = "Streak milestone",
                        priority = 6,
                        isHighlighted = true
                    )
                    events += TimelineEvent(
                        id = "achievement-${result.habitId}-${result.date}",
                        date = result.date,
                        occurredAt = result.occurredAt,
                        kind = JourneyEventKind.ACHIEVEMENT,
                        title = "Achievement unlocked for ${result.title}",
                        subtitle = "$streak-streak milestone reached",
                        accentLabel = "Unlocked",
                        priority = 7,
                        isHighlighted = true
                    )
                }
            } else {
                streak = 0
            }
        }

        return events
    }

    private fun buildScheduledUnits(
        relation: HabitWithRules,
        doneLogs: List<HabitLogEntity>,
        title: String
    ): List<UnitResult> {
        val endDate = minOf(relation.habit.endDate ?: LocalDate.now(), LocalDate.now())
        val completedAtByDate = doneLogs.associate { log -> log.date to (log.completedAt ?: atStartOfDay(log.date)) }
        val results = mutableListOf<UnitResult>()
        var cursor = relation.habit.startDate

        while (!cursor.isAfter(endDate)) {
            if (isScheduledOccurrence(relation, cursor)) {
                val occurredAt = completedAtByDate[cursor]
                results += UnitResult(
                    habitId = relation.habit.id,
                    title = title,
                    date = cursor,
                    success = occurredAt != null,
                    context = unitContext(relation, cursor),
                    occurredAt = occurredAt ?: atStartOfDay(cursor)
                )
            }
            cursor = cursor.plusDays(1)
        }

        return results
    }

    private fun buildPeriodUnits(
        relation: HabitWithRules,
        doneLogs: List<HabitLogEntity>,
        title: String
    ): List<UnitResult> {
        val target = relation.habit.someDaysCount ?: return emptyList()
        val endDate = minOf(relation.habit.endDate ?: LocalDate.now(), LocalDate.now())
        val completedLogsByDate = doneLogs.sortedBy { it.date }
        val results = mutableListOf<UnitResult>()
        var cursor = relation.habit.startDate

        while (!cursor.isAfter(endDate)) {
            val (periodStartRaw, periodEndRaw) = periodBounds(relation.habit.periodType, cursor)
            val periodStart = maxOf(periodStartRaw, relation.habit.startDate)
            val periodEnd = minOf(periodEndRaw, endDate)
            val doneInPeriod = completedLogsByDate.filter { !it.date.isBefore(periodStart) && !it.date.isAfter(periodEnd) }
            val successLog = doneInPeriod.getOrNull(target - 1)

            if (successLog != null) {
                results += UnitResult(
                    habitId = relation.habit.id,
                    title = title,
                    date = successLog.date,
                    success = true,
                    context = "${target}x target completed for ${periodLabel(relation.habit.periodType, periodStart)}",
                    occurredAt = successLog.completedAt ?: atStartOfDay(successLog.date)
                )
            }

            cursor = nextPeriodStart(relation.habit.periodType, periodStartRaw)
        }

        return results.distinctBy { "${it.habitId}-${it.date}-${it.success}" }
    }

    private fun isScheduledOccurrence(
        relation: HabitWithRules,
        date: LocalDate
    ): Boolean {
        return when (relation.habit.frequencyType) {
            FrequencyType.EVERY_DAY -> true
            FrequencyType.CUSTOM_WEEKLY -> relation.weeklyDays.any { it.dayOfWeek == date.dayOfWeek.value }
            FrequencyType.SPECIFIC_DATES_OF_MONTH -> relation.monthlyDays.any {
                matchesDayOfMonth(relation.habit.overflowPolicy, it.dayOfMonth, date)
            }
            FrequencyType.SOME_DAYS_PER_PERIOD -> false
        }
    }

    private fun unitContext(relation: HabitWithRules, date: LocalDate): String {
        return when (relation.habit.frequencyType) {
            FrequencyType.EVERY_DAY -> "Daily habit"
            FrequencyType.CUSTOM_WEEKLY -> date.dayOfWeek.name.lowercase().replaceFirstChar { it.uppercase() }
            FrequencyType.SPECIFIC_DATES_OF_MONTH -> "Scheduled on day ${date.dayOfMonth}"
            FrequencyType.SOME_DAYS_PER_PERIOD -> repo.buildScheduleText(relation)
        }
    }

    private fun periodBounds(
        periodType: PeriodType?,
        date: LocalDate
    ): Pair<LocalDate, LocalDate> {
        return when (periodType) {
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

    private fun nextPeriodStart(
        periodType: PeriodType?,
        currentStart: LocalDate
    ): LocalDate {
        return when (periodType) {
            PeriodType.WEEK -> currentStart.plusWeeks(1)
            PeriodType.MONTH -> currentStart.plusMonths(1).withDayOfMonth(1)
            null -> currentStart.plusDays(1)
        }
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

    private fun periodLabel(periodType: PeriodType?, periodStart: LocalDate): String {
        return when (periodType) {
            PeriodType.WEEK -> "week of ${periodStart.format(shortDateFormatter)}"
            PeriodType.MONTH -> periodStart.month.name.lowercase().replaceFirstChar { it.uppercase() }
            null -> periodStart.format(shortDateFormatter)
        }
    }

    private fun matchesFilter(kind: JourneyEventKind, filter: JourneyFilterUi): Boolean {
        return when (filter) {
            JourneyFilterUi.ALL -> true
            JourneyFilterUi.COMPLETED -> kind == JourneyEventKind.COMPLETED
            JourneyFilterUi.MISSED -> kind == JourneyEventKind.MISSED
            JourneyFilterUi.STREAKS -> kind == JourneyEventKind.STREAK
            JourneyFilterUi.ACHIEVEMENTS -> kind == JourneyEventKind.ACHIEVEMENT
            JourneyFilterUi.CHANGES -> kind == JourneyEventKind.CHANGE
            JourneyFilterUi.NOTES -> false
        }
    }

    private fun summaryTitle(completed: Int, highlights: Int): String {
        return when {
            completed > 0 && highlights > 0 -> "Milestone Day"
            completed > 0 -> "Perfect Day"
            highlights > 0 -> "Milestone Day"
            else -> "Quiet Day"
        }
    }

    private fun summarySubtitle(completed: Int, highlights: Int): String {
        val parts = buildList {
            if (completed > 0) add("$completed completed")
            if (highlights > 0) add("$highlights milestones")
        }
        return if (parts.isEmpty()) "No major updates recorded." else parts.joinToString(" • ")
    }

    private fun TimelineEvent.toUi(): JourneyTimelineItemUi {
        return JourneyTimelineItemUi(
            id = id,
            kind = kind,
            title = title,
            subtitle = subtitle,
            accentLabel = accentLabel,
            isHighlighted = isHighlighted
        )
    }

    private data class TimelineEvent(
        val id: String,
        val date: LocalDate,
        val occurredAt: Instant,
        val kind: JourneyEventKind,
        val title: String,
        val subtitle: String,
        val accentLabel: String,
        val priority: Int,
        val isHighlighted: Boolean
    )

    private data class UnitResult(
        val habitId: String,
        val title: String,
        val date: LocalDate,
        val success: Boolean,
        val context: String,
        val occurredAt: Instant
    )

    companion object {
        private val streakMilestones = setOf(3, 7, 14, 21, 30, 50, 100, 365)
        private val shortDateFormatter = DateTimeFormatter.ofPattern("d MMM", Locale.getDefault())
    }

    private fun atStartOfDay(date: LocalDate): Instant {
        return date.atStartOfDay(ZoneId.systemDefault()).toInstant()
    }
}
