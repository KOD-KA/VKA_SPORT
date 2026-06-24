package com.vkasport.app.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.data.model.WorkoutExercise
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import com.vkasport.app.ui.components.VkaButton
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import com.vkasport.app.ui.theme.SoftGray
import com.vkasport.app.ui.theme.White
import java.time.format.DateTimeFormatter

private fun formatW(weight: Float): String =
    if (weight == weight.toInt().toFloat()) weight.toInt().toString() else weight.toString()

@Composable
fun TrainingScreen(
    viewModel: TrainingSessionViewModel,
    onSelectMuscleGroup: () -> Unit,
    onFinishWorkout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val fmt = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")
    val weightDiff = viewModel.getWeightDifference()

    val weightLabel = state.athleteWeight?.let { w ->
        buildString {
            append("%.1f".format(w))
            if (weightDiff != null) {
                append(" (")
                append(if (weightDiff >= 0) "+%.1f".format(weightDiff)
                else "%.1f".format(weightDiff))
                append(")")
            }
            append(" кг")
        }
    } ?: "— кг"

    Column(modifier = Modifier.fillMaxSize()) {

        // ===== ШАПКА (чёрный фон) =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Black)
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(
                text = state.selectedMuscleGroup?.title?.uppercase() ?: "ТРЕНИРОВКА",
                color = White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "начало: ${state.workoutStartTime.format(fmt)}",
                    color = White.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
                Text(
                    text = weightLabel,
                    color = White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // ===== УПРАЖНЕНИЯ =====
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(
                items = state.selectedExercises,
                key = { it.name }
            ) { exercise ->
                InlineExerciseBlock(
                    exercise = exercise,
                    viewModel = viewModel
                )
            }
        }

        // ===== КНОПКИ ВНИЗУ =====
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VkaButton(
                text = "+ ДОБАВИТЬ УПРАЖНЕНИЕ",
                onClick = onSelectMuscleGroup
            )
            VkaButton(
                text = "ЗАВЕРШИТЬ ТРЕНИРОВКУ",
                onClick = onFinishWorkout
            )
        }
    }
}

@Composable
private fun InlineExerciseBlock(
    exercise: WorkoutExercise,
    viewModel: TrainingSessionViewModel
) {
    var addingSet by remember(exercise.name) { mutableStateOf(false) }
    var weightInput by remember(exercise.name) { mutableStateOf("") }
    var repsInput by remember(exercise.name) { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(top = 14.dp)
    ) {
        // Шапка упражнения (тёмная)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Black,
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                text = exercise.name,
                color = White,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }

        // Тело блока (белый фон)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    White,
                    RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
                )
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {

            // Заголовок таблицы
            Row(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "#",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkGray,
                    modifier = Modifier.width(28.dp)
                )
                Text(
                    text = "прошлая тренировка",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkGray,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "текущая",
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkGray,
                    modifier = Modifier.weight(1f)
                )
            }

            if (exercise.sets.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = SoftGray)
                Spacer(Modifier.height(6.dp))
            }

            // Строки подходов
            exercise.sets.forEachIndexed { index, set ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(28.dp)
                    )

                    // Прошлая тренировка
                    Text(
                        text = viewModel.getPreviousSet(exercise.name, index)
                            ?.let { "${formatW(it.weight)} × ${it.reps}" }
                            ?: "—",
                        color = DarkGray,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )

                    // Текущая
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${formatW(set.weight)} × ${set.reps}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        if (viewModel.isExerciseRecord(exercise.name, set.weight, set.reps)) {
                            Spacer(Modifier.width(4.dp))
                            Text(text = "🏆", fontSize = 11.sp)
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Добавить подход
            if (!addingSet) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(SoftGray, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(onClick = { addingSet = true }) {
                        Text(
                            text = "+ подход",
                            color = Black,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                // Инлайн форма: кг | повт. | ✓
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    OutlinedTextField(
                        value = weightInput,
                        onValueChange = { weightInput = it },
                        label = { Text("кг") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Black,
                            unfocusedBorderColor = SoftGray,
                            cursorColor = Black,
                            focusedTextColor = Black,
                            unfocusedTextColor = Black,
                            focusedLabelColor = Black
                        )
                    )
                    OutlinedTextField(
                        value = repsInput,
                        onValueChange = { repsInput = it },
                        label = { Text("повт.") },
                        modifier = Modifier.weight(1f),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Black,
                            unfocusedBorderColor = SoftGray,
                            cursorColor = Black,
                            focusedTextColor = Black,
                            unfocusedTextColor = Black,
                            focusedLabelColor = Black
                        )
                    )
                    // Кнопка сохранить
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Black, RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        TextButton(
                            onClick = {
                                val w = weightInput
                                    .replace(",", ".")
                                    .toFloatOrNull()
                                    ?: return@TextButton
                                val r = repsInput.toIntOrNull()
                                    ?: return@TextButton
                                viewModel.addSetToExercise(exercise.name, w, r)
                                weightInput = ""
                                repsInput = ""
                                addingSet = false
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Text(
                                text = "✓",
                                color = White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}