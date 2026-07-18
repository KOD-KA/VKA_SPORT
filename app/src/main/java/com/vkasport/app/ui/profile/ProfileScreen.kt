package com.vkasport.app.ui.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import com.vkasport.app.ui.theme.SoftGray
import com.vkasport.app.ui.theme.White
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * Вкладка «Профиль» (страница 4 Pager).
 *
 * Этап «профиль»: каркас + сводная статистика + «О приложении» внизу.
 * Следующие этапы добавят сюда разделы: бэкап/восстановление, тело и
 * замеры с графиками, прогресс и сравнение с нормативами, пожертвования.
 */
@Composable
fun ProfileScreen(viewModel: com.vkasport.app.viewmodel.TrainingSessionViewModel) {

    val workouts by viewModel.completedWorkouts.collectAsState()
    val records by viewModel.exerciseHistory.collectAsState()

    // workouts отсортирован DESC (новые первые) → самая ПЕРВАЯ тренировка
    // в жизни — это ПОСЛЕДНИЙ элемент списка. Здесь lastOrNull() корректен.
    val firstWorkoutDate = workouts.lastOrNull()?.dateTime?.toLocalDate()
    val daysSinceStart = firstWorkoutDate?.let {
        ChronoUnit.DAYS.between(it, LocalDate.now())
    }
    val last30 = workouts.count { it.dateTime.toLocalDate().isAfter(LocalDate.now().minusDays(30)) }
    val totalVolumeKg = workouts.sumOf { w ->
        w.exercises.sumOf { ex -> ex.sets.sumOf { (it.weight * it.reps).toDouble() } }
    }
    val currentWeight = workouts.firstOrNull()?.athleteWeight

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
    ) {

        Spacer(Modifier.height(16.dp))

        // ===== ШАПКА =====
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(Black, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = "Профиль",
                    tint = White,
                    modifier = Modifier.size(30.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column {
                Text("ПРОФИЛЬ", color = Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                currentWeight?.let {
                    Text(
                        "Текущий вес: %.1f кг".format(it),
                        color = DarkGray,
                        fontSize = 13.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // ===== СВОДНАЯ СТАТИСТИКА (2×2) =====
        Row(Modifier.fillMaxWidth()) {
            StatCard(
                value = workouts.size.toString(),
                label = "тренировок всего",
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            StatCard(
                value = last30.toString(),
                label = "за 30 дней",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            StatCard(
                value = records.size.toString(),
                label = "рекордов",
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            StatCard(
                value = daysSinceStart?.let { "$it" } ?: "—",
                label = "дней с первой тренировки",
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Общий поднятый объём — на всю ширину
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Black, RoundedCornerShape(16.dp))
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = if (totalVolumeKg >= 1000)
                        "%.1f т".format(totalVolumeKg / 1000.0)
                    else
                        "%.0f кг".format(totalVolumeKg),
                    color = White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "общий поднятый объём",
                    color = White.copy(alpha = 0.65f),
                    fontSize = 12.sp
                )
            }
        }

        // Здесь появятся разделы следующих этапов:
        // бэкап, тело и замеры, прогресс, сравнение, пожертвования

        Spacer(Modifier.weight(1f, fill = true))
        Spacer(Modifier.height(28.dp))

        // ===== О ПРИЛОЖЕНИИ (внизу профиля) =====
        val context = LocalContext.current
        val versionName = remember {
            try {
                context.packageManager.getPackageInfo(context.packageName, 0).versionName
            } catch (e: Exception) {
                null
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftGray, RoundedCornerShape(16.dp))
                .padding(16.dp)
        ) {
            Text("О ПРИЛОЖЕНИИ", color = Black, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "VKA SPORT — дневник тренировок: подходы, рекорды, архив, " +
                        "календарь с напоминаниями и таймер отдыха.",
                color = DarkGray,
                fontSize = 12.sp,
                lineHeight = 17.sp
            )
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = White, thickness = 1.dp)
            Spacer(Modifier.height(10.dp))
            Text(
                text = "VKA SPORT" + (versionName?.let { " · версия $it" } ?: ""),
                color = DarkGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun StatCard(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(SoftGray, RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(value, color = Black, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(2.dp))
        Text(
            label,
            color = DarkGray,
            fontSize = 11.sp,
            textAlign = TextAlign.Center
        )
    }
}