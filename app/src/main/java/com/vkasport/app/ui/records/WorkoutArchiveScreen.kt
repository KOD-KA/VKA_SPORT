package com.vkasport.app.ui.records

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.data.model.CompletedWorkout
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import com.vkasport.app.ui.theme.SoftGray
import com.vkasport.app.ui.theme.White
import java.time.format.DateTimeFormatter

private fun formatWeightArchive(weight: Float): String =
    if (weight == weight.toInt().toFloat()) weight.toInt().toString() else weight.toString()

@Composable
fun WorkoutArchiveScreen(viewModel: TrainingSessionViewModel) {

    val workouts by viewModel.completedWorkouts.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }

    Column(modifier = Modifier.fillMaxSize()) {

        // ===== КАСТОМНЫЕ ТАБЫ =====
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = 20.dp)
        ) {
            TabItem(title = "РЕКОРДЫ", selected = selectedTab == 0) { selectedTab = 0 }
            Spacer(Modifier.width(28.dp))
            TabItem(title = "АРХИВ", selected = selectedTab == 1) { selectedTab = 1 }
        }

        HorizontalDivider(color = SoftGray, thickness = 1.dp)

        // ===== КОНТЕНТ =====
        when (selectedTab) {
            0 -> ExerciseRecordsScreen(viewModel = viewModel)
            1 -> ArchiveContent(workouts = workouts.reversed())
        }
    }
}

@Composable
private fun TabItem(title: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Black else DarkGray
        )
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .width(if (selected) 48.dp else 0.dp)
                .height(3.dp)
                .background(Black, RoundedCornerShape(2.dp))
        )
    }
}

@Composable
private fun ArchiveContent(workouts: List<CompletedWorkout>) {

    if (workouts.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = "Тренировок пока нет",
                style = MaterialTheme.typography.bodyMedium,
                color = DarkGray
            )
        }
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        contentPadding = PaddingValues(vertical = 16.dp)
    ) {
        items(workouts) { workout ->
            ArchiveWorkoutCard(workout = workout)
        }
    }
}

@Composable
private fun ArchiveWorkoutCard(workout: CompletedWorkout) {

    var expanded by remember { mutableStateOf(false) }

    val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yy")
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    val hours = workout.durationMinutes / 60
    val mins = workout.durationMinutes % 60
    val durationText = if (hours > 0) "$hours ч $mins мин" else "$mins мин"

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(16.dp))
            .clickable { expanded = !expanded }
    ) {

        // Тёмная шапка: дата + время
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Black,
                    RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                )
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = workout.dateTime.format(dateFmt),
                    color = White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = workout.dateTime.format(timeFmt),
                    color = White.copy(alpha = 0.8f),
                    fontSize = 14.sp
                )
            }
        }

        // Тело карточки
        Column(modifier = Modifier.padding(14.dp)) {

            // Группы мышц
            Text(
                text = workout.muscleGroup,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = DarkGray
            )

            Spacer(Modifier.height(10.dp))

            // Статистика: вес + длительность + кол-во упражнений
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Вес атлета
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${(workout.athleteWeight ?: 0f).toInt()}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        text = "кг",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 3.dp)
                    )
                }

                // Длительность
                Text(
                    text = durationText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DarkGray
                )

                // Кол-во упражнений
                Box(
                    modifier = Modifier
                        .background(SoftGray, RoundedCornerShape(8.dp))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "× ${workout.exercises.size}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            // Раскрытые детали — упражнения и подходы
            AnimatedVisibility(visible = expanded && workout.exercises.isNotEmpty()) {
                Column {
                    Spacer(Modifier.height(12.dp))
                    HorizontalDivider(color = SoftGray)
                    Spacer(Modifier.height(12.dp))

                    workout.exercises.forEach { exercise ->

                        Text(
                            text = exercise.name,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Spacer(Modifier.height(4.dp))

                        exercise.sets.forEachIndexed { idx, set ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${idx + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = DarkGray,
                                    modifier = Modifier.width(20.dp)
                                )
                                Text(
                                    text = "${formatWeightArchive(set.weight)} кг × ${set.reps}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Spacer(Modifier.weight(1f))
                            }
                        }

                        Spacer(Modifier.height(10.dp))
                    }
                }
            }
        }
    }
}