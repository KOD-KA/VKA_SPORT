package com.vkasport.app.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.White
import androidx.compose.foundation.clickable

@Composable
fun WorkoutSummaryScreen(
    viewModel: TrainingSessionViewModel,
    onFinish: () -> Unit
) {
    val exercisesCount = viewModel.getCurrentExercisesCount()
    val setsCount = viewModel.getCurrentSetsCount()
    val volume = viewModel.getCurrentVolume()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Black),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {

            // Трофей
            Text(
                text = "🏋️",
                fontSize = 72.sp
            )

            Spacer(Modifier.height(20.dp))

            // Заголовок
            Text(
                text = "ТРЕНИРОВКА\nЗАВЕРШЕНА",
                color = White,
                fontSize = 30.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                lineHeight = 38.sp
            )

            Spacer(Modifier.height(40.dp))

            // Три блока статистики
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatBlock(
                    value = "$exercisesCount",
                    label = "упражнений"
                )
                StatBlock(
                    value = "$setsCount",
                    label = "подходов"
                )
                StatBlock(
                    value = "${volume.toInt()}",
                    label = "кг объём"
                )
            }

            Spacer(Modifier.height(56.dp))

            // Кнопка ГОТОВО (белая на чёрном фоне)
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .widthIn(min = 220.dp, max = 320.dp)
                    .background(White, RoundedCornerShape(12.dp))
                    .clickable { onFinish() }
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "ГОТОВО",
                    color = Black,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun StatBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = White,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            color = White.copy(alpha = 0.55f),
            fontSize = 12.sp
        )
    }
}