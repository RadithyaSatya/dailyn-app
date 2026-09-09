package com.tara.dailyn.ui.features.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.tara.dailyn.data.preferences.AppSettings
import com.tara.dailyn.data.repository.HabitRepository

class HomeViewModelFactory(
    private val repo: HabitRepository,
    private val appSettings: AppSettings
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        require(modelClass.isAssignableFrom(HomeViewModel::class.java))
        return HomeViewModel(repo, appSettings) as T
    }
}
