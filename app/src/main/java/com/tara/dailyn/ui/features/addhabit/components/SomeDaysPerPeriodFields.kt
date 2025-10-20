@file:OptIn(ExperimentalMaterial3Api::class)

package com.tara.dailyn.ui.features.addhabit.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.tara.dailyn.R
import com.tara.dailyn.ui.features.addhabit.model.PeriodType

@Composable
fun SomeDaysPerPeriodFields(
    count: Int?,
    onCountChange: (Int?) -> Unit,
    period: PeriodType,
    onPeriodChange: (PeriodType) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.some_days_per_period_title),
            style = MaterialTheme.typography.titleSmall
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedTextField(
                value = (count ?: 1).toString(),
                onValueChange = { onCountChange(it.toIntOrNull()?.coerceAtLeast(1)) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = { Text(stringResource(R.string.some_days_per_period_days_label)) },
                singleLine = true,
                modifier = Modifier.width(120.dp)
            )

            var expanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }) {
                OutlinedTextField(
                    value = if (period == PeriodType.WEEK)
                        stringResource(R.string.period_week)
                    else
                        stringResource(R.string.period_month),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(stringResource(R.string.some_days_per_period_period_label)) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                    modifier = Modifier.menuAnchor().width(150.dp)
                )
                ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.period_week)) },
                        onClick = { expanded = false; onPeriodChange(PeriodType.WEEK) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.period_month)) },
                        onClick = { expanded = false; onPeriodChange(PeriodType.MONTH) }
                    )
                }
            }
        }

        Text(
            text = stringResource(
                R.string.some_days_per_period_example,
                count ?: 1,
                stringResource(
                    if (period == PeriodType.WEEK)
                        R.string.period_week
                    else
                        R.string.period_month
                )
            )
        )
    }
}
