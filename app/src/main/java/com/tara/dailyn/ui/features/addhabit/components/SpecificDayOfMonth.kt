package com.tara.dailyn.ui.features.addhabit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SpecificDayOfMonthField(
    selectedDays: Set<Int>,
    onToggle: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val days = (1..31).toList()

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "Select specific days of the month",
            style = MaterialTheme.typography.titleSmall
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 4.dp)
        ) {
            items(days) { day ->
                FilterChip(
                    selected = day in selectedDays,
                    onClick = { onToggle(day) },
                    label = { Text(day.toString()) }
                )
            }
        }

        if (selectedDays.isNotEmpty()) {
            Text(
                text = "Repeats every month on days: ${
                    selectedDays.sorted().joinToString(", ")
                }"
            )
        } else {
            Text(text = "No day selected")
        }
    }
}
