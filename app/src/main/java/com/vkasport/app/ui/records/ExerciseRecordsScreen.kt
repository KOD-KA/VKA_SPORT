package com.vkasport.app.ui.records

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import com.vkasport.app.data.model.MeasureType
import com.vkasport.app.ui.common.SetFormat
import com.vkasport.app.ui.common.SimpleLineChart
import java.time.LocalDate
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseRecordsScreen(
    viewModel: TrainingSessionViewModel,
    searchQuery: String = ""
) {
    val records by viewModel.exerciseHistory.collectAsState()
    val completedWorkouts by viewModel.completedWorkouts.collectAsState()

    // Рекорд, по которому открыт bottom sheet с графиками прогресса
    var detailRecord by remember { mutableStateOf<ExerciseHistory?>(null) }

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
            items(mainRecords) { rec -> RecordCard(rec, true) { detailRecord = rec } }
        }

        item { Spacer(Modifier.height(6.dp)) }
        item { SectionLabel("ДРУГИЕ РЕКОРДЫ") }

        if (otherRecords.isEmpty()) {
            item { EmptyHint(if (searchQuery.isBlank()) "Здесь появятся рекорды по другим упражнениям" else "Ничего не найдено") }
        } else {
            items(otherRecords) { rec -> RecordCard(rec, false) { detailRecord = rec } }
        }
    }

    // ===== ПРОГРЕСС ПО УПРАЖНЕНИЮ (bottom sheet, тап по карточке) =====
    detailRecord?.let { rec ->
        val type = rec.measureType
        // Упражнение со своим весом: тип вес×повторы, но веса нет (max = 0).
        // Тогда прогресс и рекорд показываем по ПОВТОРАМ, а не по кг.
        val bodyweight = type == MeasureType.WEIGHT_REPS && rec.maxWeight <= 0f
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val chartDateFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yy") }

        // Точки по тренировкам, где встречалось упражнение (по ИМЕНИ —
        // общая история упражнения). completedWorkouts отсортирован DESC,
        // для графика сортируем по дате по возрастанию.
        val mainPoints = remember(rec.exerciseName, completedWorkouts) {
            completedWorkouts.mapNotNull { w ->
                val sets = w.exercises.filter { it.name == rec.exerciseName }.flatMap { it.sets }
                if (sets.isEmpty()) null else {
                    val v: Float? = when {
                        type == MeasureType.WEIGHT_REPS && bodyweight -> sets.maxOf { it.reps }.toFloat()
                        type == MeasureType.WEIGHT_REPS -> sets.maxOf { it.weight }
                        type == MeasureType.REPS -> sets.maxOf { it.reps }.toFloat()
                        type == MeasureType.TIME -> sets.maxOf { it.seconds ?: 0 }.toFloat()
                        type == MeasureType.DISTANCE -> sets.maxOf { it.distanceKm ?: 0f }
                        else -> null
                    }
                    v?.let { value -> w.dateTime.toLocalDate() to value }
                }
            }.sortedBy { it.first }
        }

        ModalBottomSheet(
            onDismissRequest = { detailRecord = null },
            sheetState = sheetState,
            containerColor = White
        ) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Text(
                    "ПРОГРЕСС: ${rec.exerciseName}",
                    color = Black, fontSize = 16.sp, fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                val mainTitle = when (type) {
                    MeasureType.WEIGHT_REPS -> if (bodyweight) "МАКС. ПОВТОРЫ ПО ТРЕНИРОВКАМ" else "МАКС. ВЕС ПО ТРЕНИРОВКАМ, КГ"
                    MeasureType.REPS -> "МАКС. ПОВТОРЫ ПО ТРЕНИРОВКАМ"
                    MeasureType.TIME -> "ЛУЧШЕЕ ВРЕМЯ ПО ТРЕНИРОВКАМ, СЕК"
                    MeasureType.DISTANCE -> "ДИСТАНЦИЯ ПО ТРЕНИРОВКАМ, КМ"
                    MeasureType.CARDIO -> "ПРОГРЕСС"
                }
                Column(
                    Modifier.fillMaxWidth().background(SoftGray, RoundedCornerShape(16.dp)).padding(14.dp)
                ) {
                    Text(mainTitle, color = Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    SimpleLineChart(
                        values = mainPoints.map { it.second },
                        startLabel = mainPoints.firstOrNull()?.first?.format(chartDateFmt),
                        endLabel = mainPoints.lastOrNull()?.first?.format(chartDateFmt)
                    )
                }

                // Для классики — второй график: объём за тренировку
                if (type == MeasureType.WEIGHT_REPS && !bodyweight) {
                    Spacer(Modifier.height(12.dp))
                    val volumePoints = remember(rec.exerciseName, completedWorkouts) {
                        completedWorkouts.mapNotNull { w ->
                            val sets = w.exercises.filter { it.name == rec.exerciseName }.flatMap { it.sets }
                            if (sets.isEmpty()) null
                            else w.dateTime.toLocalDate() to
                                    sets.sumOf { (it.weight * it.reps).toDouble() }.toFloat()
                        }.sortedBy { it.first }
                    }
                    Column(
                        Modifier.fillMaxWidth().background(SoftGray, RoundedCornerShape(16.dp)).padding(14.dp)
                    ) {
                        Text("ОБЪЁМ ЗА ТРЕНИРОВКУ, КГ", color = Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        SimpleLineChart(
                            values = volumePoints.map { it.second },
                            startLabel = volumePoints.firstOrNull()?.first?.format(chartDateFmt),
                            endLabel = volumePoints.lastOrNull()?.first?.format(chartDateFmt)
                        )
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  КАРТОЧКА РЕКОРДА
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun RecordCard(record: ExerciseHistory, isPrimary: Boolean, onClick: () -> Unit = {}) {
    val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yy")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftGray, RoundedCornerShape(16.dp))
            .clickable { onClick() }
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

            // МОДЕЛЬ V2: содержимое карточки зависит от типа упражнения
            when (record.measureType) {
                MeasureType.REPS -> MetricRow(
                    icon     = "🏆",
                    label    = "макс. повторы",
                    bigValue = "${record.maxReps}",
                    subValue = "повт.",
                    date     = record.recordDate?.format(dateFmt),
                    subDate  = record.athleteWeight?.let { "%.1f кг".format(it) }
                )
                MeasureType.TIME -> MetricRow(
                    icon     = "🏆",
                    label    = "лучшее время",
                    bigValue = SetFormat.time(record.bestSeconds ?: 0),
                    subValue = "мин:сек",
                    date     = record.recordDate?.format(dateFmt),
                    subDate  = record.athleteWeight?.let { "%.1f кг".format(it) }
                )
                MeasureType.DISTANCE -> MetricRow(
                    icon     = "🏆",
                    label    = "макс. дистанция",
                    bigValue = SetFormat.num(record.bestDistanceKm ?: 0f),
                    subValue = "км",
                    date     = record.recordDate?.format(dateFmt),
                    subDate  = record.athleteWeight?.let { "%.1f кг".format(it) }
                )
                else -> {
                    if (record.maxWeight > 0f) {
                        // ─ МАКС. ВЕС ────────────────────────────────
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

                        // ─ МАКС. ОБЪЁМ ──────────────────────────────
                        val volSubValue = if (record.bestVolumeWeight != null && record.bestVolumeReps != null)
                            "${fmtW(record.bestVolumeWeight)} × ${record.bestVolumeReps}"
                        else "кг"

                        MetricRow(
                            icon        = "📊",
                            label       = "макс. объём",
                            bigValue    = "${record.bestVolume.toInt()}",
                            subValue    = volSubValue,
                            date        = record.recordDate?.format(dateFmt),
                            subDate     = record.athleteWeight?.let { "%.1f кг".format(it) }
                        )
                    } else {
                        // Упражнение со СВОИМ ВЕСОМ (без отягощения): вес и
                        // объём в кг = 0, поэтому рекорд — по числу повторов
                        MetricRow(
                            icon        = "🏆",
                            label       = "макс. повторов (свой вес)",
                            bigValue    = "${record.maxReps}",
                            subValue    = "повт.",
                            date        = record.recordDate?.format(dateFmt),
                            subDate     = record.athleteWeight?.let { "%.1f кг".format(it) }
                        )
                    }
                }
            }
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