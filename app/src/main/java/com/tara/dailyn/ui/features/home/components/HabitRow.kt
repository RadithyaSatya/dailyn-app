package com.tara.dailyn.ui.features.home.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.tara.dailyn.ui.features.home.model.HabitUi

@Composable
fun HabitRow(
    habit: HabitUi,
    onClick: () -> Unit,
    onToggle: () -> Unit,
    toggleEnabled: Boolean = true,
    dragEnabled: Boolean = false,
    dragHandle: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val contentAlpha = if (habit.isCompletedToday) 0.55f else 1f

    Surface(
        shape = MaterialTheme.shapes.medium,
        tonalElevation = 1.dp,
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .alpha(contentAlpha)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = habit.categoryIcon.ifBlank { "✨" },
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(end = 12.dp)
            )
            Column(
                Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Text(
                    text = habit.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (habit.isCompletedToday) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (habit.description.isNotBlank()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = habit.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Checkbox(
                checked = habit.isCompletedToday,
                onCheckedChange = if (toggleEnabled) ({ onToggle() }) else null
            )
            if (dragEnabled && dragHandle != null) {
                Spacer(Modifier.width(8.dp))
                Box(contentAlignment = Alignment.Center) {
                    dragHandle()
                }
            }
        }
    }
}
