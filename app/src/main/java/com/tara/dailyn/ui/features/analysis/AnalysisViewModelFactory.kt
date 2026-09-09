package com.tara.dailyn.ui.features.analysis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tara.dailyn.data.repository.HabitRepository

class AnalysisViewModelFactory(
    private val repo: HabitRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(AnalysisViewModel::class.java))
        return AnalysisViewModel(repo) as T
    }
}
