package com.vkasport.app.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.White
import com.vkasport.app.viewmodel.TrainingSessionViewModel

@Composable
fun TrainingFlowScreen(viewModel: TrainingSessionViewModel) {

    val state by viewModel.state.collectAsState()
    val completedWorkouts by viewModel.completedWorkouts.collectAsState()
    val currentScreen = state.currentScreen

    // Вес из последней тренировки — для пре-заполнения поля
    val lastWeight = completedWorkouts.lastOrNull()?.athleteWeight

    when (currentScreen) {

        // ================= СТАРТ =================
        "start" -> {
            StartWorkoutScreen(
                viewModel = viewModel,
                onStart = { viewModel.startTraining() }
            )
        }

        // ================= ВЕС =================
        "weight" -> {

            var weightText by remember(lastWeight) {
                mutableStateOf(lastWeight?.let { "%.1f".format(it) } ?: "")
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Black),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    text = "ВАШ ВЕС СЕГОДНЯ",
                    color = White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(Modifier.height(8.dp))

                lastWeight?.let {
                    Text(
                        text = "В прошлый раз: %.1f кг".format(it),
                        color = White.copy(alpha = 0.5f),
                        fontSize = 13.sp
                    )
                }

                Spacer(Modifier.height(28.dp))

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("кг", color = White.copy(alpha = 0.6f)) },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(color = White),
                    modifier = Modifier.width(180.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = White,
                        unfocusedBorderColor = White.copy(alpha = 0.4f),
                        cursorColor = White,
                        focusedTextColor = White,
                        unfocusedTextColor = White
                    ),
                    singleLine = true
                )

                Spacer(Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .height(50.dp)
                        .widthIn(min = 180.dp, max = 260.dp)
                        .background(White, RoundedCornerShape(12.dp))
                        .clickable {
                            weightText.replace(",", ".").toFloatOrNull()?.let { w ->
                                viewModel.updateAthleteWeight(w)
                            }
                            viewModel.setCurrentScreen("muscles")
                        }
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "ДАЛЕЕ →",
                        color = Black,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // ================= ГРУППЫ МЫШЦ =================
        // Этот экран открывается и для первой группы, и когда пользователь
        // нажимает "Добавить группу мышц" на экране тренировки
        "muscles" -> {
            MuscleGroupScreen(
                onGroupSelected = {
                    viewModel.selectMuscleGroup(it)
                    viewModel.setCurrentScreen("exercises")
                }
            )
        }

        // ================= УПРАЖНЕНИЯ =================
        // Список фильтруется по state.selectedMuscleGroup — эта группа
        // устанавливается либо на экране "muscles", либо напрямую при
        // нажатии "+" внутри уже существующей группы на экране тренировки
        "exercises" -> {
            val group = state.selectedMuscleGroup
            group?.let {
                ExerciseSelectionScreen(
                    muscleGroup = it,
                    alreadyAdded = state.selectedExercises.map { ex -> ex.name },
                    onExerciseSelected = { ex ->
                        viewModel.addExercise(ex)
                        viewModel.setCurrentScreen("training")
                    }
                )
            }
        }

        // ================= ТРЕНИРОВКА =================
        // Упражнения сгруппированы по группам мышц; можно добавить ещё
        // упражнение в существующую группу или добавить новую группу целиком
        "training" -> {
            TrainingScreen(
                viewModel = viewModel,
                onAddExerciseToGroup = { group ->
                    viewModel.selectMuscleGroup(group)
                    viewModel.setCurrentScreen("exercises")
                },
                onAddNewGroup = {
                    viewModel.setCurrentScreen("muscles")
                },
                onFinishWorkout = {
                    viewModel.finishCurrentWorkout()
                }
            )
        }

        // ================= ИТОГИ =================
        "summary" -> {
            WorkoutSummaryScreen(
                viewModel = viewModel,
                onFinish = {
                    viewModel.resetWorkout()
                }
            )
        }
    }
}