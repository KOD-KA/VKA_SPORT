package com.vkasport.app.ui.training


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vkasport.app.data.local.entity.WorkoutEntity
import com.vkasport.app.ui.components.VkaButton
import com.vkasport.app.ui.components.VkaCard
import com.vkasport.app.viewmodel.WorkoutViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale



@Composable
fun CurrentTrainingScreen(

    viewModel: WorkoutViewModel

) {


    val workouts by
    viewModel.workouts.collectAsState()



    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)

    ) {



        Text(

            text = "Тренировки",

            style =
                androidx.compose.material3.MaterialTheme
                    .typography
                    .titleLarge

        )



        Spacer(
            modifier = Modifier.height(12.dp)
        )



        VkaButton(

            text = "НАЧАТЬ ТРЕНИРОВКУ",

            onClick = {


                viewModel.addWorkout(

                    WorkoutEntity(

                        startTime =
                            System.currentTimeMillis(),

                        athleteWeight =
                            80f

                    )

                )

            }

        )



        Spacer(
            modifier = Modifier.height(16.dp)
        )



        LazyColumn {


            items(workouts) { workout ->


                WorkoutItem(

                    workout = workout,

                    viewModel = viewModel

                )

            }

        }

    }

}




@Composable
fun WorkoutItem(

    workout: WorkoutEntity,

    viewModel: WorkoutViewModel

) {


    val date =

        SimpleDateFormat(

            "dd.MM.yyyy HH:mm",

            Locale.getDefault()

        )
            .format(
                Date(workout.startTime)
            )



    VkaCard(

        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp)

    ) {



        Text(

            text =
                "Тренировка №${workout.id}",

            style =
                androidx.compose.material3.MaterialTheme
                    .typography
                    .titleMedium

        )



        Spacer(
            modifier = Modifier.height(8.dp)
        )



        Text(

            text =
                "Вес: ${workout.athleteWeight} кг",

            style =
                androidx.compose.material3.MaterialTheme
                    .typography
                    .bodyMedium

        )



        Text(

            text =
                "Дата: $date",

            style =
                androidx.compose.material3.MaterialTheme
                    .typography
                    .bodySmall

        )



        Spacer(
            modifier = Modifier.height(12.dp)
        )



        VkaButton(

            text = "УДАЛИТЬ",

            onClick = {

                viewModel.deleteWorkout(
                    workout
                )

            }

        )

    }

}