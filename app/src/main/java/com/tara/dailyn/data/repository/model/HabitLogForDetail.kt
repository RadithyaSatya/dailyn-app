package com.tara.dailyn.data.repository.model

import java.time.LocalDate

data class HabitLogForDetail(
    val date: LocalDate,
    val statusDone: Boolean
)
