package com.tara.dailyn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.tara.dailyn.ui.features.addhabit.AddHabitRoute
import com.tara.dailyn.ui.features.addhabit.model.FormMode
import com.tara.dailyn.ui.features.diary.DiaryRoute
import com.tara.dailyn.ui.features.habitdetail.HabitDetailRoute
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
                onAddHabitClick = {
                    navController.navigate(Screen.AddHabit.route)
                },
                onHabitClick = { habitId ->
                    navController.navigate(AppRoute.habitDetail(habitId))
                }
            )
        }

        composable(Screen.AddHabit.route) {
            AddHabitRoute(
                onSaved = { _ ->
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onCancel = {
                    navController.popBackStack()
                },
                mode = FormMode.Create
            )
        }

        composable(
            route = AppRoute.EditHabit,
            arguments = listOf(navArgument("habitId") { type = NavType.StringType })
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments!!.getString("habitId")!!
            AddHabitRoute(
                onSaved = { _ ->
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() },
                mode = FormMode.Edit(habitId)
            )
        }


        composable(Screen.Diary.route) {
            DiaryRoute()
        }

        // DETAIL HABIT
        composable(
            route = AppRoute.HabitDetail,
            arguments = listOf(
                navArgument("habitId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments!!.getString("habitId")!!
            HabitDetailRoute(
                habitId = habitId,
                onEdit = {
                    navController.navigate(AppRoute.editHabit(habitId))
                },
                onBack = {
                    navController.popBackStack()
                },
                onDeleted = {
                    navController.popBackStack(Screen.Home.route, inclusive = false)
                }
            )
        }

        composable(
            route = AppRoute.EditHabit,
            arguments = listOf(
                navArgument("habitId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val habitId = backStackEntry.arguments!!.getString("habitId")!!
            AddHabitRoute(
                onSaved = { _ ->
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                onCancel = { navController.popBackStack() },
                mode = FormMode.Edit(habitId)
            )
        }
    }
}
