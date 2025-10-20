import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tara.dailyn.ui.features.addhabit.model.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalTime

class AddHabitViewModel : ViewModel() {

    private val _state = MutableStateFlow(AddHabitUiState())
    val state: StateFlow<AddHabitUiState> = _state.asStateFlow()

    private val _effects = MutableSharedFlow<AddHabitEffect>()
    val effects = _effects.asSharedFlow()

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
                        selectedDaysOfWeek = s.selectedDaysOfWeek, // keep
                        specificDaysOfMonth = emptySet(),
                        someDaysCount = null
                    )
                    FrequencyType.SPECIFIC_DAY_OF_MONTH -> s.copy(
                        frequencyType = event.type,
                        selectedDaysOfWeek = emptySet(),
                        specificDaysOfMonth = s.specificDaysOfMonth, // keep
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

            // GANTI: handler untuk toggle tanggal (multiple select)
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
        val s = _state.value

        if (s.title.isBlank()) {
            update { it.copy(titleError = "Title tidak boleh kosong") }
            _effects.emit(AddHabitEffect.ValidationError)
            return@launch
        }

        when (s.frequencyType) {
            FrequencyType.SPECIFIC_DAYS_OF_WEEK -> {
                if (s.selectedDaysOfWeek.isEmpty()) {
                    _effects.emit(AddHabitEffect.ValidationError); return@launch
                }
            }
            FrequencyType.SPECIFIC_DAY_OF_MONTH -> {
                if (s.specificDaysOfMonth.isEmpty()) {
                    _effects.emit(AddHabitEffect.ValidationError); return@launch
                }
            }
            FrequencyType.SOME_DAYS_PER_PERIOD -> {
                if (s.someDaysCount == null || s.someDaysCount <= 0) {
                    _effects.emit(AddHabitEffect.ValidationError); return@launch
                }
            }
            else -> Unit
        }

        update { it.copy(isSaving = true) }

        _effects.emit(
            AddHabitEffect.Saved(
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
