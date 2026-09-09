package com.tara.dailyn.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.ui.graphics.vector.ImageVector
import com.tara.dailyn.R

sealed class Screen(
    val route: String,
    @StringRes val titleRes: Int,
    val icon: ImageVector
) {
    object Home : Screen("home", R.string.tab_home_title, Icons.Default.Home)
    object Habits : Screen("habits", R.string.tab_habits_title, Icons.AutoMirrored.Filled.ListAlt)
    object Analysis : Screen("analysis", R.string.tab_analysis_title, Icons.Default.BarChart)
    object AddHabit : Screen("addHabit", R.string.tab_add_habit, Icons.Default.Add)
    object Journey : Screen("journey", R.string.tab_journey_title, Icons.Default.AutoStories)
}

// extra routes (bukan tab)
object AppRoute {
    const val HabitDetail = "habit/{habitId}"
    const val EditHabit = "habit/{habitId}/edit"
    const val Settings = "settings"
    const val ManageCategories = "settings/categories"
    fun habitDetail(habitId: String) = "habit/$habitId"
    fun editHabit(habitId: String) = "habit/$habitId/edit"
}
