package com.vkasport.app.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.White
import java.time.format.DateTimeFormatter

@Composable
fun StartWorkoutScreen(
    viewModel: TrainingSessionViewModel,
    onStart: () -> Unit
) {
    val workouts by viewModel.completedWorkouts.collectAsState()
    val lastWorkout = workouts.firstOrNull()
    val daysSince = viewModel.getDaysSinceLastWorkout()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Black),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Spacer(Modifier.weight(1f))

        // ===== ЛОГОТИП =====
        Text(
            text = "VKA SPORT",
            color = White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(Modifier.height(10.dp))

        // ===== СЧЁТЧИК ДНЕЙ =====
        val daysLabel = when (daysSince) {
            null -> "Первая тренировка"
            0L   -> "Сегодня уже тренировался 💪"
            1L   -> "1 день без тренировки"
            else -> "$daysSince дней без тренировки"
        }

        Text(
            text = daysLabel,
            color = White.copy(alpha = 0.55f),
            fontSize = 14.sp
        )

        Spacer(Modifier.weight(1f))

        // ===== КАРТОЧКА ПОСЛЕДНЕЙ ТРЕНИРОВКИ =====
        if (lastWorkout != null) {

            val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yy")
            val hours   = lastWorkout.durationMinutes / 60
            val mins    = lastWorkout.durationMinutes % 60
            val durText = if (hours > 0) "$hours ч $mins мин" else "$mins мин"

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .background(
                        White.copy(alpha = 0.1f),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "Последняя тренировка",
                    color = White.copy(alpha = 0.5f),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = lastWorkout.dateTime.format(dateFmt),
                    color = White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = lastWorkout.muscleGroup,
                    color = White.copy(alpha = 0.75f),
                    fontSize = 14.sp
                )

                Spacer(Modifier.height(12.dp))

                // Статистика: упражнения / подходы / длительность
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    LastWorkoutStat(
                        value = "${lastWorkout.exercises.size}",
                        label = "упр."
                    )
                    LastWorkoutStat(
                        value = "${lastWorkout.exercises.sumOf { it.sets.size }}",
                        label = "под."
                    )
                    LastWorkoutStat(
                        value = durText,
                        label = ""
                    )
                    lastWorkout.athleteWeight?.let { w ->
                        LastWorkoutStat(
                            value = "%.1f".format(w),
                            label = "кг"
                        )
                    }
                }
            }

            Spacer(Modifier.height(32.dp))
        } else {
            Spacer(Modifier.height(32.dp))
        }

        // ===== КНОПКА СТАРТ =====
        Box(
            modifier = Modifier
                .height(56.dp)
                .widthIn(min = 240.dp, max = 340.dp)
                .background(White, RoundedCornerShape(14.dp))
                .clickable { onStart() }
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "НАЧАТЬ ТРЕНИРОВКУ",
                color = Black,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(56.dp))
    }
}

@Composable
private fun LastWorkoutStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = White,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )
        if (label.isNotEmpty()) {
            Text(
                text = label,
                color = White.copy(alpha = 0.5f),
                fontSize = 11.sp
            )
        }
    }
}