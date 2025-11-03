package com.tara.dailyn.ui.features.habitdetail

import android.app.AlertDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tara.dailyn.ui.features.habitdetail.model.HabitDetailUi
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitDetailScreen(
    ui: HabitDetailUi,
    onBack: () -> Unit,
    onEdit: () -> Unit,
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
                        Icon(Icons.Rounded.ArrowBack, contentDescription = null)
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

        LinearProgressIndicator(progress = ui.completion7d / 7f)
        Text("${ui.completion7d}/7 days completed")

        Divider()
        Text("Recent Logs", style = MaterialTheme.typography.titleSmall)
        ui.recentLogs.forEach { log ->
            ListItem(
                headlineContent = { Text(log.dateLabel) },
                supportingContent = { Text(if (log.done) "Done" else "Missed") }
            )
        }
    }
}

