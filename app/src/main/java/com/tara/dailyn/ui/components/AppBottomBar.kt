package com.tara.dailyn.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.tara.dailyn.ui.navigation.Screen
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.graphics.Shape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.LaunchedEffect

@Composable
fun AppBottomBar(
    navController: NavController,
    shape: Shape = RoundedCornerShape(24.dp),
) {
    val items = listOf(Screen.Home, Screen.Habits, Screen.Analysis, Screen.Journey)
    val currentRoute by navController.currentBackStackEntryAsState()

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 0.dp, end = 16.dp, bottom = 8.dp, start = 16.dp)
            .navigationBarsPadding(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Surface(
            shape = shape,
            tonalElevation = 6.dp,
            shadowElevation = 0.dp,
            color = MaterialTheme.colorScheme.surface
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .height(56.dp)
                    .clip(shape)
            ) {
                items.forEach { screen ->
                    val selected = currentRoute?.destination?.route == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            if (!selected) {
                                navController.navigate(screen.route) {
                                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        },
                        icon = { Icon(screen.icon, contentDescription = stringResource(screen.titleRes)) },
//                        label = { Text(stringResource(screen.titleRes)) }
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, name = "Floating Bottom Bar")
@Composable
private fun AppBottomBarPreview() {
    val nav = rememberNavController()
    LaunchedEffect(Unit) { runCatching { nav.navigate(Screen.Home.route) } }

    MaterialTheme {
        Box(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(bottom = 96.dp)
            )

            AppBottomBar(nav)
        }
    }
}
