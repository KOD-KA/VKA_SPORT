package com.vkasport.app.ui.records

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.data.model.ExerciseHistory
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import com.vkasport.app.ui.theme.White
import java.time.format.DateTimeFormatter

// Имена совпадают с ExerciseLibrary
private val PRIORITY_EXERCISES = listOf(
    "Жим штанги лёжа",
    "Становая тяга",
    "Приседания со штангой",
    "Подъем штанги на бицепс"
)

@Composable
fun ExerciseRecordsScreen(viewModel: TrainingSessionViewModel) {

    val records by viewModel.exerciseHistory.collectAsState()

    val mainRecords = records.values
        .filter { it.exerciseName in PRIORITY_EXERCISES }
        .sortedBy { PRIORITY_EXERCISES.indexOf(it.exerciseName) }

    val otherRecords = records.values
        .filter { it.exerciseName !in PRIORITY_EXERCISES }
        .sortedByDescending { it.recordDate }

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {

        // ===== ОСНОВНЫЕ РЕКОРДЫ =====
        // item { } — встроенный метод LazyGridScope, импорт не нужен
        item(span = { GridItemSpan(maxLineSpan) }) {
            SectionHeader(title = "ОСНОВНЫЕ РЕКОРДЫ")
        }

        if (mainRecords.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Выполните тренировки с основными упражнениями",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(mainRecords) { record ->
                RecordGridCard(record = record, isPrimary = true)
            }
        }

        // ===== ДРУГИЕ РЕКОРДЫ =====
        item(span = { GridItemSpan(maxLineSpan) }) {
            Spacer(Modifier.height(4.dp))
            SectionHeader(title = "ДРУГИЕ РЕКОРДЫ")
        }

        if (otherRecords.isEmpty()) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(White, RoundedCornerShape(12.dp))
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Здесь появятся рекорды по другим упражнениям",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DarkGray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            items(otherRecords) { record ->
                RecordGridCard(record = record, isPrimary = false)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Black, RoundedCornerShape(12.dp))
            .padding(vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = title,
            color = White,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun RecordGridCard(record: ExerciseHistory, isPrimary: Boolean) {

    val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yy")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(White, RoundedCornerShape(12.dp))
    ) {

        // Тёмная шапка с названием упражнения
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Black,
                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                )
                .padding(horizontal = 8.dp, vertical = 9.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = record.exerciseName,
                color = White,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 2,
                lineHeight = 15.sp
            )
        }

        Column(modifier = Modifier.padding(10.dp)) {

            if (isPrimary) {
                Text(text = "🏆", fontSize = 16.sp)
                Spacer(Modifier.height(4.dp))
            }

            // макс. вес
            Text(
                text = "макс. вес",
                style = MaterialTheme.typography.labelSmall,
                color = DarkGray
            )

            Spacer(Modifier.height(2.dp))

            Text(
                text = "${record.maxWeight.toInt()} кг",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "× ${record.maxWeightReps}",
                fontSize = 13.sp,
                color = DarkGray
            )

            record.recordDate?.let { date ->
                Text(
                    text = date.format(dateFmt),
                    style = MaterialTheme.typography.labelSmall,
                    color = DarkGray
                )
            }

            Spacer(Modifier.height(8.dp))

            // макс. объём
            Text(
                text = "макс. объём",
                style = MaterialTheme.typography.labelSmall,
                color = DarkGray
            )

            Text(
                text = "${record.bestVolume.toInt()} кг",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}