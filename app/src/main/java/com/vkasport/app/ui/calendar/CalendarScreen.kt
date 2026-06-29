package com.vkasport.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

private fun fmtW(w: Float) = if (w == w.toInt().toFloat()) w.toInt().toString() else "%.1f".format(w)

// ═══════════════════════════════════════════════════════════════════
//  ROOT
// ═══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: TrainingSessionViewModel) {

    val workouts        by viewModel.completedWorkouts.collectAsState()
    val exerciseHistory by viewModel.exerciseHistory.collectAsState()

    var yearMonth    by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    var selectedTab  by remember { mutableStateOf(0) }
    var detailWorkout by remember { mutableStateOf<CompletedWorkout?>(null) }
    val sheetState   = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val workoutsByDate = remember(workouts) { workouts.groupBy { it.dateTime.toLocalDate() } }
    val recordDays     = remember(workouts, exerciseHistory) {
        workouts.filter { w ->
            w.exercises.any { ex ->
                val r = exerciseHistory[ex.name] ?: return@any false
                ex.sets.any { it.weight >= r.maxWeight }
            }
        }.map { it.dateTime.toLocalDate() }.toSet()
    }
    val selectedWorkout = selectedDate?.let { workoutsByDate[it]?.lastOrNull() }

    // сбрасываем выбор при смене месяца
    LaunchedEffect(yearMonth) { selectedDate = null }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(White)
    ) {
        // ── ТАБЫ ──────────────────────────────────────────────────────
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(White)
                .padding(horizontal = 20.dp)
        ) {
            CalTab("КАЛЕНДАРЬ",       selectedTab == 0) { selectedTab = 0 }
            Spacer(Modifier.width(28.dp))
            CalTab("ЗАПЛАНИРОВАННОЕ", selectedTab == 1) { selectedTab = 1 }
        }
        HorizontalDivider(color = SoftGray, thickness = 1.dp)

        when (selectedTab) {
            0 -> CalendarTab(
                yearMonth       = yearMonth,
                workoutsByDate  = workoutsByDate,
                recordDays      = recordDays,
                selectedDate    = selectedDate,
                selectedWorkout = selectedWorkout,
                exerciseHistory = exerciseHistory,
                onPrev          = { yearMonth = yearMonth.minusMonths(1) },
                onNext          = { yearMonth = yearMonth.plusMonths(1) },
                onDayClick      = { d -> selectedDate = if (selectedDate == d) null else d },
                onWorkoutClick  = { detailWorkout = it }
            )
            else -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Запланированные тренировки — скоро", color = DarkGray, fontSize = 14.sp)
            }
        }
    }

    // ── BOTTOM SHEET: ПОЛНАЯ ТРЕНИРОВКА ───────────────────────────
    detailWorkout?.let { w ->
        ModalBottomSheet(
            onDismissRequest = { detailWorkout = null },
            sheetState       = sheetState,
            containerColor   = White,
            shape            = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            WorkoutDetailSheet(workout = w)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  ТАБ-КНОПКА
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun CalTab(title: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() }.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 15.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color      = if (selected) Black else DarkGray)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(if (selected) 48.dp else 0.dp).height(3.dp)
            .background(Black, RoundedCornerShape(2.dp)))
    }
}

// ═══════════════════════════════════════════════════════════════════
//  ВКЛАДКА КАЛЕНДАРЯ
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun CalendarTab(
    yearMonth:       YearMonth,
    workoutsByDate:  Map<LocalDate, List<CompletedWorkout>>,
    recordDays:      Set<LocalDate>,
    selectedDate:    LocalDate?,
    selectedWorkout: CompletedWorkout?,
    exerciseHistory: Map<String, ExerciseHistory>,
    onPrev:         () -> Unit,
    onNext:         () -> Unit,
    onDayClick:     (LocalDate) -> Unit,
    onWorkoutClick: (CompletedWorkout) -> Unit
) {
    // Статистика месяца
    val monthWorkouts = workoutsByDate.entries
        .filter { (d, _) -> d.year == yearMonth.year && d.monthValue == yearMonth.monthValue }
        .flatMap { it.value }
    val monthCount   = monthWorkouts.size
    val monthRecords = monthWorkouts.count { w ->
        w.exercises.any { ex ->
            val r = exerciseHistory[ex.name] ?: return@any false
            ex.sets.any { it.weight >= r.maxWeight }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 14.dp)
    ) {
        Spacer(Modifier.height(14.dp))

        // Мини-статистика месяца
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftGray, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            MonthStat("$monthCount",   "тренировок")
            Box(Modifier.width(1.dp).height(24.dp).background(LightGray))
            MonthStat("$monthRecords", "рекордов 🏆")
        }

        Spacer(Modifier.height(12.dp))

        // Тёмная карточка-календарь
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(DarkGray, RoundedCornerShape(20.dp))
        ) {
            // Навигация месяца
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(White.copy(alpha = 0.12f), CircleShape)
                        .clickable { onPrev() },
                    contentAlignment = Alignment.Center
                ) { Text("<", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }

                val name = yearMonth.month
                    .getDisplayName(TextStyle.FULL_STANDALONE, Locale("ru"))
                    .replaceFirstChar { it.uppercase() }
                Text("$name ${yearMonth.year}", color = White,
                    fontWeight = FontWeight.Bold, fontSize = 17.sp)

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(White.copy(alpha = 0.12f), CircleShape)
                        .clickable { onNext() },
                    contentAlignment = Alignment.Center
                ) { Text(">", color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
            }

            // Заголовки дней недели
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                listOf("пн","вт","ср","чт","пт","сб","вс").forEach { d ->
                    Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text(d, color = White.copy(alpha = 0.55f), fontSize = 12.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }

            Spacer(Modifier.height(6.dp))

            // Сетка — BoxWithConstraints для равных ячеек
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
            ) {
                val cellSize: Dp = maxWidth / 7
                CalendarGrid(
                    yearMonth      = yearMonth,
                    workoutsByDate = workoutsByDate,
                    recordDays     = recordDays,
                    selectedDate   = selectedDate,
                    cellSize       = cellSize,
                    onDayClick     = onDayClick
                )
            }

            Spacer(Modifier.height(6.dp))

            // Легенда
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment     = Alignment.CenterVertically
            ) {
                Dot(Gold);  Spacer(Modifier.width(5.dp))
                Text("рекорд", color = White.copy(alpha = 0.7f), fontSize = 11.sp)
                Spacer(Modifier.width(16.dp))
                Dot(White.copy(alpha = 0.85f));  Spacer(Modifier.width(5.dp))
                Text("тренировка", color = White.copy(alpha = 0.7f), fontSize = 11.sp)
            }

            // Инфо выбранного дня (кликабельное)
            if (selectedWorkout != null && selectedDate != null) {
                HorizontalDivider(color = White.copy(alpha = 0.12f))

                val hasRecord = selectedWorkout.exercises.any { ex ->
                    val r = exerciseHistory[ex.name] ?: return@any false
                    ex.sets.any { it.weight >= r.maxWeight }
                }
                val h   = selectedWorkout.durationMinutes / 60
                val m   = selectedWorkout.durationMinutes % 60
                val dur = if (h > 0) "$h ч $m мин" else "$m мин"

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onWorkoutClick(selectedWorkout) }
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                selectedDate.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")),
                                color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp
                            )
                            if (hasRecord) {
                                Spacer(Modifier.width(6.dp))
                                Text("🏆", fontSize = 14.sp)
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(selectedWorkout.muscleGroup,
                            color = White.copy(alpha = 0.7f), fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        selectedWorkout.athleteWeight?.let {
                            Text("%.1f кг".format(it), color = White,
                                fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                        Text(dur, color = White.copy(alpha = 0.7f), fontSize = 12.sp)
                        Spacer(Modifier.height(2.dp))
                        // Подсказка — нажать для деталей
                        Text("подробнее →", color = Gold, fontSize = 10.sp,
                            fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  СЕТКА С РАВНЫМИ ЯЧЕЙКАМИ
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun CalendarGrid(
    yearMonth:      YearMonth,
    workoutsByDate: Map<LocalDate, List<CompletedWorkout>>,
    recordDays:     Set<LocalDate>,
    selectedDate:   LocalDate?,
    cellSize:       Dp,
    onDayClick:     (LocalDate) -> Unit
) {
    val offset      = yearMonth.atDay(1).dayOfWeek.value - 1   // 0=Mon
    val daysInMonth = yearMonth.lengthOfMonth()
    val rows        = (offset + daysInMonth + 6) / 7

    Column {
        for (row in 0 until rows) {
            Row(modifier = Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val dayNum = row * 7 + col - offset + 1
                    Box(modifier = Modifier.size(cellSize)) {
                        if (dayNum in 1..daysInMonth) {
                            val date   = yearMonth.atDay(dayNum)
                            DayCell(
                                dayNum     = dayNum,
                                cellSize   = cellSize,
                                hasWorkout = workoutsByDate.containsKey(date),
                                isRecord   = recordDays.contains(date),
                                isSelected = date == selectedDate,
                                isToday    = date == LocalDate.now(),
                                onClick    = { onDayClick(date) }
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(2.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════
//  ЯЧЕЙКА ДНЯ
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun DayCell(
    dayNum:     Int,
    cellSize:   Dp,
    hasWorkout: Boolean,
    isRecord:   Boolean,
    isSelected: Boolean,
    isToday:    Boolean,
    onClick:    () -> Unit
) {
    val bgColor = when {
        isSelected && isToday -> Gold.copy(alpha = 0.30f)
        isSelected            -> White.copy(alpha = 0.18f)
        else                  -> Color.Transparent
    }
    Box(
        modifier = Modifier
            .size(cellSize)
            .padding(3.dp)
            .background(bgColor, RoundedCornerShape(10.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text       = "$dayNum",
                color      = when {
                    isToday    -> Gold
                    isSelected -> White
                    else       -> White.copy(alpha = 0.88f)
                },
                fontSize   = 18.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                textAlign  = TextAlign.Center
            )
            Spacer(Modifier.height(2.dp))
            Box(
                Modifier
                    .size(5.dp)
                    .background(
                        when {
                            !hasWorkout -> Color.Transparent
                            isRecord    -> Gold
                            else        -> White.copy(alpha = 0.8f)
                        }, CircleShape
                    )
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  ПОЛНЫЙ BOTTOM SHEET С ДЕТАЛЯМИ ТРЕНИРОВКИ
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun WorkoutDetailSheet(workout: CompletedWorkout) {
    val dateFmt     = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ru"))
    val timeFmt     = DateTimeFormatter.ofPattern("HH:mm")
    val totalSets   = workout.exercises.sumOf { it.sets.size }
    val totalVolume = workout.exercises.sumOf { ex -> ex.sets.sumOf { (it.weight * it.reps).toDouble() } }.toInt()
    val h = workout.durationMinutes / 60
    val m = workout.durationMinutes % 60
    val dur = if (h > 0 && m > 0) "$h ч $m мин" else if (h > 0) "$h ч" else "$m мин"

    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 32.dp)) {

        item {
            Column(
                Modifier.fillMaxWidth().background(DarkGray)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                Text(workout.dateTime.format(dateFmt), color = White,
                    fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(3.dp))
                Text("${workout.dateTime.format(timeFmt)}  •  ${workout.muscleGroup}",
                    color = White.copy(alpha = 0.7f), fontSize = 13.sp)
            }
        }

        item {
            Row(
                Modifier.fillMaxWidth().background(SoftGray)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DStat("%.1f кг".format(workout.athleteWeight ?: 0f), "вес")
                DVert(); DStat("${workout.exercises.size}", "упражн.")
                DVert(); DStat("$totalSets", "подходов")
                DVert(); DStat("$totalVolume кг", "объём")
                DVert(); DStat(dur, "время")
            }
        }

        item { Spacer(Modifier.height(14.dp)) }

        items(workout.exercises) { exercise ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 10.dp)) {
                Box(
                    Modifier.fillMaxWidth()
                        .background(Black, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Text(exercise.name, color = White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Column(
                    Modifier.fillMaxWidth()
                        .background(White, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("#",      Modifier.width(28.dp), fontSize = 11.sp, color = DarkGray)
                        Text("Вес",   Modifier.weight(1f),   fontSize = 11.sp, color = DarkGray)
                        Text("Повт.", Modifier.weight(1f),   fontSize = 11.sp, color = DarkGray)
                        Text("Объём", Modifier.weight(1f),   fontSize = 11.sp, color = DarkGray)
                    }
                    Spacer(Modifier.height(4.dp))
                    HorizontalDivider(color = LightGray)
                    Spacer(Modifier.height(4.dp))

                    exercise.sets.forEachIndexed { idx, set ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text("${idx+1}", Modifier.width(28.dp), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${fmtW(set.weight)} кг", Modifier.weight(1f), fontSize = 14.sp)
                            Text("× ${set.reps}", Modifier.weight(1f), fontSize = 14.sp)
                            Text("${(set.weight * set.reps).toInt()} кг",
                                Modifier.weight(1f), fontSize = 13.sp, color = DarkGray)
                        }
                        if (idx < exercise.sets.lastIndex)
                            HorizontalDivider(color = SoftGray, thickness = 0.5.dp)
                    }

                    if (exercise.sets.isNotEmpty()) {
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

// ═══════════════════════════════════════════════════════════════════
//  ВСПОМОГАТЕЛЬНЫЕ
// ═══════════════════════════════════════════════════════════════════

@Composable private fun MonthStat(v: String, l: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Black)
        Text(l, fontSize = 11.sp, color = DarkGray)
    }
}
@Composable private fun Dot(c: Color) = Box(Modifier.size(7.dp).background(c, CircleShape))
@Composable private fun DStat(v: String, l: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
        Text(l, fontSize = 10.sp, color = DarkGray)
    }
}
@Composable private fun DVert() = Box(Modifier.width(1.dp).height(28.dp).background(LightGray))