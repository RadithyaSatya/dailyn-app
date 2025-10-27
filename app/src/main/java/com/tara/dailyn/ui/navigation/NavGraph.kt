package com.tara.dailyn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tara.dailyn.ui.features.addhabit.AddHabitRoute
import com.tara.dailyn.ui.features.diary.DiaryRoute
import com.tara.dailyn.ui.features.home.HomeRoute

@Composable
fun NavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) {
            HomeRoute(
                onAddHabitClick = { navController.navigate(Screen.AddHabit.route) }
            )
        }

        composable(Screen.AddHabit.route) {
            AddHabitRoute(
                onSaved = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                }
            )
        }

        composable(Screen.Diary.route) { DiaryRoute() }
    }
}
