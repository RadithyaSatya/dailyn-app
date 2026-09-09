package com.tara.dailyn.ui.features.habitdetail

import android.app.AlertDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.tara.dailyn.ui.features.habitdetail.model.HabitCalendarDayState
import com.tara.dailyn.ui.features.habitdetail.model.HabitCalendarDayUi
import com.tara.dailyn.ui.features.habitdetail.model.HabitDetailUi
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    ui: HabitDetailUi,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onDeleteConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(ui.title.ifBlank { "Habit Detail" }) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Rounded.Edit, contentDescription = null)
                    }
                    IconButton(onClick = { showDelete = true }) {
                        Icon(Icons.Rounded.Delete, contentDescription = null)
                    }
                }
            )
        }
    ) { pad ->
        when {
            ui.isLoading -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(pad),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            ui.error != null -> {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(pad),
                    contentAlignment = androidx.compose.ui.Alignment.Center
                ) {
                    Text("Error: ${ui.error}")
                }
            }
            else -> {
                HabitDetailContent(
                    ui = ui,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    modifier = Modifier.padding(pad)
                )
            }
        }
    }

    if (showDelete) {
        AlertDialog(
            onDismissRequest = { showDelete = false },
            title = { Text("Delete habit?") },
            text = { Text("This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDelete = false
                        onDeleteConfirm()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDelete = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HabitDetailContent(
    ui: HabitDetailUi,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (ui.description.isNotBlank()) {
            Text(ui.description, style = MaterialTheme.typography.bodyLarge)
        }
        AssistChip(onClick = {}, label = { Text(ui.scheduleText) })
        Text("Streak: ${ui.streak} 🔥", style = MaterialTheme.typography.titleMedium)

        HorizontalDivider()
        HabitHistoryCalendar(
            visibleMonth = ui.visibleMonth,
            days = ui.calendarDays,
            canGoToPreviousMonth = ui.canGoToPreviousMonth,
            canGoToNextMonth = ui.canGoToNextMonth,
            onPreviousMonth = onPreviousMonth,
            onNextMonth = onNextMonth
        )
    }
}

@Composable
private fun HabitHistoryCalendar(
    visibleMonth: YearMonth,
    days: List<HabitCalendarDayUi>,
    canGoToPreviousMonth: Boolean,
    canGoToNextMonth: Boolean,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("History Calendar", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onPreviousMonth, enabled = canGoToPreviousMonth) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Previous month")
            }
            Text(
                text = visibleMonth.formatMonthLabel(),
                style = MaterialTheme.typography.labelLarge
            )
            IconButton(onClick = onNextMonth, enabled = canGoToNextMonth) {
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Next month")
            }
        }

        CalendarWeekHeader()

        days.chunked(7).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                week.forEach { day ->
                    CalendarDayCell(
                        day = day,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        CalendarLegend()
    }
}

@Composable
private fun CalendarWeekHeader() {
    val labels = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        labels.forEach { label ->
            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: HabitCalendarDayUi,
    modifier: Modifier = Modifier
) {
    val colors = dayColors(day.state)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(14.dp))
            .background(colors.container)
            .border(1.dp, colors.border, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (day.state != HabitCalendarDayState.OUTSIDE_MONTH) {
            Text(
                text = day.dayOfMonth.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = colors.content
            )
        }
    }
}

@Composable
private fun CalendarLegend() {
    val items = listOf(
        "Done" to HabitCalendarDayState.DONE,
        "Missed" to HabitCalendarDayState.MISSED,
        "Scheduled" to HabitCalendarDayState.SCHEDULED,
        "Empty" to HabitCalendarDayState.IDLE
    )
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items.forEach { (label, state) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(dayColors(state).container)
                        .border(1.dp, dayColors(state).border, RoundedCornerShape(4.dp))
                )
                Text(label, style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun dayColors(state: HabitCalendarDayState): CalendarDayColors {
    val scheme = MaterialTheme.colorScheme
    return when (state) {
        HabitCalendarDayState.OUTSIDE_MONTH -> CalendarDayColors(Color.Transparent, Color.Transparent, Color.Transparent)
        HabitCalendarDayState.FUTURE -> CalendarDayColors(
            container = scheme.surfaceVariant.copy(alpha = 0.35f),
            border = scheme.outline.copy(alpha = 0.2f),
            content = scheme.onSurface.copy(alpha = 0.35f)
        )
        HabitCalendarDayState.IDLE -> CalendarDayColors(
            container = scheme.surfaceVariant.copy(alpha = 0.55f),
            border = scheme.outline.copy(alpha = 0.25f),
            content = scheme.onSurfaceVariant
        )
        HabitCalendarDayState.SCHEDULED -> CalendarDayColors(
            container = scheme.tertiary.copy(alpha = 0.16f),
            border = scheme.tertiary.copy(alpha = 0.45f),
            content = scheme.tertiary
        )
        HabitCalendarDayState.DONE -> CalendarDayColors(
            container = scheme.primary,
            border = scheme.primary,
            content = scheme.onPrimary
        )
        HabitCalendarDayState.MISSED -> CalendarDayColors(
            container = scheme.error.copy(alpha = 0.14f),
            border = scheme.error.copy(alpha = 0.45f),
            content = scheme.error
        )
    }
}

private data class CalendarDayColors(
    val container: Color,
    val border: Color,
    val content: Color
)

private fun YearMonth.formatMonthLabel(): String {
    val monthName = month.getDisplayName(TextStyle.FULL, Locale.getDefault())
    return "$monthName $year"
}
