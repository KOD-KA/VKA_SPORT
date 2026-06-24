package com.vkasport.app.ui.training

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vkasport.app.data.model.WorkoutExercise
import com.vkasport.app.ui.components.VkaButton
import com.vkasport.app.ui.components.VkaCard
import com.vkasport.app.ui.components.formatWeight
import com.vkasport.app.viewmodel.TrainingSessionViewModel


@Composable
fun ExerciseDetailScreen(

    exercise: WorkoutExercise,

    viewModel: TrainingSessionViewModel,

    onBackClick: () -> Unit

) {


    var addingSet by remember {
        mutableStateOf(false)
    }

    var weight by remember {
        mutableStateOf("")
    }

    var reps by remember {
        mutableStateOf("")
    }



    Column(

        modifier =
            Modifier
                .fillMaxSize()
                .padding(16.dp)

    ) {


        VkaButton(
            text = "НАЗАД",
            onClick = onBackClick
        )


        Spacer(
            Modifier.height(16.dp)
        )


        Text(
            text = exercise.name.uppercase(),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )


        Spacer(
            Modifier.height(16.dp)
        )


        VkaCard {


            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement =
                    Arrangement.SpaceBetween
            ){

                Text("#")

                Text(
                    "ПРОШЛАЯ",
                    fontWeight = FontWeight.Bold
                )

                Text(
                    "ТЕКУЩАЯ",
                    fontWeight = FontWeight.Bold
                )

            }



            Spacer(
                Modifier.height(12.dp)
            )



            exercise.sets.forEachIndexed { index, set ->



                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement =
                        Arrangement.SpaceBetween
                ){

                    Text(
                        "${index+1}"
                    )


                    Text(
                        viewModel
                            .getPreviousSet(
                                exercise.name,
                                index
                            )
                            ?.let {
                                "${formatWeight(it.weight)}×${it.reps}"
                            }
                            ?: "---"
                    )


                    Text(
                        "${formatWeight(set.weight)}×${set.reps}" +
                                if(
                                    viewModel.isExerciseRecord(
                                        exercise.name,
                                        set.weight,
                                        set.reps
                                    )
                                )
                                    " 🏆"
                                else
                                    ""
                    )

                }


                Spacer(
                    Modifier.height(8.dp)
                )

            }



            VkaButton(

                text =
                    if(addingSet)
                        "СКРЫТЬ"
                    else
                        "+ ДОБАВИТЬ ПОДХОД",

                onClick = {
                    addingSet = !addingSet
                }

            )



            if(addingSet){


                Spacer(
                    Modifier.height(12.dp)
                )


                OutlinedTextField(
                    value = weight,
                    onValueChange = {
                        weight = it
                    },
                    label = {
                        Text("Вес кг")
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )


                Spacer(
                    Modifier.height(8.dp)
                )


                OutlinedTextField(
                    value = reps,
                    onValueChange = {
                        reps = it
                    },
                    label = {
                        Text("Повторы")
                    },
                    modifier =
                        Modifier.fillMaxWidth()
                )


                Spacer(
                    Modifier.height(12.dp)
                )


                VkaButton(

                    text = "СОХРАНИТЬ",

                    onClick = {


                        val w =
                            weight.toFloatOrNull()
                                ?: return@VkaButton


                        val r =
                            reps.toIntOrNull()
                                ?: return@VkaButton



                        viewModel.addSetToExercise(
                            exercise.name,
                            w,
                            r
                        )


                        weight=""
                        reps=""

                        addingSet=false

                    }

                )

            }

        }

    }

}