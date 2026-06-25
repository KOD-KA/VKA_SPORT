package com.vkasport.app.ui.records

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.data.model.CompletedWorkout
import com.vkasport.app.data.model.ExerciseHistory
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import com.vkasport.app.ui.theme.Gold
import com.vkasport.app.ui.theme.LightGray
import com.vkasport.app.ui.theme.SoftGray
import com.vkasport.app.ui.theme.White
import java.time.format.DateTimeFormatter

private fun fmtW(weight: Float): String =
    if (weight == weight.toInt().toFloat()) weight.toInt().toString() else "%.1f".format(weight)

private fun matchesArchive(workout: CompletedWorkout, q: String): Boolean {
    if (q.isBlank()) return true
    val query = q.trim().lowercase()
    return workout.muscleGroup.lowercase().contains(query) ||
            workout.dateTime.format(DateTimeFormatter.ofPattern("dd.MM.yy")).contains(query) ||
            workout.exercises.any { it.name.lowercase().contains(query) }
}

// ═══════════════════════════════════════════════════════════════════
//  КОРНЕВОЙ ЭКРАН (табы + поиск)
// ═══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutArchiveScreen(viewModel: TrainingSessionViewModel) {

    val workouts        by viewModel.completedWorkouts.collectAsState()
    val exerciseHistory by viewModel.exerciseHistory.collectAsState()

    var selectedTab  by remember { mutableStateOf(0) }
    var showSearch   by remember { mutableStateOf(false) }
    var searchQuery  by remember { mutableStateOf("") }
    var detailWorkout by remember { mutableStateOf<CompletedWorkout?>(null) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val focusRequester = remember { FocusRequester() }
    val focusManager   = LocalFocusManager.current

    // Сбрасываем поиск при смене таба
    LaunchedEffect(selectedTab) {
        searchQuery = ""
        showSearch = false
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── ТАБЫ + ИКОНКА ПОИСКА ─────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(start = 20.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TabItem("РЕКОРДЫ", selectedTab == 0) {
                selectedTab = 0
            }
            Spacer(Modifier.width(28.dp))
            TabItem("АРХИВ", selectedTab == 1) {
                selectedTab = 1
            }

            Spacer(Modifier.weight(1f))

            // Кнопка поиска / закрытия
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (showSearch) DarkGray else SoftGray,
                        RoundedCornerShape(10.dp)
                    )
                    .clickable {
                        showSearch = !showSearch
                        if (!showSearch) {
                            searchQuery = ""
                            focusManager.clearFocus()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text     = if (showSearch) "✕" else "🔍",
                    fontSize = if (showSearch) 14.sp else 16.sp,
                    color    = if (showSearch) White else Black
                )
            }
        }

        HorizontalDivider(color = SoftGray, thickness = 1.dp)

        // ── ПОИСКОВАЯ СТРОКА (анимирована) ────────────────────────────
        AnimatedVisibility(
            visible = showSearch,
            enter   = expandVertically(),
            exit    = shrinkVertically()
        ) {
            val hint = if (selectedTab == 0)
                "Упражнение, группа мышц или вес рекорда..."
            else
                "Упражнение, группа мышц или дата..."

            LaunchedEffect(showSearch) {
                if (showSearch) focusRequester.requestFocus()
            }

            OutlinedTextField(
                value         = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder   = { Text(hint, fontSize = 13.sp, color = DarkGray) },
                modifier      = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .focusRequester(focusRequester),
                singleLine    = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                trailingIcon  = {
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            "✕",
                            modifier = Modifier
                                .clickable { searchQuery = "" }
                                .padding(8.dp),
                            fontSize = 14.sp,
                            color    = DarkGray
                        )
                    }
                },
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = DarkGray,
                    unfocusedBorderColor = LightGray,
                    cursorColor          = Black,
                    focusedTextColor     = Black,
                    unfocusedTextColor   = Black
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // ── КОНТЕНТ ───────────────────────────────────────────────────
        when (selectedTab) {
            0 -> ExerciseRecordsScreen(viewModel = viewModel, searchQuery = searchQuery)
            1 -> ArchiveGrid(
                workouts        = workouts.reversed(),
                exerciseHistory = exerciseHistory,
                searchQuery     = searchQuery,
                onCardClick     = { detailWorkout = it }
            )
        }
    }

    // ── BOTTOM SHEET С ДЕТАЛЯМИ ────────────────────────────────────
    detailWorkout?.let { workout ->
        ModalBottomSheet(
            onDismissRequest = { detailWorkout = null },
            sheetState       = sheetState,
            containerColor   = White,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            WorkoutDetailSheet(workout = workout)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  ТАБ-КНОПКА
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun TabItem(title: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() }.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 16.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color      = if (selected) Black else DarkGray)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(if (selected) 48.dp else 0.dp).height(3.dp)
            .background(Black, RoundedCornerShape(2.dp)))
    }
}

// ═══════════════════════════════════════════════════════════════════
//  СЕТКА АРХИВА
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun ArchiveGrid(
    workouts:        List<CompletedWorkout>,
    exerciseHistory: Map<String, ExerciseHistory>,
    searchQuery:     String,
    onCardClick:     (CompletedWorkout) -> Unit
) {
    val filtered = workouts.filter { matchesArchive(it, searchQuery) }

    if (filtered.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                if (searchQuery.isBlank()) "Тренировок пока нет" else "Ничего не найдено",
                color = DarkGray
            )
        }
        return
    }

    LazyVerticalGrid(
        columns               = GridCells.Fixed(2),
        contentPadding        = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement   = Arrangement.spacedBy(10.dp),
        modifier              = Modifier.fillMaxSize()
    ) {
        items(filtered) { workout ->
            ArchiveCard(workout, exerciseHistory) { onCardClick(workout) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  КАРТОЧКА АРХИВА
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun ArchiveCard(
    workout:         CompletedWorkout,
    exerciseHistory: Map<String, ExerciseHistory>,
    onClick:         () -> Unit
) {
    val dateFmt = DateTimeFormatter.ofPattern("dd.MM.yy")
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    val h = workout.durationMinutes / 60
    val m = workout.durationMinutes % 60
    val dur = when { h > 0 && m > 0 -> "$h ч $m мин"; h > 0 -> "$h ч"; else -> "$m мин" }

    val recordCount = workout.exercises.count { ex ->
        val rec  = exerciseHistory[ex.name] ?: return@count false
        val best = ex.sets.maxOfOrNull { it.weight } ?: return@count false
        best >= rec.maxWeight
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftGray, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkGray, RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        ) {
            Row(Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(workout.dateTime.format(dateFmt), color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Text(workout.dateTime.format(timeFmt), color = White.copy(alpha = 0.75f), fontSize = 13.sp)
            }
        }
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp)) {
            Text(workout.muscleGroup, fontSize = 13.sp, color = Black, lineHeight = 18.sp)
            Spacer(Modifier.height(10.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween) {
                Text("%.1f кг".format(workout.athleteWeight ?: 0f), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                if (recordCount > 0) {
                    Row(
                        modifier = Modifier
                            .background(Gold.copy(alpha = 0.30f), RoundedCornerShape(50.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("🏆", fontSize = 13.sp)
                        Text("× $recordCount", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF6B5010))
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            Text(dur, fontSize = 13.sp, color = DarkGray)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  ДЕТАЛЬНЫЙ BOTTOM SHEET
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun WorkoutDetailSheet(workout: CompletedWorkout) {
    val dateFmt = DateTimeFormatter.ofPattern("dd MMMM yyyy", java.util.Locale("ru"))
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    val totalSets   = workout.exercises.sumOf { it.sets.size }
    val totalVolume = workout.exercises.sumOf { ex -> ex.sets.sumOf { (it.weight * it.reps).toDouble() } }.toInt()
    val h = workout.durationMinutes / 60
    val m = workout.durationMinutes % 60
    val dur = when { h > 0 && m > 0 -> "$h ч $m мин"; h > 0 -> "$h ч"; else -> "$m мин" }

    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 32.dp)) {

        item {
            Column(Modifier.fillMaxWidth().background(DarkGray).padding(horizontal = 20.dp, vertical = 16.dp)) {
                Text(workout.dateTime.format(dateFmt), color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text("${workout.dateTime.format(timeFmt)}  •  ${workout.muscleGroup}",
                    color = White.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }

        item {
            Row(Modifier.fillMaxWidth().background(SoftGray).padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly) {
                SummaryBlock("%.1f кг".format(workout.athleteWeight ?: 0f), "вес")
                SummaryDivider()
                SummaryBlock("${workout.exercises.size}", "упражн.")
                SummaryDivider()
                SummaryBlock("$totalSets", "подходов")
                SummaryDivider()
                SummaryBlock("$totalVolume кг", "объём")
                SummaryDivider()
                SummaryBlock(dur, "время")
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        items(workout.exercises) { exercise ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp)) {
                Box(Modifier.fillMaxWidth().background(Black, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(exercise.name, color = White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Column(Modifier.fillMaxWidth().background(White, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("#",      Modifier.width(28.dp), fontSize = 11.sp, color = DarkGray)
                        Text("Вес",   Modifier.weight(1f),   fontSize = 11.sp, color = DarkGray)
                        Text("Повт.", Modifier.weight(1f),   fontSize = 11.sp, color = DarkGray)
                        Text("Объём", Modifier.weight(1f),   fontSize = 11.sp, color = DarkGray)
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = LightGray)
                    Spacer(Modifier.height(4.dp))

                    if (exercise.sets.isEmpty()) {
                        Text("Нет подходов", fontSize = 13.sp, color = DarkGray)
                    } else {
                        exercise.sets.forEachIndexed { idx, set ->
                            Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically) {
                                Text("${idx + 1}", Modifier.width(28.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("${fmtW(set.weight)} кг", Modifier.weight(1f), fontSize = 14.sp)
                                Text("× ${set.reps}", Modifier.weight(1f), fontSize = 14.sp)
                                Text("${(set.weight * set.reps).toInt()} кг", Modifier.weight(1f), fontSize = 13.sp, color = DarkGray)
                            }
                            if (idx < exercise.sets.lastIndex)
                                HorizontalDivider(color = SoftGray, thickness = 0.5.dp)
                        }
                        val exVol = exercise.sets.sumOf { (it.weight * it.reps).toDouble() }.toInt()
                        val exMax = exercise.sets.maxOf { it.weight }
                        Spacer(Modifier.height(6.dp))
                        HorizontalDivider(color = LightGray)
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Макс: ${fmtW(exMax)} кг", fontSize = 12.sp, color = DarkGray)
                            Text("Всего: $exVol кг",         fontSize = 12.sp, color = DarkGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable private fun SummaryBlock(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Black)
        Text(label, fontSize = 10.sp, color = DarkGray)
    }
}
@Composable private fun SummaryDivider() {
    Box(Modifier.width(1.dp).height(28.dp).background(LightGray))
}