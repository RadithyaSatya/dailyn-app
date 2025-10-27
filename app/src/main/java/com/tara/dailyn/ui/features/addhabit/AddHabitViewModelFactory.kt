// ui/features/addhabit/AddHabitViewModelFactory.kt
package com.tara.dailyn.ui.features.addhabit

import AddHabitViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tara.dailyn.data.repository.HabitRepository

class AddHabitViewModelFactory(
    private val repo: HabitRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AddHabitViewModel::class.java))
        return AddHabitViewModel(repo) as T
    }
}
