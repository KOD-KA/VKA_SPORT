package com.vkasport.app.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.data.model.*
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import com.vkasport.app.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

private val Blue = Color(0xFF4285F4)
private fun fmtW(w: Float) = if (w == w.toInt().toFloat()) w.toInt().toString() else "%.1f".format(w)
private val DAY_LABELS = listOf("пн","вт","ср","чт","пт","сб","вс")

// ═══════════════════════════════════════════════════════════════════
//  ROOT
// ═══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: TrainingSessionViewModel) {
    val workouts        by viewModel.completedWorkouts.collectAsState()
    val exerciseHistory by viewModel.exerciseHistory.collectAsState()
    val plannedWorkouts by viewModel.plannedWorkouts.collectAsState()

    var selectedTab by remember { mutableStateOf(0) }
    var detailWorkout by remember { mutableStateOf<CompletedWorkout?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Экран всегда с белой шапкой — статус-бар с тёмными иконками
    com.vkasport.app.ui.theme.SystemBarsAppearance(statusBarColor = White, darkIcons = true)

    LaunchedEffect(Unit) { viewModel.loadPlannedWorkouts() }

    val workoutsByDate = remember(workouts) { workouts.groupBy { it.dateTime.toLocalDate() } }
    val recordDays = remember(workouts, exerciseHistory) {
        workouts.filter { w -> w.exercises.any { ex ->
            val r = exerciseHistory[ex.name] ?: return@any false
            ex.sets.any { it.weight >= r.maxWeight }
        }}.map { it.dateTime.toLocalDate() }.toSet()
    }
    val plannedByDate = remember(plannedWorkouts) { plannedWorkouts.groupBy { it.date } }

    Column(Modifier.fillMaxSize().background(White)) {
        // ── ТАБЫ ────────────────────────────────────────────────────
        Row(Modifier.fillMaxWidth().background(White).padding(horizontal = 20.dp)) {
            CalTab("КАЛЕНДАРЬ",       selectedTab == 0) { selectedTab = 0 }
            Spacer(Modifier.width(20.dp))
            CalTab("ЗАПЛАНИРОВАННОЕ", selectedTab == 1) { selectedTab = 1 }
        }
        HorizontalDivider(color = SoftGray)

        when (selectedTab) {
            0 -> MonthTab(workoutsByDate, recordDays, plannedByDate, exerciseHistory,
                onWorkoutClick = { detailWorkout = it })
            1 -> WeekTab(plannedWorkouts, plannedByDate, viewModel)
        }
    }

    detailWorkout?.let { w ->
        ModalBottomSheet(onDismissRequest = { detailWorkout = null }, sheetState = sheetState,
            containerColor = White, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            WorkoutDetailSheet(w, viewModel = viewModel, onDeleted = { detailWorkout = null })
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  ВКЛАДКА 1: МЕСЯЦ
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun MonthTab(
    workoutsByDate:  Map<LocalDate, List<CompletedWorkout>>,
    recordDays:      Set<LocalDate>,
    plannedByDate:   Map<LocalDate, List<PlannedWorkout>>,
    exerciseHistory: Map<String, ExerciseHistory>,
    onWorkoutClick:  (CompletedWorkout) -> Unit
) {
    var yearMonth    by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(LocalDate.now()) }
    LaunchedEffect(yearMonth) { selectedDate = null }

    val monthWorkouts = workoutsByDate.entries
        .filter { (d, _) -> d.year == yearMonth.year && d.monthValue == yearMonth.monthValue }
        .flatMap { it.value }

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(14.dp))

        // Мини-статистика месяца
        Row(Modifier.fillMaxWidth().background(SoftGray, RoundedCornerShape(12.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceEvenly) {
            MiniStat("${monthWorkouts.size}", "тренировок")
            Box(Modifier.width(1.dp).height(24.dp).background(LightGray))
            MiniStat("${monthWorkouts.count { w -> w.exercises.any { ex ->
                val r = exerciseHistory[ex.name] ?: return@any false; ex.sets.any { it.weight >= r.maxWeight }
            }}}", "рекордов 🏆")
            Box(Modifier.width(1.dp).height(24.dp).background(LightGray))
            MiniStat("${plannedByDate.entries.count { (d, _) ->
                d.year == yearMonth.year && d.monthValue == yearMonth.monthValue
            }}", "запланировано")
        }

        Spacer(Modifier.height(12.dp))

        Column(Modifier.fillMaxWidth().background(DarkGray, RoundedCornerShape(20.dp))) {
            // Навигация
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                NavCircle("<") { yearMonth = yearMonth.minusMonths(1) }
                val name = yearMonth.month.getDisplayName(JTextStyle.FULL_STANDALONE, Locale("ru")).replaceFirstChar { it.uppercase() }
                Text("$name ${yearMonth.year}", color = White, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                NavCircle(">") { yearMonth = yearMonth.plusMonths(1) }
            }
            // Заголовки
            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                DAY_LABELS.forEach { Box(Modifier.weight(1f), Alignment.Center) {
                    Text(it, color = White.copy(alpha = 0.55f), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                }}
            }
            Spacer(Modifier.height(6.dp))
            // Сетка
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                val cell: Dp = maxWidth / 7
                MonthGrid(yearMonth, workoutsByDate, recordDays, plannedByDate, selectedDate, cell) { d ->
                    selectedDate = if (selectedDate == d) null else d
                }
            }
            Spacer(Modifier.height(4.dp))
            // Легенда
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
                Dot(Gold); Spacer(Modifier.width(5.dp)); Text("рекорд", color = White.copy(.7f), fontSize = 11.sp)
                Spacer(Modifier.width(14.dp))
                Dot(White.copy(.85f)); Spacer(Modifier.width(5.dp)); Text("тренировка", color = White.copy(.7f), fontSize = 11.sp)
                Spacer(Modifier.width(14.dp))
                Dot(Blue); Spacer(Modifier.width(5.dp)); Text("запланировано", color = White.copy(.7f), fontSize = 11.sp)
            }
            // Инфо выбранного дня
            val selWorkout  = selectedDate?.let { workoutsByDate[it]?.firstOrNull() }
            val selPlanned  = selectedDate?.let { plannedByDate[it]?.firstOrNull() }
            if (selWorkout != null && selectedDate != null) {
                HorizontalDivider(color = White.copy(.12f))
                val hasRec = selWorkout.exercises.any { ex ->
                    val r = exerciseHistory[ex.name] ?: return@any false; ex.sets.any { it.weight >= r.maxWeight }
                }
                val h = selWorkout.durationMinutes/60; val m = selWorkout.durationMinutes%60
                Row(Modifier.fillMaxWidth().clickable { onWorkoutClick(selWorkout) }.padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(selectedDate!!.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            if (hasRec) { Spacer(Modifier.width(6.dp)); Text("🏆", fontSize = 14.sp) }
                        }
                        Text(selWorkout.muscleGroup, color = White.copy(.7f), fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        selWorkout.athleteWeight?.let { Text("%.1f кг".format(it), color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }
                        Text(if (h > 0) "$h ч $m мин" else "$m мин", color = White.copy(.7f), fontSize = 12.sp)
                        Text("подробнее →", color = Gold, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                    }
                }
            } else if (selPlanned != null && selectedDate != null) {
                HorizontalDivider(color = White.copy(.12f))
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(selectedDate!!.format(DateTimeFormatter.ofPattern("dd.MM.yyyy")), color = Blue, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(selPlanned.muscleGroup, color = White.copy(.7f), fontSize = 12.sp)
                    }
                    Text("%02d:%02d".format(selPlanned.hour, selPlanned.minute), color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            } else { Spacer(Modifier.height(12.dp)) }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  СЕТКА МЕСЯЦА
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun MonthGrid(
    ym: YearMonth, workoutsByDate: Map<LocalDate, List<CompletedWorkout>>,
    recordDays: Set<LocalDate>, plannedByDate: Map<LocalDate, List<PlannedWorkout>>,
    selectedDate: LocalDate?, cell: Dp, onDayClick: (LocalDate) -> Unit
) {
    val offset = ym.atDay(1).dayOfWeek.value - 1
    val days   = ym.lengthOfMonth()
    val rows   = (offset + days + 6) / 7
    Column {
        for (row in 0 until rows) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0..6) {
                    val d = row*7 + col - offset + 1
                    Box(Modifier.size(cell)) {
                        if (d in 1..days) {
                            val date = ym.atDay(d)
                            MonthDayCell(d, cell,
                                hasWorkout = workoutsByDate.containsKey(date),
                                isRecord   = recordDays.contains(date),
                                isPlanned  = plannedByDate.containsKey(date),
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

@Composable
private fun MonthDayCell(dayNum: Int, cell: Dp, hasWorkout: Boolean, isRecord: Boolean,
                         isPlanned: Boolean, isSelected: Boolean, isToday: Boolean, onClick: () -> Unit) {
    Box(Modifier.size(cell).padding(3.dp).background(
        if (isSelected) White.copy(.18f) else Color.Transparent, RoundedCornerShape(10.dp))
        .clickable { onClick() }, contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$dayNum", color = if (isToday) Gold else if (isSelected) White else White.copy(.88f),
                fontSize = 18.sp, fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal)
            Spacer(Modifier.height(2.dp))
            Box(Modifier.size(5.dp).background(when {
                !hasWorkout && !isPlanned -> Color.Transparent
                isRecord   -> Gold
                hasWorkout -> White.copy(.85f)
                else       -> Blue
            }, CircleShape))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  ВКЛАДКА 2: ЗАПЛАНИРОВАННОЕ (еженедельный вид)
// ═══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WeekTab(
    plannedWorkouts: List<PlannedWorkout>,
    plannedByDate:   Map<LocalDate, List<PlannedWorkout>>,
    viewModel:       TrainingSessionViewModel
) {
    val today = LocalDate.now()
    var weekStart by remember {
        mutableStateOf(today.minusDays((today.dayOfWeek.value - 1).toLong()))
    }
    var selectedDate by remember { mutableStateOf<LocalDate?>(today) }
    var showAddSheet by remember { mutableStateOf(false) }
    val addSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    LaunchedEffect(weekStart) { selectedDate = null }

    val weekEnd = weekStart.plusDays(6)
    val fmtShort = DateTimeFormatter.ofPattern("d MMM", Locale("ru"))
    val weekLabel = "${weekStart.format(fmtShort)} — ${weekEnd.format(fmtShort)}"

    val selDayPlanned = selectedDate?.let { plannedByDate[it] } ?: emptyList()

    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Spacer(Modifier.height(14.dp))

        Column(Modifier.fillMaxWidth().background(DarkGray, RoundedCornerShape(20.dp))) {
            // Навигация по неделям
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                NavCircle("<") { weekStart = weekStart.minusWeeks(1) }
                Text(weekLabel, color = White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                NavCircle(">") { weekStart = weekStart.plusWeeks(1) }
            }

            // Заголовки + числа дней
            BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                val cell: Dp = maxWidth / 7
                Row {
                    for (i in 0..6) {
                        val date = weekStart.plusDays(i.toLong())
                        val hasPlan = plannedByDate.containsKey(date)
                        val isSel   = date == selectedDate
                        val isTod   = date == today
                        Box(Modifier.size(cell).padding(3.dp).background(
                            if (isSel) White.copy(.18f) else Color.Transparent, RoundedCornerShape(10.dp))
                            .clickable { selectedDate = if (selectedDate == date) null else date },
                            contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(DAY_LABELS[i], color = White.copy(.55f), fontSize = 11.sp)
                                Spacer(Modifier.height(2.dp))
                                Text("${date.dayOfMonth}",
                                    color = if (isTod) Gold else if (isSel) White else White.copy(.88f),
                                    fontSize = 20.sp, fontWeight = if (isTod || isSel) FontWeight.Bold else FontWeight.Normal)
                                Spacer(Modifier.height(3.dp))
                                Box(Modifier.size(5.dp).background(if (hasPlan) Blue else Color.Transparent, CircleShape))
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        Spacer(Modifier.height(12.dp))

        // Карточки запланированных тренировок выбранного дня
        if (selDayPlanned.isEmpty() && selectedDate != null) {
            Box(Modifier.fillMaxWidth().background(SoftGray, RoundedCornerShape(14.dp))
                .padding(20.dp), contentAlignment = Alignment.Center) {
                Text("Нет тренировок на этот день", color = DarkGray, fontSize = 13.sp)
            }
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(selDayPlanned, key = { it.id }) { planned ->
                PlannedWorkoutCard(planned = planned, onDelete = { viewModel.deletePlannedWorkout(planned.id) })
            }

            item {
                // Кнопка "+" добавить тренировку
                Box(Modifier.fillMaxWidth().height(52.dp)
                    .background(DarkGray, RoundedCornerShape(14.dp))
                    .clickable { showAddSheet = true }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("+", color = White, fontSize = 22.sp, fontWeight = FontWeight.Light)
                        Text("Запланировать тренировку", color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }

    // ── SHEET: ДОБАВИТЬ ПЛАНОВУЮ ТРЕНИРОВКУ ─────────────────────
    if (showAddSheet) {
        ModalBottomSheet(onDismissRequest = { showAddSheet = false }, sheetState = addSheetState,
            containerColor = White, shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)) {
            AddPlannedWorkoutSheet(
                initialDate = selectedDate ?: today,
                weekStart   = weekStart,
                onSave      = { date, hour, min, mg, exercises ->
                    viewModel.addPlannedWorkout(date, hour, min, mg, exercises)
                    showAddSheet = false
                },
                onDismiss = { showAddSheet = false }
            )
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  КАРТОЧКА ПЛАНОВОЙ ТРЕНИРОВКИ
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun PlannedWorkoutCard(planned: PlannedWorkout, onDelete: () -> Unit) {
    Column(Modifier.fillMaxWidth().background(SoftGray, RoundedCornerShape(14.dp))) {
        // Шапка
        Box(Modifier.fillMaxWidth()
            .background(Black, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically) {
                Text(planned.muscleGroup, color = White, fontWeight = FontWeight.Bold, fontSize = 14.sp,
                    modifier = Modifier.weight(1f))
                val dayName = planned.date.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale("ru")).replaceFirstChar { it.uppercase() }
                Text("$dayName • %02d:%02d".format(planned.hour, planned.minute), color = White.copy(.75f), fontSize = 13.sp)
            }
        }
        // Тело: упражнения
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            if (planned.exercises.isEmpty()) {
                Text("Упражнения не указаны", color = DarkGray, fontSize = 13.sp)
            } else {
                planned.exercises.forEach { ex ->
                    Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).background(DarkGray, CircleShape))
                        Spacer(Modifier.width(8.dp))
                        Text(ex, fontSize = 13.sp, color = Black)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            // Удалить
            Box(Modifier.fillMaxWidth().height(36.dp)
                .background(LightGray, RoundedCornerShape(8.dp))
                .clickable { onDelete() }, contentAlignment = Alignment.Center) {
                Text("Удалить", fontSize = 13.sp, color = DarkGray, fontWeight = FontWeight.Medium)
            }
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  SHEET: ДОБАВИТЬ ПЛАНОВУЮ ТРЕНИРОВКУ
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun AddPlannedWorkoutSheet(
    initialDate: LocalDate, weekStart: LocalDate,
    onSave: (LocalDate, Int, Int, String, List<String>) -> Unit,
    onDismiss: () -> Unit
) {
    var selDate by remember { mutableStateOf(initialDate) }
    var hourStr  by remember { mutableStateOf("19") }
    var minStr   by remember { mutableStateOf("00") }
    var selGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    val selExercises = remember { mutableStateListOf<String>() }

    val filteredEx = selGroup?.let { g ->
        ExerciseLibrary.exercises.filter { it.muscleGroup == g }.map { it.name }
    } ?: emptyList()

    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {

        item {
            Text("ЗАПЛАНИРОВАТЬ ТРЕНИРОВКУ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Black,
                modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)
        }

        // Выбор дня (7 кнопок текущей недели)
        item {
            Column {
                Text("День", fontSize = 12.sp, color = DarkGray, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (i in 0..6) {
                        val d = weekStart.plusDays(i.toLong())
                        val sel = d == selDate
                        Box(Modifier.weight(1f).height(52.dp)
                            .background(if (sel) DarkGray else SoftGray, RoundedCornerShape(10.dp))
                            .clickable { selDate = d; selExercises.clear() }, contentAlignment = Alignment.Center) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(DAY_LABELS[i], fontSize = 10.sp, color = if (sel) White.copy(.7f) else DarkGray)
                                Text("${d.dayOfMonth}", fontSize = 15.sp, fontWeight = FontWeight.Bold,
                                    color = if (sel) White else Black)
                            }
                        }
                    }
                }
            }
        }

        // Время
        item {
            Column {
                Text("Время", fontSize = 12.sp, color = DarkGray, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val minuteFocusRequester = remember { FocusRequester() }
                    val focusManager = LocalFocusManager.current
                    OutlinedTextField(hourStr, { hourStr = it.filter(Char::isDigit).take(2) },
                        label = { Text("Час", color = DarkGray) }, modifier = Modifier.width(90.dp), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(onNext = { minuteFocusRequester.requestFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Black, unfocusedBorderColor = LightGray,
                            focusedTextColor = Black, unfocusedTextColor = Black, cursorColor = Black))
                    Text(":", color = Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(minStr, { minStr = it.filter(Char::isDigit).take(2) },
                        label = { Text("Мин", color = DarkGray) },
                        modifier = Modifier.width(90.dp).focusRequester(minuteFocusRequester), singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Black, unfocusedBorderColor = LightGray,
                            focusedTextColor = Black, unfocusedTextColor = Black, cursorColor = Black))
                }
            }
        }

        // Группа мышц
        item {
            Column {
                Text("Группа мышц", fontSize = 12.sp, color = DarkGray, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                // 2 строки чипов
                val groups = MuscleGroup.values().toList()
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (row in groups.chunked(3)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            row.forEach { g ->
                                val sel = g == selGroup
                                Box(Modifier.height(36.dp).background(
                                    if (sel) DarkGray else SoftGray, RoundedCornerShape(10.dp))
                                    .clickable { selGroup = g; selExercises.clear() }
                                    .padding(horizontal = 12.dp), contentAlignment = Alignment.Center) {
                                    Text(g.title, fontSize = 13.sp, color = if (sel) White else Black,
                                        fontWeight = if (sel) FontWeight.Medium else FontWeight.Normal)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Упражнения
        if (filteredEx.isNotEmpty()) {
            item {
                Text("Упражнения (необязательно)", fontSize = 12.sp, color = DarkGray, fontWeight = FontWeight.Medium)
            }
            items(filteredEx) { ex ->
                val checked = ex in selExercises
                Row(Modifier.fillMaxWidth().clickable { if (checked) selExercises.remove(ex) else selExercises.add(ex) }
                    .background(if (checked) DarkGray.copy(.07f) else Color.Transparent, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.size(20.dp).background(if (checked) DarkGray else LightGray, RoundedCornerShape(4.dp)),
                        contentAlignment = Alignment.Center) {
                        if (checked) Text("✓", color = White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(ex, fontSize = 14.sp, color = Black)
                }
            }
        }

        // Сохранить
        item {
            val hour = hourStr.toIntOrNull()?.coerceIn(0, 23) ?: 19
            val min  = minStr.toIntOrNull()?.coerceIn(0, 59)  ?: 0
            val canSave = selGroup != null
            Box(Modifier.fillMaxWidth().height(50.dp)
                .background(if (canSave) Black else LightGray, RoundedCornerShape(12.dp))
                .clickable(enabled = canSave) { onSave(selDate, hour, min, selGroup!!.title, selExercises.toList()) },
                contentAlignment = Alignment.Center) {
                Text("СОХРАНИТЬ", color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  BOTTOM SHEET ДЕТАЛЕЙ ТРЕНИРОВКИ
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun WorkoutDetailSheet(
    workout: CompletedWorkout,
    viewModel: TrainingSessionViewModel,
    onDeleted: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    val dateFmt = DateTimeFormatter.ofPattern("dd MMMM yyyy", Locale("ru"))
    val timeFmt = DateTimeFormatter.ofPattern("HH:mm")
    val totalSets   = workout.exercises.sumOf { it.sets.size }
    val totalVolume = workout.exercises.sumOf { ex -> ex.sets.sumOf { (it.weight * it.reps).toDouble() } }.toInt()
    val h = workout.durationMinutes/60; val m = workout.durationMinutes%60
    val dur = if (h > 0 && m > 0) "$h ч $m мин" else if (h > 0) "$h ч" else "$m мин"

    LazyColumn(Modifier.fillMaxWidth(), contentPadding = PaddingValues(bottom = 32.dp)) {
        item { Column(Modifier.fillMaxWidth().background(DarkGray).padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text(workout.dateTime.format(dateFmt), color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(3.dp))
            Text("${workout.dateTime.format(timeFmt)}  •  ${workout.muscleGroup}", color = White.copy(.7f), fontSize = 13.sp)
        }}
        item { Row(Modifier.fillMaxWidth().background(SoftGray).padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
            DStat("%.1f кг".format(workout.athleteWeight ?: 0f), "вес"); DVert()
            DStat("${workout.exercises.size}", "упражн."); DVert()
            DStat("$totalSets", "подходов"); DVert()
            DStat("$totalVolume кг", "объём"); DVert()
            DStat(dur, "время")
        }}
        item { Spacer(Modifier.height(14.dp)) }
        items(workout.exercises) { ex ->
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 10.dp)) {
                Box(Modifier.fillMaxWidth().background(Black, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Text(ex.name, color = White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                }
                Column(Modifier.fillMaxWidth().background(White, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(Modifier.fillMaxWidth()) {
                        Text("#", Modifier.width(28.dp), fontSize = 11.sp, color = DarkGray)
                        Text("Вес",   Modifier.weight(1f), fontSize = 11.sp, color = DarkGray)
                        Text("Повт.", Modifier.weight(1f), fontSize = 11.sp, color = DarkGray)
                        Text("Объём", Modifier.weight(1f), fontSize = 11.sp, color = DarkGray)
                    }
                    Spacer(Modifier.height(4.dp)); HorizontalDivider(color = LightGray); Spacer(Modifier.height(4.dp))
                    ex.sets.forEachIndexed { idx, set ->
                        Row(Modifier.fillMaxWidth().padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${idx+1}", Modifier.width(28.dp), color = Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            Text("${fmtW(set.weight)} кг", Modifier.weight(1f), color = Black, fontSize = 14.sp)
                            Text("× ${set.reps}", Modifier.weight(1f), color = Black, fontSize = 14.sp)
                            Text("${(set.weight*set.reps).toInt()} кг", Modifier.weight(1f), fontSize = 13.sp, color = DarkGray)
                        }
                        if (idx < ex.sets.lastIndex) HorizontalDivider(color = SoftGray, thickness = .5.dp)
                    }
                    if (ex.sets.isNotEmpty()) {
                        val vol = ex.sets.sumOf { (it.weight*it.reps).toDouble() }.toInt()
                        val max = ex.sets.maxOf { it.weight }
                        Spacer(Modifier.height(6.dp)); HorizontalDivider(color = LightGray); Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Макс: ${fmtW(max)} кг", fontSize = 12.sp, color = DarkGray)
                            Text("Всего: $vol кг", fontSize = 12.sp, color = DarkGray)
                        }
                    }
                }
            }
        }

        // ── ЗАМЕТКИ (если есть) ─────────────────────────────────────
        workout.notes?.takeIf { it.isNotBlank() }?.let { notes ->
            item {
                Column(
                    Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 12.dp)
                        .background(SoftGray, RoundedCornerShape(12.dp))
                        .padding(14.dp)
                ) {
                    Text("ЗАМЕТКИ", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = DarkGray)
                    Spacer(Modifier.height(6.dp))
                    Text(notes, fontSize = 14.sp, color = Black, lineHeight = 19.sp)
                }
            }
        }

        // ── УДАЛИТЬ ТРЕНИРОВКУ ──────────────────────────────────────
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .background(SoftGray, RoundedCornerShape(12.dp))
                    .clickable { showDeleteConfirm = true }
                    .padding(vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Удалить тренировку", color = Color(0xFFE53935), fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = White,
            titleContentColor = Black,
            textContentColor = DarkGray,
            title = { Text("Удалить тренировку?", fontWeight = FontWeight.Bold) },
            text = { Text("Тренировка и все данные о ней (упражнения, подходы) будут удалены без возможности восстановления.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteWorkout(workout.id)
                    showDeleteConfirm = false
                    onDeleted()
                }) { Text("Удалить", color = Color(0xFFE53935), fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Отмена", color = DarkGray) }
            }
        )
    }
}

// ═══════════════════════════════════════════════════════════════════
//  ВСПОМОГАТЕЛЬНЫЕ
// ═══════════════════════════════════════════════════════════════════

@Composable private fun CalTab(title: String, sel: Boolean, onClick: () -> Unit) =
    Column(Modifier.clickable { onClick() }.padding(vertical = 12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, fontSize = 14.sp, fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal, color = if (sel) Black else DarkGray)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(if (sel) 48.dp else 0.dp).height(3.dp).background(Black, RoundedCornerShape(2.dp)))
    }

@Composable private fun NavCircle(label: String, onClick: () -> Unit) =
    Box(Modifier.size(36.dp).background(White.copy(.12f), CircleShape).clickable { onClick() }, Alignment.Center) {
        Text(label, color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
    }

@Composable private fun MiniStat(v: String, l: String) =
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(v, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Black)
        Text(l, fontSize = 11.sp, color = DarkGray)
    }

@Composable private fun Dot(c: Color) = Box(Modifier.size(7.dp).background(c, CircleShape))
@Composable private fun DStat(v: String, l: String) = Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Text(v, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black); Text(l, fontSize = 10.sp, color = DarkGray) }
@Composable private fun DVert() = Box(Modifier.width(1.dp).height(28.dp).background(LightGray))