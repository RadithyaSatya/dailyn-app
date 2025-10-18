package com.tara.dailyn.ui.features.home.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DayChip(
    date: LocalDate,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dow = date.format(DateTimeFormatter.ofPattern("EEE", Locale.getDefault()))
    val d = date.format(DateTimeFormatter.ofPattern("d", Locale.getDefault()))
    val mmm = date.format(DateTimeFormatter.ofPattern("MMM", Locale.getDefault()))

    ElevatedCard(
        onClick = onClick,
        colors = if (selected)
            CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        else
            CardDefaults.elevatedCardColors(),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(dow.uppercase(), style = MaterialTheme.typography.titleMedium)
            Text(d, style = MaterialTheme.typography.titleMedium)
            Text(mmm, style = MaterialTheme.typography.labelSmall)
        }
    }
}
