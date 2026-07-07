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

    // ИСПРАВЛЕНО: убран общий вызов SystemBarsAppearance(White,true) отсюда.
    // Раньше он конкурировал с вызовами внутри TrainingFlowScreen — оба
    // выполнялись при каждой рекомпозиции, и порядок применения не был
    // гарантирован, из-за чего статус-бар иногда оставался с тёмными
    // (невидимыми на чёрном фоне) иконками. Теперь каждый экран,
    // где реально нужен белый фон статус-бара (Рекорды/Архив, Календарь,
    // Инфо), сам вызывает SystemBarsAppearance у себя — без конфликтов.
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            VkaBottomBar(
                items = items,
                currentRoute = currentRoute
            ) { item ->
                if (currentRoute != item.route) {
                    navController.navigate(item.route) {
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