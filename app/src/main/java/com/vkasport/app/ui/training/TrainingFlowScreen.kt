package com.vkasport.app.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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
    val customExercises by viewModel.customExercises.collectAsState()
    val currentScreen = state.currentScreen

    // ИЗМЕНЕНО: статус-баром теперь управляет ТОЛЬКО MainScreen.kt
    // (единая точка, без гонок между страницами Pager).

    // Уведомление-таймер в шторке: показывается пока идёт экран тренировки,
    // убирается при выходе на итоги/старт. Системный chronometer в
    // уведомлении тикает сам — обновлять уведомление каждую секунду не нужно.
    val restTimerStart by viewModel.restTimerStart.collectAsState()
    val notifContext = androidx.compose.ui.platform.LocalContext.current
    LaunchedEffect(currentScreen, restTimerStart) {
        val start = restTimerStart
        if (currentScreen == "training" && start != null) {
            com.vkasport.app.notifications.RestTimerNotification.show(notifContext, start)
        } else {
            com.vkasport.app.notifications.RestTimerNotification.cancel(notifContext)
        }
    }

    val lastWeight = completedWorkouts.firstOrNull()?.athleteWeight

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

                val focusManager = LocalFocusManager.current
                fun confirmWeight() {
                    liveWeight?.let { w -> viewModel.updateAthleteWeight(w) }
                    focusManager.clearFocus()
                    // Если упражнения уже загружены (запуск плановой тренировки) —
                    // сразу к тренировке, иначе к выбору групп мышц
                    if (state.selectedExercises.isNotEmpty()) viewModel.setCurrentScreen("training")
                    else viewModel.setCurrentScreen("muscles")
                }

                OutlinedTextField(
                    value = weightText,
                    onValueChange = { weightText = it },
                    label = { Text("кг", color = White.copy(alpha = 0.6f)) },
                    textStyle = MaterialTheme.typography.headlineMedium.copy(color = White),
                    modifier = Modifier.width(180.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Decimal, imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { confirmWeight() }),
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
                        .clickable { confirmWeight() }
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
                    onRenameCustom = { oldName, newName ->
                        viewModel.renameCustomExercise(g, oldName, newName) { _, msg ->
                            android.widget.Toast.makeText(notifContext, msg, android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    onAddCustomExercise = { name, measureType ->
                        viewModel.addCustomExercise(name, g, measureType)
                        // Передаём тип явно: своё упражнение могло ещё не
                        // успеть загрузиться из БД к моменту добавления
                        viewModel.addExercise(name, measureType)
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