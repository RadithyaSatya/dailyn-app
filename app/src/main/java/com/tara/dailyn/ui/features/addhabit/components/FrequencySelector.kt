@file:OptIn(ExperimentalMaterial3Api::class)

package com.tara.dailyn.ui.features.addhabit.components
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.tara.dailyn.ui.features.addhabit.model.FrequencyType
import com.tara.dailyn.R

@Composable
fun FrequencySelector(
    selected: FrequencyType,
    onSelected: (FrequencyType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    val options: List<Pair<FrequencyType, Int>> = listOf(
        FrequencyType.EVERY_DAY to R.string.frequency_type_every_day,
        FrequencyType.SOME_DAYS_PER_PERIOD to R.string.frequency_type_some_days_per_period,
        FrequencyType.SPECIFIC_DAYS_OF_WEEK to R.string.frequency_type_specific_days_of_week,
        FrequencyType.SPECIFIC_DAY_OF_MONTH to R.string.frequency_type_specific_days_of_month,
    )

    val selectedLabel = stringResource(options.first { it.first == selected }.second)

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor().fillMaxWidth(),
            value = selectedLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.frequency_type_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { (value, labelRes) ->
                DropdownMenuItem(
                    text = { Text(stringResource(labelRes)) },
                    onClick = {
                        expanded = false
                        onSelected(value)
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, name = "")
@Composable
fun FrequencySelectorPreview(){
    MaterialTheme{
        Box(modifier = Modifier.fillMaxSize()) {
            Box(
                Modifier.fillMaxSize()
            ){
                FrequencySelector(
                    selected = FrequencyType.SPECIFIC_DAY_OF_MONTH,
                    onSelected = {}
                )
            }
        }
    }
}