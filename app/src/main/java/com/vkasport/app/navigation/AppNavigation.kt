package com.vkasport.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vkasport.app.data.local.database.AppDatabaseProvider
import com.vkasport.app.ui.calendar.CalendarScreen
import com.vkasport.app.ui.info.InfoScreen
import com.vkasport.app.ui.records.WorkoutArchiveScreen
import com.vkasport.app.ui.training.TrainingFlowScreen
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import com.vkasport.app.viewmodel.WorkoutViewModel

@Composable
fun AppNavigation(
    viewModel: WorkoutViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val database = remember { AppDatabaseProvider.getDatabase(context) }
    val trainingViewModel = remember { TrainingSessionViewModel(database) }

    // Загружаем данные один раз при создании ViewModel, а не при
    // каждой рекомпозиции AppNavigation (раньше .also{} внутри remember
    // вызывался на каждый ре-рендер, порождая лишние запросы к БД —
    // одна из причин "моргания" при частых нажатиях).
    LaunchedEffect(trainingViewModel) {
        trainingViewModel.loadArchiveFromDatabase()
        trainingViewModel.loadRecordsFromDatabase()
        trainingViewModel.loadCustomExercises()
    }

    NavHost(
        navController  = navController,
        startDestination = Screen.Training.route,
        modifier       = modifier
    ) {

        composable(Screen.Training.route) {
            TrainingFlowScreen(viewModel = trainingViewModel)
        }

        composable(Screen.Records.route) {
            // Обновляем данные при каждом заходе на вкладку (например,
            // после завершения тренировки), но только один раз за вход,
            // а не на каждую рекомпозицию.
            LaunchedEffect(Unit) {
                trainingViewModel.loadArchiveFromDatabase()
                trainingViewModel.loadRecordsFromDatabase()
            }
            WorkoutArchiveScreen(viewModel = trainingViewModel)
        }

        composable(Screen.Calendar.route) {
            CalendarScreen(viewModel = trainingViewModel)
        }

        composable(Screen.Info.route) {
            InfoScreen(viewModel = trainingViewModel)
        }
    }
}