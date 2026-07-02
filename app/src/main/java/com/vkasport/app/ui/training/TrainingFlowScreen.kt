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
import com.vkasport.app.ui.theme.SystemBarsAppearance
import com.vkasport.app.ui.theme.White
import com.vkasport.app.viewmodel.TrainingSessionViewModel

@Composable
fun TrainingFlowScreen(viewModel: TrainingSessionViewModel) {

    val state by viewModel.state.collectAsState()
    val completedWorkouts by viewModel.completedWorkouts.collectAsState()
    val customExercises by viewModel.customExercises.collectAsState()
    val currentScreen = state.currentScreen

    val (barColor, darkIcons) = when (currentScreen) {
        "start", "weight", "summary" -> Black to false
        "muscles"                    -> White to true
        else                         -> Black to false
    }
    SystemBarsAppearance(statusBarColor = barColor, darkIcons = darkIcons)

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

            val liveWeight = weightText.replace(",", ".").toFloatOrNull()
            val liveDiff = if (liveWeight != null && lastWeight != null) liveWeight - lastWeight else null

            Column(
                modifier = Modifier.fillMaxSize().background(Black),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text("ВАШ ВЕС СЕГОДНЯ", color = White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))

                lastWeight?.let {
                    Text("В прошлый раз: %.1f кг".format(it), color = White.copy(alpha = 0.5f), fontSize = 13.sp)
                }

                Spacer(Modifier.height(28.dp))

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("кг", color = White.copy(alpha = 0.6f)) },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(color = White),
                    modifier = Modifier.width(180.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = White, unfocusedBorderColor = White.copy(alpha = 0.4f),
                        cursorColor = White, focusedTextColor = White, unfocusedTextColor = White
                    ),
                    singleLine = true
                )

                if (liveDiff != null) {
                    Spacer(Modifier.height(10.dp))
                    val diffColor = when {
                        liveDiff > 0f -> androidx.compose.ui.graphics.Color(0xFF4CAF50)
                        liveDiff < 0f -> androidx.compose.ui.graphics.Color(0xFFE53935)
                        else -> White.copy(alpha = 0.6f)
                    }
                    val sign = if (liveDiff >= 0f) "+" else ""
                    Text("$sign%.1f кг".format(liveDiff), color = diffColor, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(Modifier.height(28.dp))

                Box(
                    modifier = Modifier
                        .height(50.dp)
                        .widthIn(min = 180.dp, max = 260.dp)
                        .background(White, RoundedCornerShape(12.dp))
                        .clickable {
                            liveWeight?.let { w -> viewModel.updateAthleteWeight(w) }
                            viewModel.setCurrentScreen("muscles")
                        }
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("ДАЛЕЕ →", color = Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // ================= ГРУППЫ МЫШЦ =================
        "muscles" -> {
            MuscleGroupScreen(
                onBack = {
                    if (state.selectedExercises.isEmpty()) viewModel.setCurrentScreen("weight")
                    else viewModel.setCurrentScreen("training")
                },
                onGroupSelected = {
                    viewModel.selectMuscleGroup(it)
                    viewModel.setCurrentScreen("exercises")
                }
            )
        }

        // ================= УПРАЖНЕНИЯ =================
        "exercises" -> {
            val group = state.selectedMuscleGroup
            group?.let { g ->
                val lastGroupExercises = remember(g, completedWorkouts) {
                    viewModel.getLastMuscleGroupExercises(g)
                        .filter { it !in state.selectedExercises.map { ex -> ex.name } }
                }
                ExerciseSelectionScreen(
                    muscleGroup = g,
                    alreadyAdded = state.selectedExercises.map { ex -> ex.name },
                    customExercises = customExercises.filter { it.muscleGroup == g }.map { it.name },
                    lastGroupExercises = lastGroupExercises,
                    onBack = { viewModel.setCurrentScreen("muscles") },
                    onRepeatLastGroup = {
                        viewModel.repeatLastMuscleGroup(g)
                        viewModel.setCurrentScreen("training")
                    },
                    onExerciseSelected = { ex ->
                        viewModel.addExercise(ex)
                        viewModel.setCurrentScreen("training")
                    },
                    onAddCustomExercise = { name ->
                        viewModel.addCustomExercise(name, g)
                        viewModel.addExercise(name)
                        viewModel.setCurrentScreen("training")
                    }
                )
            }
        }

        // ================= ТРЕНИРОВКА =================
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
                onFinish = { viewModel.resetWorkout() }
            )
        }
    }
}