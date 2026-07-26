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
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.vkasport.app.data.local.database.AppDatabaseProvider
import com.vkasport.app.navigation.AppNavigation
import com.vkasport.app.navigation.BottomNavItem
import com.vkasport.app.ui.components.VkaBottomBar
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
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

    // Запуск плановой тренировки из календаря — переключаемся на вкладку 0
    val navToTraining by trainingViewModel.navigateToTraining.collectAsState()
    LaunchedEffect(navToTraining) {
        if (navToTraining > 0) pagerState.animateScrollToPage(0, animationSpec = tween(420))
    }

    // п6: системная кнопка «назад» на под-экранах тренировки возвращает
    // на предыдущий под-экран (в т.ч. с «введите вес» → на старт), а не
    // сворачивает приложение. Активен только когда открыта вкладка тренировки.
    // v1.6.1 (п2): с экрана самой тренировки выход — через ДВА подтверждения:
    // 1) «Вы уверены, что хотите вернуться назад?» — всегда;
    // 2) «Данные будут утеряны» — только если хоть один подход уже записан.
    var showExitConfirm by remember { mutableStateOf(false) }
    var showLossConfirm by remember { mutableStateOf(false) }
    val hasEnteredData = trainingState.selectedExercises.any { it.sets.isNotEmpty() }

    BackHandler(
        enabled = pagerState.currentPage == 0 &&
                trainingState.currentScreen in listOf("weight", "muscles", "exercises", "training")
    ) {
        when (trainingState.currentScreen) {
            "weight"    -> trainingViewModel.setCurrentScreen("start")
            "muscles"   -> trainingViewModel.setCurrentScreen(
                if (trainingState.selectedExercises.isEmpty()) "weight" else "training")
            "exercises" -> trainingViewModel.setCurrentScreen("muscles")
            "training"  -> showExitConfirm = true
        }
    }

    // Подтверждение 1: точно назад?
    if (showExitConfirm) {
        AlertDialog(
            onDismissRequest = { showExitConfirm = false },
            containerColor = White,
            titleContentColor = Black,
            textContentColor = DarkGray,
            title = { Text("Вы уверены, что хотите вернуться назад?", fontWeight = FontWeight.Bold) },
            text = { Text("Ведение тренировки будет закрыто.") },
            confirmButton = {
                TextButton(onClick = {
                    showExitConfirm = false
                    if (hasEnteredData) showLossConfirm = true
                    else trainingViewModel.resetWorkout()
                }) { Text("Да, назад", color = Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showExitConfirm = false }) { Text("Отмена", color = DarkGray) }
            }
        )
    }

    // Подтверждение 2: данные будут потеряны
    if (showLossConfirm) {
        AlertDialog(
            onDismissRequest = { showLossConfirm = false },
            containerColor = White,
            titleContentColor = Black,
            textContentColor = DarkGray,
            title = { Text("Данные будут утеряны", fontWeight = FontWeight.Bold) },
            text = { Text("В тренировке есть записанные подходы. Если выйти, они не сохранятся.") },
            confirmButton = {
                TextButton(onClick = {
                    showLossConfirm = false
                    trainingViewModel.resetWorkout()
                }) { Text("Всё равно выйти", color = Black, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showLossConfirm = false }) { Text("Отмена", color = DarkGray) }
            }
        )
    }

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
                        pagerState.animateScrollToPage(index, animationSpec = tween(420))
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