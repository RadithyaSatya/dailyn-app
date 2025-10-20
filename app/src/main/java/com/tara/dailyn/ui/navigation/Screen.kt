package com.tara.dailyn.ui.navigation

import android.graphics.drawable.Icon
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.tara.dailyn.R

sealed class Screen(val route: String, @StringRes val titleRes: Int, val icon: ImageVector){
    object Home: Screen("home", R.string.tab_home_title, Icons.Default.Home)
    object AddHabit: Screen("addHabit", R.string.tab_add_habit, Icons.Default.Add)
    object Diary: Screen("diary", R.string.tab_dairy_title, Icons.Default.AccountBox)
}