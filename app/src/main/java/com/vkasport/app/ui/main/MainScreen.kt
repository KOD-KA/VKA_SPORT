package com.vkasport.app.ui.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vkasport.app.data.local.database.AppDatabaseProvider
import com.vkasport.app.navigation.AppNavigation
import com.vkasport.app.navigation.BottomNavItem
import com.vkasport.app.ui.components.VkaBottomBar
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.SystemBarsAppearance
import com.vkasport.app.ui.theme.White
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import com.vkasport.app.viewmodel.WorkoutViewModel
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: WorkoutViewModel
) {

    // ДОБАВЛЕНО (этап «профиль»): 5-я вкладка Profile
    val items = listOf(
        BottomNavItem.Training,
        BottomNavItem.Records,
        BottomNavItem.Calendar,
        BottomNavItem.Info,
        BottomNavItem.Profile
    )

    // Состояние текущей вкладки хранит PagerState — он же используется и для
    // свайпа, и для подсветки нижней навигации, всегда синхронизированы.
    val pagerState = rememberPagerState(pageCount = { items.size })
    val coroutineScope = rememberCoroutineScope()

    // TrainingSessionViewModel создаётся здесь (единственное место, которое
    // одновременно знает вкладку Pager и под-экран тренировки — только здесь
    // можно принять ЕДИНСТВЕННОЕ решение о цвете статус-бара, без гонок).
    val context = LocalContext.current
    val database = remember { AppDatabaseProvider.getDatabase(context) }
    val trainingViewModel = remember { TrainingSessionViewModel(database) }
    val trainingState by trainingViewModel.state.collectAsState()

    // ЕДИНСТВЕННЫЙ источник правды для статус-бара во всём приложении.
    // Вкладка 0 (тренировка): все под-экраны чёрные, кроме "muscles".
    // Вкладки 1–4 (рекорды/календарь/инфо/профиль): всегда белые.
    val isBlackBar = pagerState.currentPage == 0 && trainingState.currentScreen != "muscles"
    val barColor = if (isBlackBar) Black else White
    SystemBarsAppearance(darkIcons = !isBlackBar)

    Box(Modifier.fillMaxSize()) {

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                VkaBottomBar(
                    items = items,
                    selectedIndex = pagerState.currentPage
                ) { index ->
                    coroutineScope.launch {
                        pagerState.animateScrollToPage(index)
                    }
                }
            }
        ) { padding ->

            AppNavigation(
                viewModel = viewModel,
                trainingViewModel = trainingViewModel,
                pagerState = pagerState,
                modifier = Modifier.padding(padding)
            )
        }

        // Полоса-подложка под системными иконками (время/батарея).
        // С API 35 window.statusBarColor игнорируется системой — цвет
        // статус-бара может нарисовать только само приложение.
        Box(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .windowInsetsTopHeight(WindowInsets.statusBars)
                .background(barColor)
        )
    }
}