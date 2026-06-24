package com.vkasport.app.navigation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.vkasport.app.ui.training.TrainingFlowScreen
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import com.vkasport.app.viewmodel.WorkoutViewModel
import androidx.compose.runtime.remember
import com.vkasport.app.ui.records.WorkoutArchiveScreen
import androidx.compose.ui.platform.LocalContext
import com.vkasport.app.data.local.database.AppDatabaseProvider

@Composable
fun AppNavigation(
    viewModel: WorkoutViewModel,
    navController: NavHostController,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current

    // ИСПРАВЛЕНО: используем единый AppDatabaseProvider вместо
    // второй базы "workout_database". Раньше было два разных файла БД,
    // из-за чего архив и рекорды всегда были пустыми.
    val database = remember {
        AppDatabaseProvider.getDatabase(context)
    }

    val trainingViewModel = remember {
        TrainingSessionViewModel(database)
    }.also {
        it.loadArchiveFromDatabase()
        it.loadRecordsFromDatabase()
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Training.route,
        modifier = modifier
    ) {

        composable(Screen.Training.route) {
            TrainingFlowScreen(viewModel = trainingViewModel)
        }

        composable(Screen.Records.route) {

            trainingViewModel.loadArchiveFromDatabase()
            trainingViewModel.loadRecordsFromDatabase()

            WorkoutArchiveScreen(viewModel = trainingViewModel)
        }

        composable(Screen.Calendar.route) {
            Text("Календарь (скоро)")
        }

        composable(Screen.Info.route) {
            Text("Информация (скоро)")
        }
    }
}