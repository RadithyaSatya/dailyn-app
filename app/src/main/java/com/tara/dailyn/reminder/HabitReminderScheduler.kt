package com.tara.dailyn.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.tara.dailyn.R
import com.tara.dailyn.data.local.dao.HabitLogDao
import com.tara.dailyn.data.local.db.AppDatabase
import com.tara.dailyn.data.local.entity.HabitReminderEntity
import com.tara.dailyn.data.local.model.FrequencyType
import com.tara.dailyn.data.local.model.LogStatus
import com.tara.dailyn.data.local.model.OverflowPolicy
import com.tara.dailyn.data.local.model.PeriodType
import com.tara.dailyn.data.local.relation.HabitWithRules
import com.tara.dailyn.ui.main.MainActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters

object HabitReminderScheduler {
    const val channelId = "habit_reminders"
    private const val channelName = "Habit reminders"
    private const val extraHabitId = "habit_id"
    private const val maintenanceRequestCode = -7_711_223
    private const val searchHorizonDays = 400L

    suspend fun syncAll(context: Context) = withContext(Dispatchers.IO) {
        ensureNotificationChannel(context)
        val db = AppDatabase.get(context)
        rollOverMissedLogs(db)
        scheduleDailyMaintenance(context)
        db.habitDao().getAllHabitsWithRules().forEach { habitWithRules ->
            syncHabitInternal(context, db.habitLogDao(), habitWithRules)
        }
    }

    suspend fun syncHabit(context: Context, habitId: String) = withContext(Dispatchers.IO) {
        ensureNotificationChannel(context)
        val db = AppDatabase.get(context)
        val relation = db.habitDao().getHabitWithRules(habitId)
        if (relation == null || relation.habit.isArchived) {
            cancelHabit(context, habitId)
            return@withContext
        }
        syncHabitInternal(context, db.habitLogDao(), relation)
    }

    fun cancelHabit(context: Context, habitId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderPendingIntent(context, habitId))
    }

    suspend fun handleAlarm(context: Context, habitId: String) = withContext(Dispatchers.IO) {
        ensureNotificationChannel(context)
        val db = AppDatabase.get(context)
        rollOverMissedLogs(db)
        val relation = db.habitDao().getHabitWithRules(habitId)
        if (relation == null || relation.habit.isArchived) {
            cancelHabit(context, habitId)
            return@withContext
        }

        val reminder = relation.reminders.firstOrNull { it.enabled }
        if (reminder == null) {
            cancelHabit(context, habitId)
            return@withContext
        }

        val today = LocalDate.now()
        if (isDueOnDate(relation, today, db.habitLogDao()) && !isCompletedOnDate(db.habitLogDao(), habitId, today)) {
            showNotification(context, relation)
        }

        syncHabitInternal(context, db.habitLogDao(), relation)
    }

    suspend fun runDailyMaintenance(context: Context) = withContext(Dispatchers.IO) {
        ensureNotificationChannel(context)
        val db = AppDatabase.get(context)
        rollOverMissedLogs(db)
        scheduleDailyMaintenance(context)
        db.habitDao().getAllHabitsWithRules().forEach { habitWithRules ->
            syncHabitInternal(context, db.habitLogDao(), habitWithRules)
        }
    }

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(
            channelId,
            channelName,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Reminder notifications for due habits"
        }
        manager.createNotificationChannel(channel)
    }

    private suspend fun syncHabitInternal(
        context: Context,
        habitLogDao: HabitLogDao,
        relation: HabitWithRules
    ) {
        val reminder = relation.reminders.firstOrNull { it.enabled }
        if (reminder == null) {
            cancelHabit(context, relation.habit.id)
            return
        }

        val nextTrigger = computeNextTriggerAt(relation, reminder, habitLogDao)
        if (nextTrigger == null) {
            cancelHabit(context, relation.habit.id)
            return
        }

        scheduleAlarm(context, relation.habit.id, nextTrigger)
    }

    private suspend fun computeNextTriggerAt(
        relation: HabitWithRules,
        reminder: HabitReminderEntity,
        habitLogDao: HabitLogDao
    ): Long? {
        val now = LocalDateTime.now()
        val time = reminder.timeOfDay

        for (offset in 0..searchHorizonDays) {
            val date = now.toLocalDate().plusDays(offset)
            if (!isDueOnDate(relation, date, habitLogDao)) continue

            val triggerDateTime = LocalDateTime.of(date, time)
            if (!triggerDateTime.isAfter(now)) continue

            return triggerDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
        }

        return null
    }

    private fun scheduleAlarm(context: Context, habitId: String, triggerAtMillis: Long) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(context, habitId)

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            else -> {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun scheduleDailyMaintenance(context: Context) {
        val triggerAtMillis = LocalDate.now()
            .plusDays(1)
            .atTime(LocalTime.MIDNIGHT.plusMinutes(1))
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = maintenancePendingIntent(context)

        when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms() -> {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.M -> {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            else -> {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun reminderPendingIntent(context: Context, habitId: String): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java)
            .putExtra(extraHabitId, habitId)

        return PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun maintenancePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, HabitMaintenanceReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            maintenanceRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun habitIdFromIntent(intent: Intent?): String? = intent?.getStringExtra(extraHabitId)

    private suspend fun isDueOnDate(
        relation: HabitWithRules,
        date: LocalDate,
        habitLogDao: HabitLogDao
    ): Boolean {
        val habit = relation.habit
        if (habit.startDate.isAfter(date)) return false
        if (habit.endDate != null && habit.endDate!!.isBefore(date)) return false

        return when (habit.frequencyType) {
            FrequencyType.EVERY_DAY -> true
            FrequencyType.CUSTOM_WEEKLY -> {
                relation.weeklyDays.any { it.dayOfWeek == date.dayOfWeek.value }
            }
            FrequencyType.SPECIFIC_DATES_OF_MONTH -> {
                relation.monthlyDays.any { matchesDayOfMonth(habit.overflowPolicy, it.dayOfMonth, date) }
            }
            FrequencyType.SOME_DAYS_PER_PERIOD -> {
                val target = habit.someDaysCount ?: return false
                when (habit.periodType) {
                    PeriodType.WEEK -> {
                        val start = date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                        val end = date.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
                        habitLogDao.countDoneInRange(habit.id, LogStatus.DONE, start, end) < target
                    }
                    PeriodType.MONTH -> {
                        val ym = YearMonth.from(date)
                        habitLogDao.countDoneInRange(
                            habit.id,
                            LogStatus.DONE,
                            ym.atDay(1),
                            ym.atEndOfMonth()
                        ) < target
                    }
                    null -> false
                }
            }
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

    private suspend fun isCompletedOnDate(
        habitLogDao: HabitLogDao,
        habitId: String,
        date: LocalDate
    ): Boolean {
        return habitLogDao.countDoneInRange(habitId, LogStatus.DONE, date, date) > 0
    }

    private suspend fun rollOverMissedLogs(db: AppDatabase) {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        if (yesterday.isBefore(LocalDate.MIN.plusDays(1))) return

        val habitLogDao = db.habitLogDao()
        db.habitDao().getAllHabitsWithRules().forEach { relation ->
            val habit = relation.habit
            if (habit.isArchived || habit.startDate.isAfter(yesterday)) return@forEach

            when (habit.frequencyType) {
                FrequencyType.SOME_DAYS_PER_PERIOD -> markMissedQuotaPeriods(habitLogDao, relation, today)
                else -> markMissedScheduledDays(habitLogDao, relation, yesterday)
            }
        }
    }

    private suspend fun markMissedScheduledDays(
        habitLogDao: HabitLogDao,
        relation: HabitWithRules,
        lastDate: LocalDate
    ) {
        val habit = relation.habit
        val endDate = minOf(habit.endDate ?: lastDate, lastDate)
        var cursor = habit.startDate

        while (!cursor.isAfter(endDate)) {
            if (isDueOnDate(relation, cursor, habitLogDao)) {
                upsertMissedLogIfNeeded(
                    habitLogDao = habitLogDao,
                    habitId = habit.id,
                    date = cursor,
                    occurIndex = 0
                )
            }
            cursor = cursor.plusDays(1)
        }
    }

    private suspend fun markMissedQuotaPeriods(
        habitLogDao: HabitLogDao,
        relation: HabitWithRules,
        today: LocalDate
    ) {
        val habit = relation.habit
        val target = habit.someDaysCount ?: return
        var cursor = habit.startDate

        while (true) {
            val (periodStartRaw, periodEndRaw) = periodBounds(habit.periodType, cursor)
            val periodStart = maxOf(periodStartRaw, habit.startDate)
            val configuredEnd = habit.endDate ?: periodEndRaw
            val periodEnd = minOf(periodEndRaw, configuredEnd)
            if (periodStart.isAfter(periodEnd)) break
            if (!periodEnd.isBefore(today)) break

            val doneCount = habitLogDao.countDoneInRange(
                habit.id,
                LogStatus.DONE,
                periodStart,
                periodEnd
            )
            if (doneCount < target) {
                upsertMissedLogIfNeeded(
                    habitLogDao = habitLogDao,
                    habitId = habit.id,
                    date = periodEnd,
                    occurIndex = 1
                )
            }

            cursor = nextPeriodStart(habit.periodType, periodStartRaw)
        }
    }

    private suspend fun upsertMissedLogIfNeeded(
        habitLogDao: HabitLogDao,
        habitId: String,
        date: LocalDate,
        occurIndex: Int
    ) {
        val existing = habitLogDao.findOne(habitId, date, occurIndex)
        when {
            existing == null -> habitLogDao.insert(
                com.tara.dailyn.data.local.entity.HabitLogEntity(
                    habitId = habitId,
                    date = date,
                    occurIndex = occurIndex,
                    status = LogStatus.SKIPPED,
                    completedAt = null
                )
            )
            existing.status == LogStatus.DONE || existing.status == LogStatus.SKIPPED -> Unit
            else -> habitLogDao.updateStatus(existing.id, LogStatus.SKIPPED, null)
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

    private fun showNotification(context: Context, relation: HabitWithRules) {
        if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return

        val openAppIntent = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context,
            relation.habit.id.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val text = relation.habit.description?.takeIf { it.isNotBlank() }
            ?: relation.habit.title

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_dailyn)
            .setContentTitle("Habit reminder")
            .setContentText(text)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("${relation.habit.title} is due now.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context)
            .notify(relation.habit.id.hashCode(), notification)
    }
}
