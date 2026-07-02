package com.vkasport.app.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vkasport.app.navigation.AppNavigation
import com.vkasport.app.navigation.BottomNavItem
import com.vkasport.app.ui.components.VkaBottomBar
import com.vkasport.app.ui.theme.SystemBarsAppearance
import com.vkasport.app.ui.theme.White
import com.vkasport.app.viewmodel.WorkoutViewModel

@Composable
fun MainScreen(
    viewModel: WorkoutViewModel
) {

    val navController = rememberNavController()

    val items = listOf(
        BottomNavItem.Training,
        BottomNavItem.Records,
        BottomNavItem.Calendar,
        BottomNavItem.Info
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Базовый цвет статус-бара для вкладок с белой шапкой (Рекорды/Архив,
    // Календарь, Инфо). Экран "Тренировка" переопределяет его сам —
    // см. TrainingFlowScreen, т.к. там фон меняется по шагам.
    SystemBarsAppearance(statusBarColor = White, darkIcons = true)

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            VkaBottomBar(
                items = items,
                currentRoute = currentRoute
            ) { item ->
                // ВАЖНО: не вызываем navigate(), если пользователь уже
                // находится на этом экране — повторный вызов navigate()
                // на тот же route заставлял NavHost пересоздавать экран
                // при каждом нажатии, из-за чего он "моргал".
                if (currentRoute != item.route) {
                    navController.navigate(item.route) {
                        // Сохраняем состояние вкладок при переключении —
                        // так они не пересоздаются с нуля (тоже устраняет
                        // моргание) и не сбрасывают скролл/данные.
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    ) { padding ->

        AppNavigation(
            viewModel = viewModel,
            navController = navController,
            modifier = Modifier.padding(padding)
        )
    }
}