package com.tara.dailyn.ui.features.addhabit.model

sealed interface FormMode {
    data object Create : FormMode
    data class Edit(val habitId: String) : FormMode
}