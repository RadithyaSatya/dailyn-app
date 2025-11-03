import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tara.dailyn.data.repository.HabitRepository
import com.tara.dailyn.ui.features.addhabit.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

class AddHabitViewModel(
    private val repository: HabitRepository,
    private val mode: FormMode
) : ViewModel() {

    private val _state = MutableStateFlow(AddHabitUiState())
    val state: StateFlow<AddHabitUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<AddHabitEffect>()
    val effects: SharedFlow<AddHabitEffect> = _effects.asSharedFlow()

    init {
        // Prefill jika Edit
        if (mode is FormMode.Edit) {
            viewModelScope.launch {
                // Ambil data dari repo → mapping ke UI
                val data = repository.getHabitForEdit(mode.habitId)
                // NOTE: implementasikan mapping ini sesuai model entity/domain kamu
                _state.update { s ->
                    s.copy(
                        title = data.title,
                        description = data.description ?: "",
                        frequencyType = data.uiFrequencyType,                  // FrequencyType
                        periodType = data.uiPeriodType,                        // PeriodType
                        selectedDaysOfWeek = data.selectedDaysOfWeek.toSet(),  // Set<DayOfWeek>
                        specificDaysOfMonth = data.specificDaysOfMonth.toSet(),// Set<Int>
                        someDaysCount = data.someDaysCount,
                        reminderEnabled = data.reminderEnabled,
                        reminderTime = data.reminderTime
                    )
                }
            }
        }
    }

    fun onEvent(event: AddHabitEvent) {
        when (event) {
            is AddHabitEvent.TitleChanged -> update { it.copy(title = event.value, titleError = null) }
            is AddHabitEvent.DescriptionChanged -> update { it.copy(description = event.value) }

            is AddHabitEvent.FrequencyChanged -> update { s ->
                when (event.type) {
                    FrequencyType.EVERY_DAY -> s.copy(
                        frequencyType = event.type,
                        selectedDaysOfWeek = emptySet(),
                        specificDaysOfMonth = emptySet(),
                        someDaysCount = null
                    )
                    FrequencyType.SPECIFIC_DAYS_OF_WEEK -> s.copy(
                        frequencyType = event.type,
                        selectedDaysOfWeek = s.selectedDaysOfWeek,
                        specificDaysOfMonth = emptySet(),
                        someDaysCount = null
                    )
                    FrequencyType.SPECIFIC_DAY_OF_MONTH -> s.copy(
                        frequencyType = event.type,
                        selectedDaysOfWeek = emptySet(),
                        specificDaysOfMonth = s.specificDaysOfMonth,
                        someDaysCount = null
                    )
                    FrequencyType.SOME_DAYS_PER_PERIOD -> s.copy(
                        frequencyType = event.type,
                        selectedDaysOfWeek = emptySet(),
                        specificDaysOfMonth = emptySet(),
                        someDaysCount = s.someDaysCount
                    )
                }
            }

            is AddHabitEvent.ToggleDayOfWeek -> update { s ->
                s.copy(selectedDaysOfWeek = s.selectedDaysOfWeek.toggle(event.day))
            }

            is AddHabitEvent.ToggleSpecificDayOfMonth -> update { s ->
                val newSet = if (event.day in s.specificDaysOfMonth)
                    s.specificDaysOfMonth - event.day
                else
                    s.specificDaysOfMonth + event.day
                s.copy(specificDaysOfMonth = newSet)
            }

            is AddHabitEvent.SomeDaysCountChanged -> update { s ->
                s.copy(someDaysCount = event.count)
            }

            is AddHabitEvent.PeriodChanged -> update { it.copy(periodType = event.period) }

            is AddHabitEvent.ReminderEnabledChanged -> update { s ->
                s.copy(
                    reminderEnabled = event.enabled,
                    reminderTime = if (event.enabled) (s.reminderTime ?: LocalTime.of(8, 0)) else null
                )
            }

            is AddHabitEvent.ReminderTimeChanged -> update { s ->
                s.copy(reminderTime = event.time)
            }

            AddHabitEvent.SaveClicked -> onSave()
            AddHabitEvent.CancelClicked -> viewModelScope.launch { _effects.emit(AddHabitEffect.Cancelled) }
        }
    }

    private inline fun update(block: (AddHabitUiState) -> AddHabitUiState) {
        _state.update(block)
    }

    private fun onSave() = viewModelScope.launch {
        val s = state.value
        update { it.copy(isSaving = true) }

        val id = when (val m = mode) {
            is FormMode.Create -> {
                repository.createHabit(
                    title = s.title,
                    description = s.description,
                    uiFrequency = s.frequencyType,
                    selectedDaysOfWeek = s.selectedDaysOfWeek,
                    specificDaysOfMonth = s.specificDaysOfMonth,
                    someDaysCount = s.someDaysCount,
                    uiPeriodType = s.periodType,
                    reminderEnabled = s.reminderEnabled,
                    reminderTime = s.reminderTime
                )
            }
            is FormMode.Edit -> {
                repository.updateHabit(
                    habitId = m.habitId,
                    title = s.title,
                    description = s.description,
                    uiFrequency = s.frequencyType,
                    selectedDaysOfWeek = s.selectedDaysOfWeek,
                    specificDaysOfMonth = s.specificDaysOfMonth,
                    someDaysCount = s.someDaysCount,
                    uiPeriodType = s.periodType,
                    reminderEnabled = s.reminderEnabled,
                    reminderTime = s.reminderTime
                )
                m.habitId // return existing id
            }
        }

        _effects.emit(
            AddHabitEffect.Saved(
                id = id,
                title = s.title.trim(),
                description = s.description.trim(),
                frequencyType = s.frequencyType,
                selectedDaysOfWeek = s.selectedDaysOfWeek,
                someDaysCount = s.someDaysCount,
                periodType = s.periodType,
                reminderEnabled = s.reminderEnabled,
                reminderTime = s.reminderTime,
                specificDaysOfMonth = s.specificDaysOfMonth
            )
        )
        update { it.copy(isSaving = false) }
    }
}

private fun Set<DayOfWeek>.toggle(day: DayOfWeek): Set<DayOfWeek> =
    if (contains(day)) this - day else this + day
