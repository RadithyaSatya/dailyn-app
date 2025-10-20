@file:OptIn(ExperimentalMaterial3Api::class)

package com.tara.dailyn.ui.features.addhabit

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tara.dailyn.R
import com.tara.dailyn.ui.features.addhabit.components.DaysOfWeekSelector
import com.tara.dailyn.ui.features.addhabit.components.FrequencySelector
import com.tara.dailyn.ui.features.addhabit.components.SomeDaysPerPeriodFields
import com.tara.dailyn.ui.features.addhabit.components.SpecificDayOfMonthField
import com.tara.dailyn.ui.features.addhabit.model.AddHabitEvent
import com.tara.dailyn.ui.features.addhabit.model.AddHabitUiState
import com.tara.dailyn.ui.features.addhabit.model.FrequencyType
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AddHabitScreen(
    state: AddHabitUiState,
    onEvent: (AddHabitEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }

    fun showTimePicker(initial: LocalTime?) {
        val time = initial ?: LocalTime.of(7, 0)
        TimePickerDialog(
            context,
            { _, h, m -> onEvent(AddHabitEvent.ReminderTimeChanged(LocalTime.of(h, m))) },
            time.hour, time.minute, true
        ).show()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_habit_title)) },
                navigationIcon = {
                    IconButton(onClick = { onEvent(AddHabitEvent.CancelClicked) }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back_button)
                        )
                    }
                },
                actions = {
                    val canSave = !state.isSaving && state.title.isNotBlank()
                    TextButton(
                        onClick = { onEvent(AddHabitEvent.SaveClicked) },
                        enabled = canSave
                    ) {
                        Text(
                            if (state.isSaving)
                                stringResource(R.string.saving)
                            else
                                stringResource(R.string.save)
                        )
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = state.title,
                    onValueChange = { onEvent(AddHabitEvent.TitleChanged(it)) },
                    label = { Text(stringResource(R.string.habit_title_label)) },
                    isError = state.titleError != null,
                    supportingText = { state.titleError?.let { Text(it) } },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = { onEvent(AddHabitEvent.DescriptionChanged(it)) },
                    label = { Text(stringResource(R.string.habit_description_label)) },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item { Spacer(Modifier.height(8.dp)) }
            item {
                OutlinedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = stringResource(R.string.frequency),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )

                        FrequencySelector(
                            selected = state.frequencyType,
                            onSelected = { onEvent(AddHabitEvent.FrequencyChanged(it)) }
                        )

                        when (state.frequencyType) {
                            FrequencyType.SPECIFIC_DAYS_OF_WEEK -> {
                                DaysOfWeekSelector(
                                    state.selectedDaysOfWeek
                                ) { onEvent(AddHabitEvent.ToggleDayOfWeek(it)) }
                            }
                            FrequencyType.SPECIFIC_DAY_OF_MONTH -> {
                                SpecificDayOfMonthField(
                                    selectedDays = state.specificDaysOfMonth,
                                    onToggle = { onEvent(AddHabitEvent.ToggleSpecificDayOfMonth(it)) }
                                )
                            }
                            FrequencyType.SOME_DAYS_PER_PERIOD -> {
                                SomeDaysPerPeriodFields(
                                    count = state.someDaysCount,
                                    onCountChange = { onEvent(AddHabitEvent.SomeDaysCountChanged(it)) },
                                    period = state.periodType,
                                    onPeriodChange = { onEvent(AddHabitEvent.PeriodChanged(it)) }
                                )
                            }
                            else -> Unit
                        }
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = state.reminderEnabled,
                        onCheckedChange = { onEvent(AddHabitEvent.ReminderEnabledChanged(it)) }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.reminder))
                }
            }

            if (state.reminderEnabled) {
                item {
                    OutlinedButton(onClick = { showTimePicker(state.reminderTime) }) {
                        Text(
                            state.reminderTime?.format(timeFormatter)
                                ?: stringResource(R.string.pick_time)
                        )
                    }
                }
            }

            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Preview(showBackground = true, name = "Add Habit Preview")
@Composable
fun AddHabitScreenPreview() {
    AddHabitScreen(
        state = AddHabitUiState(),
        onEvent = {}
    )
}
