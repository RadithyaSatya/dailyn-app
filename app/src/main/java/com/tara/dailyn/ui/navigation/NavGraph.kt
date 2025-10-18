package com.tara.dailyn.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.tara.dailyn.ui.features.analysis.AnalysisRoute
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
        composable(Screen.Home.route) { HomeRoute() }
        composable(Screen.Analysis.route) { AnalysisRoute() }
        composable(Screen.Diary.route) { DiaryRoute() }
    }
}
