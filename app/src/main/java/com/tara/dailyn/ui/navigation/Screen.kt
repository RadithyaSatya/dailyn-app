package com.tara.dailyn.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
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
    object AddHabit : Screen("addHabit", R.string.tab_add_habit, Icons.Default.Add)
    object Diary : Screen("diary", R.string.tab_dairy_title, Icons.Default.AccountBox)
}

// extra routes (bukan tab)
object AppRoute {
    const val HabitDetail = "habit/{habitId}"
    const val EditHabit = "habit/{habitId}/edit"
    fun habitDetail(habitId: String) = "habit/$habitId"
    fun editHabit(habitId: String) = "habit/$habitId/edit"
}
