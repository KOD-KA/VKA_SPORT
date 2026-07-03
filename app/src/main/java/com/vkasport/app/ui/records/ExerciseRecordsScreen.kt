package com.vkasport.app.ui.records

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.data.model.ExerciseHistory
import com.vkasport.app.data.model.ExerciseLibrary
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import com.vkasport.app.ui.theme.Gold
import com.vkasport.app.ui.theme.LightGray
import com.vkasport.app.ui.theme.SoftGray
import com.vkasport.app.ui.theme.White
import java.time.format.DateTimeFormatter

private val PRIORITY_EXERCISES = listOf(
    "Жим штанги лёжа",
    "Становая тяга",
    "Приседания со штангой",
    "Подъем штанги на бицепс"
)

private fun fmtW(weight: Float): String =
    if (weight == weight.toInt().toFloat()) weight.toInt().toString() else "%.1f".format(weight)

private fun muscleGroupOf(name: String): String =
    ExerciseLibrary.exercises.find { it.name == name }?.muscleGroup?.title?.lowercase() ?: ""

private fun matchesRecord(record: ExerciseHistory, q: String): Boolean {
    if (q.isBlank()) return true
    val query = q.trim().lowercase()
    return record.exerciseName.lowercase().contains(query) ||
            muscleGroupOf(record.exerciseName).contains(query) ||
            record.maxWeight.toInt().toString().contains(query)
}

// ═══════════════════════════════════════════════════════════════════
//  КОРНЕВОЙ ЭКРАН
// ═══════════════════════════════════════════════════════════════════

@Composable
fun ExerciseRecordsScreen(
    viewModel: TrainingSessionViewModel,
    searchQuery: String = ""
) {
    val records by viewModel.exerciseHistory.collectAsState()

    val mainRecords = records.values
        .filter { it.exerciseName in PRIORITY_EXERCISES && matchesRecord(it, searchQuery) }
        .sortedBy { PRIORITY_EXERCISES.indexOf(it.exerciseName) }

    val otherRecords = records.values
        .filter { it.exerciseName !in PRIORITY_EXERCISES && matchesRecord(it, searchQuery) }
        .sortedByDescending { it.recordDate }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionLabel("ОСНОВНЫЕ РЕКОРДЫ") }

        if (mainRecords.isEmpty()) {
            item { EmptyHint(if (searchQuery.isBlank()) "Выполните тренировки с основными упражнениями" else "Ничего не найдено") }
        } else {
            items(mainRecords) { RecordCard(it, true) }
        }

        item { Spacer(Modifier.height(6.dp)) }
        item { SectionLabel("ДРУГИЕ РЕКОРДЫ") }

        if (otherRecords.isEmpty()) {
            item { EmptyHint(if (searchQuery.isBlank()) "Здесь появятся рекорды по другим упражнениям" else "Ничего не найдено") }
        } else {
            items(otherRecords) { RecordCard(it, false) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  КАРТОЧКА РЕКОРДА
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun RecordCard(record: ExerciseHistory, isPrimary: Boolean) {
    val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yy")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftGray, RoundedCornerShape(16.dp))
    ) {
        // Шапка
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkGray, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text(record.exerciseName, color = White, fontWeight = FontWeight.Bold,
                    fontSize = 15.sp, modifier = Modifier.weight(1f))
                if (isPrimary) {
                    Spacer(Modifier.width(8.dp))
                    Text("🏆", fontSize = 18.sp)
                }
            }
        }

        // Тело
        Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {

            // ─ МАКС. ВЕС ────────────────────────────────────────────
            MetricRow(
                icon        = "🏆",
                label       = "макс. вес",
                bigValue    = "${record.maxWeight.toInt()}",
                subValue    = "× ${record.maxWeightReps}",
                date        = record.recordDate?.format(dateFmt),
                subDate     = record.athleteWeight?.let { "%.1f кг".format(it) }
            )

            // Толстый разделитель
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.5.dp)
                    .background(DarkGray.copy(alpha = 0.22f))
            )
            Spacer(Modifier.height(10.dp))

            // ─ МАКС. ОБЪЁМ ──────────────────────────────────────────
            val volSubValue = if (record.bestVolumeWeight != null && record.bestVolumeReps != null)
                "${fmtW(record.bestVolumeWeight)} × ${record.bestVolumeReps}"
            else "кг"

            MetricRow(
                icon        = "📦",
                label       = "макс. объём",
                bigValue    = "${record.bestVolume.toInt()}",
                subValue    = volSubValue,
                date        = record.recordDate?.format(dateFmt),
                subDate     = record.athleteWeight?.let { "%.1f кг".format(it) }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  СТРОКА МЕТРИКИ
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun MetricRow(
    icon: String, label: String,
    bigValue: String, subValue: String,
    date: String?, subDate: String?
) {
    Column {
        Text(label, fontSize = 11.sp, color = DarkGray)
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            // Иконка в золотом круге
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(Gold.copy(alpha = 0.18f), CircleShape),
                contentAlignment = Alignment.Center
            ) { Text(icon, fontSize = 20.sp) }

            Spacer(Modifier.width(12.dp))

            // Большое число + подпись
            Row(Modifier.weight(1f), verticalAlignment = Alignment.Bottom) {
                Text(bigValue, color = Black, fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 38.sp)
                Spacer(Modifier.width(6.dp))
                Text(subValue, fontSize = 14.sp, color = DarkGray, modifier = Modifier.padding(bottom = 4.dp))
            }

            // Дата + вес атлета
            if (date != null || subDate != null) {
                Column(horizontalAlignment = Alignment.End) {
                    date?.let { Text(it, fontSize = 12.sp, color = DarkGray) }
                    subDate?.let { Text(it, fontSize = 11.sp, color = DarkGray.copy(alpha = 0.7f)) }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  ВСПОМОГАТЕЛЬНЫЕ
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun SectionLabel(title: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        HorizontalDivider(Modifier.weight(1f), color = LightGray)
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = DarkGray)
        HorizontalDivider(Modifier.weight(1f), color = LightGray)
    }
}

@Composable
private fun EmptyHint(text: String) {
    Box(Modifier.fillMaxWidth().background(SoftGray, RoundedCornerShape(12.dp)).padding(16.dp),
        contentAlignment = Alignment.Center) {
        Text(text, fontSize = 13.sp, color = DarkGray, textAlign = TextAlign.Center)
    }
}