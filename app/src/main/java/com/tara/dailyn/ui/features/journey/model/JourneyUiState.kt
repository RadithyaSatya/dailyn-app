package com.tara.dailyn.ui.features.journey.model

import java.time.LocalDate

enum class JourneyFilterUi(val label: String) {
    ALL("All"),
    COMPLETED("Completed"),
    MISSED("Missed"),
    STREAKS("Streaks"),
    ACHIEVEMENTS("Achievements"),
    CHANGES("Changes"),
    NOTES("Notes")
}

enum class JourneyEventKind {
    COMPLETED,
    MISSED,
    STREAK,
    ACHIEVEMENT,
    CHANGE
}

data class JourneyTimelineItemUi(
    val id: String,
    val kind: JourneyEventKind,
    val title: String,
    val subtitle: String,
    val accentLabel: String = "",
    val isHighlighted: Boolean = false
)

data class JourneyDateSectionUi(
    val date: LocalDate,
    val summaryTitle: String,
    val summarySubtitle: String,
    val items: List<JourneyTimelineItemUi>
)

data class JourneyUiState(
    val selectedFilter: JourneyFilterUi = JourneyFilterUi.ALL,
    val sections: List<JourneyDateSectionUi> = emptyList(),
    val isLoading: Boolean = true
)
