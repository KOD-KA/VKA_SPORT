package com.vkasport.app.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import com.vkasport.app.ui.calendar.CalendarScreen
import com.vkasport.app.ui.info.InfoScreen
import com.vkasport.app.ui.profile.ProfileScreen
import com.vkasport.app.ui.records.WorkoutArchiveScreen
import com.vkasport.app.ui.training.TrainingFlowScreen
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import com.vkasport.app.viewmodel.WorkoutViewModel

// TrainingSessionViewModel создаётся в MainScreen.kt и передаётся сюда
// параметром (MainScreen централизованно управляет статус-баром).
// Инстанс один на всё приложение.
@Composable
fun AppNavigation(
    viewModel: WorkoutViewModel,
    trainingViewModel: TrainingSessionViewModel,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {

    LaunchedEffect(trainingViewModel) {
        trainingViewModel.loadArchiveFromDatabase()
        trainingViewModel.loadRecordsFromDatabase()
        trainingViewModel.loadCustomExercises()
        trainingViewModel.loadInProgressWorkout()
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize()
    ) { page ->
        when (page) {
            0 -> TrainingFlowScreen(viewModel = trainingViewModel)

            1 -> {
                // Обновляем архив/рекорды при каждом заходе на вкладку
                LaunchedEffect(Unit) {
                    trainingViewModel.loadArchiveFromDatabase()
                    trainingViewModel.loadRecordsFromDatabase()
                }
                WorkoutArchiveScreen(viewModel = trainingViewModel)
            }

            2 -> CalendarScreen(viewModel = trainingViewModel)

            3 -> InfoScreen(viewModel = trainingViewModel)

            // ДОБАВЛЕНО (этап «профиль»): 5-я вкладка
            4 -> ProfileScreen(viewModel = trainingViewModel)
        }
    }
}