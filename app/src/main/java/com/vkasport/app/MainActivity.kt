package com.vkasport.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.vkasport.app.data.local.database.AppDatabaseProvider
import com.vkasport.app.data.repository.WorkoutRepository
import com.vkasport.app.ui.main.MainScreen
import com.vkasport.app.viewmodel.WorkoutViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = AppDatabaseProvider.getDatabase(this)

        val repository =
            WorkoutRepository(
                db.workoutDao()
            )

        val viewModel =
            WorkoutViewModel(
                repository
            )

        setContent {
            MainScreen(
                viewModel = viewModel
            )
        }
    }
}