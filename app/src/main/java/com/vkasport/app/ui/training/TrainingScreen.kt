package com.vkasport.app.ui.training

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.data.model.MeasureType
import com.vkasport.app.data.model.MuscleGroup
import com.vkasport.app.ui.common.SetFormat
import com.vkasport.app.data.model.WorkoutExercise
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import com.vkasport.app.ui.theme.SoftGray
import com.vkasport.app.ui.theme.White
import java.time.format.DateTimeFormatter

private fun formatW(weight: Float): String =
    if (weight == weight.toInt().toFloat()) weight.toInt().toString() else "%.1f".format(weight)

private val GreenColor = Color(0xFF4CAF50)
private val RedColor = Color(0xFFE53935)

@Composable
fun TrainingScreen(
    viewModel: TrainingSessionViewModel,
    onAddExerciseToGroup: (MuscleGroup) -> Unit,
    onAddNewGroup: () -> Unit,
    onFinishWorkout: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val completedWorkouts by viewModel.completedWorkouts.collectAsState()
    val restTimerStart by viewModel.restTimerStart.collectAsState()
    val fmt = DateTimeFormatter.ofPattern("dd.MM.yy HH:mm")

    // Тик раз в секунду для таймера отдыха (только пока этот экран открыт)
    var nowMs by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(restTimerStart) {
        while (true) {
            nowMs = System.currentTimeMillis()
            kotlinx.coroutines.delay(1000)
        }
    }

    // completedWorkouts гарантированно отсортирован от новых к старым —
    // firstOrNull() это последняя завершённая тренировка
    val lastWeight = completedWorkouts.firstOrNull()?.athleteWeight
    val weightDiff = if (lastWeight != null && state.athleteWeight != null)
        state.athleteWeight!! - lastWeight
    else null

    // Диалоги подтверждения удаления
    var confirmRemoveGroup by remember { mutableStateOf<MuscleGroup?>(null) }
    // ИСПРАВЛЕНО: храним весь WorkoutExercise (нужен и id для удаления,
    // и name для текста диалога), а не только имя
    var confirmRemoveExercise by remember { mutableStateOf<WorkoutExercise?>(null) }

    val groupedExercises: Map<MuscleGroup, List<WorkoutExercise>> = remember(state.selectedExercises) {
        state.selectedExercises
            .filter { it.muscleGroup != null }
            .groupBy { it.muscleGroup!! }
    }

    val headerTitle = if (groupedExercises.isNotEmpty())
        groupedExercises.keys.joinToString(", ") { it.title }.uppercase()
    else "ТРЕНИРОВКА"

    Column(modifier = Modifier.fillMaxSize()) {

        // ===== ШАПКА =====
        Column(
            modifier = Modifier.fillMaxWidth().background(Black).padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(headerTitle, color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("начало: ${state.workoutStartTime.format(fmt)}", color = White.copy(alpha = 0.65f), fontSize = 12.sp)

                // Таймер отдыха НАД весом: сбрасывается после каждой записи подхода
                Column(horizontalAlignment = Alignment.End) {
                    restTimerStart?.let { start ->
                        val restSec = ((nowMs - start) / 1000).coerceAtLeast(0)
                        Text(
                            "⏱ %d:%02d".format(restSec / 60, restSec % 60),
                            color = White, fontSize = 16.sp, fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(2.dp))
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(state.athleteWeight?.let { "%.1f".format(it) } ?: "—", color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        Text(" кг", color = White, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                        if (weightDiff != null) {
                            Spacer(Modifier.width(4.dp))
                            val diffColor = when {
                                weightDiff > 0f -> GreenColor
                                weightDiff < 0f -> RedColor
                                else -> White
                            }
                            val sign = if (weightDiff > 0f) "+" else ""
                            Text("($sign%.1f)".format(weightDiff), color = diffColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // ===== СПИСОК ГРУПП МЫШЦ + УПРАЖНЕНИЙ =====
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            groupedExercises.forEach { (group, exercises) ->

                // Заголовок группы мышц с кнопкой удаления группы (✕)
                item(key = "group_${group.name}") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 14.dp)
                            .background(Black, RoundedCornerShape(12.dp))
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            group.title.uppercase(),
                            color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp,
                            modifier = Modifier.align(Alignment.Center)
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 4.dp)
                                .size(26.dp)
                                .clickable { confirmRemoveGroup = group },
                            contentAlignment = Alignment.Center
                        ) {
                            Text("✕", color = White.copy(alpha = 0.8f), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // Упражнения этой группы — ключ по УНИКАЛЬНОМУ id экземпляра
                // (не по имени!), иначе повторное добавление одноимённого
                // упражнения крашит LazyColumn на дублирующихся ключах
                items(exercises, key = { it.id }) { exercise ->
                    InlineExerciseBlock(
                        exercise = exercise,
                        viewModel = viewModel,
                        onRemove = { confirmRemoveExercise = exercise }
                    )
                }

                // Кнопка "+" добавить ещё упражнение в эту же группу
                item(key = "add_ex_${group.name}") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp)
                            .height(46.dp)
                            .background(DarkGray, RoundedCornerShape(12.dp))
                            .clickable { onAddExerciseToGroup(group) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("+", color = White, fontSize = 20.sp, fontWeight = FontWeight.Light)
                    }
                }
            }

            // Кнопка "+" добавить ещё одну группу мышц
            item(key = "add_group") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 14.dp)
                        .height(48.dp)
                        .background(Black, RoundedCornerShape(12.dp))
                        .clickable { onAddNewGroup() },
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("+", color = White, fontSize = 20.sp, fontWeight = FontWeight.Light)
                        Text("Добавить группу мышц", color = White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

        // ===== КНОПКА ЗАВЕРШИТЬ =====
        Column(
            modifier = Modifier.fillMaxWidth().background(White).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(SoftGray, RoundedCornerShape(14.dp))
                    .clickable { onFinishWorkout() },
                contentAlignment = Alignment.Center
            ) {
                Text("ЗАВЕРШИТЬ ТРЕНИРОВКУ", color = Black, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }

    // ===== ДИАЛОГ: УДАЛИТЬ ГРУППУ МЫШЦ =====
    confirmRemoveGroup?.let { group ->
        AlertDialog(
            onDismissRequest = { confirmRemoveGroup = null },
            containerColor = White,
            titleContentColor = Black,
            textContentColor = DarkGray,
            title = { Text("Убрать группу «${group.title}»?", fontWeight = FontWeight.Bold) },
            text = { Text("Все упражнения и подходы этой группы в текущей тренировке будут удалены. Это действие нельзя отменить.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeMuscleGroup(group)
                    confirmRemoveGroup = null
                }) { Text("Убрать", color = RedColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoveGroup = null }) { Text("Отмена", color = DarkGray) }
            }
        )
    }

    // ===== ДИАЛОГ: УДАЛИТЬ УПРАЖНЕНИЕ =====
    confirmRemoveExercise?.let { ex ->
        AlertDialog(
            onDismissRequest = { confirmRemoveExercise = null },
            containerColor = White,
            titleContentColor = Black,
            textContentColor = DarkGray,
            title = { Text("Убрать упражнение?", fontWeight = FontWeight.Bold) },
            text = { Text("«${ex.name}» и все его подходы будут удалены из текущей тренировки.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeExercise(ex.id)
                    confirmRemoveExercise = null
                }) { Text("Убрать", color = RedColor, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { confirmRemoveExercise = null }) { Text("Отмена", color = DarkGray) }
            }
        )
    }
}

@Composable
private fun InlineExerciseBlock(
    exercise: WorkoutExercise,
    viewModel: TrainingSessionViewModel,
    onRemove: () -> Unit
) {
    // addingSet=true — форма добавления НОВОГО подхода в конец
    // editingIndex!=null — форма редактирования УЖЕ введённого подхода
    var addingSet by remember(exercise.id) { mutableStateOf(false) }
    var editingIndex by remember(exercise.id) { mutableStateOf<Int?>(null) }
    var weightInput by remember(exercise.id) { mutableStateOf("") }
    var repsInput by remember(exercise.id) { mutableStateOf("") }
    // Поля для новых типов упражнений (модель v2)
    var minInput by remember(exercise.id) { mutableStateOf("") }
    var secInput by remember(exercise.id) { mutableStateOf("") }
    var kmInput by remember(exercise.id) { mutableStateOf("") }
    var loadInput by remember(exercise.id) { mutableStateOf("") }
    var speedInput by remember(exercise.id) { mutableStateOf("") }

    fun clearInputs() {
        weightInput = ""; repsInput = ""; minInput = ""; secInput = ""
        kmInput = ""; loadInput = ""; speedInput = ""
    }

    val focusManager = LocalFocusManager.current
    val repsFocusRequester = remember(exercise.id) { FocusRequester() }

    fun openAddForm() {
        editingIndex = null
        clearInputs()
        addingSet = true
    }

    fun openEditForm(index: Int, set: com.vkasport.app.data.model.WorkoutSet) {
        addingSet = false
        editingIndex = index
        weightInput = if (set.weight > 0f) formatW(set.weight) else ""
        repsInput = if (set.reps > 0) set.reps.toString() else ""
        minInput = set.seconds?.let { (it / 60).toString() } ?: ""
        secInput = set.seconds?.let { (it % 60).toString() } ?: ""
        kmInput = set.distanceKm?.let { formatW(it) } ?: ""
        loadInput = set.load?.let { formatW(it) } ?: ""
        speedInput = set.speed?.let { formatW(it) } ?: ""
    }

    // ИСПРАВЛЕНО: передаём exercise.id (уникальный id экземпляра), а не
    // exercise.name — ViewModel ищет упражнение по id, поиск по имени
    // никогда не находил совпадения и подход просто не сохранялся
    fun confirmForm() {
        // Собираем подход в зависимости от типа упражнения.
        // return внутри ветки = не все обязательные поля заполнены.
        val newSet: com.vkasport.app.data.model.WorkoutSet = when (exercise.measureType) {
            MeasureType.WEIGHT_REPS -> {
                // Вес МОЖНО оставить пустым — это «без доп. веса» (вес 0)
                val w = weightInput.replace(",", ".").toFloatOrNull() ?: 0f
                val r = repsInput.toIntOrNull() ?: return
                com.vkasport.app.data.model.WorkoutSet(weight = w, reps = r)
            }
            MeasureType.REPS -> {
                val r = repsInput.toIntOrNull() ?: return
                com.vkasport.app.data.model.WorkoutSet(reps = r)
            }
            MeasureType.TIME -> {
                val total = (minInput.toIntOrNull() ?: 0) * 60 + (secInput.toIntOrNull() ?: 0)
                if (total <= 0) return
                com.vkasport.app.data.model.WorkoutSet(seconds = total)
            }
            MeasureType.DISTANCE -> {
                val km = kmInput.replace(",", ".").toFloatOrNull() ?: return
                val total = (minInput.toIntOrNull() ?: 0) * 60 + (secInput.toIntOrNull() ?: 0)
                com.vkasport.app.data.model.WorkoutSet(distanceKm = km, seconds = total)
            }
            MeasureType.CARDIO -> {
                val m = minInput.toIntOrNull() ?: return
                com.vkasport.app.data.model.WorkoutSet(
                    load = loadInput.replace(",", ".").toFloatOrNull(),
                    speed = speedInput.replace(",", ".").toFloatOrNull(),
                    seconds = m * 60
                )
            }
        }
        val idx = editingIndex
        if (idx != null) {
            viewModel.updateSet(exercise.id, idx, newSet)
        } else {
            viewModel.addSetToExercise(exercise.id, newSet)
        }
        clearInputs()
        addingSet = false; editingIndex = null
        focusManager.clearFocus()
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(top = 10.dp)
    ) {
        // Шапка упражнения с кнопкой удаления (✕)
        Box(
            modifier = Modifier.fillMaxWidth()
                .background(DarkGray, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(
                exercise.name,
                color = White, fontWeight = FontWeight.SemiBold, fontSize = 15.sp,
                modifier = Modifier.align(Alignment.CenterStart).padding(end = 30.dp)
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .size(24.dp)
                    .clickable { onRemove() },
                contentAlignment = Alignment.Center
            ) {
                Text("✕", color = White.copy(alpha = 0.8f), fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier.fillMaxWidth()
                .background(White, RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp))
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {

            Row(modifier = Modifier.fillMaxWidth()) {
                Text("#", style = MaterialTheme.typography.labelSmall, color = DarkGray, modifier = Modifier.width(28.dp))
                Text("прошлая тренировка", style = MaterialTheme.typography.labelSmall, color = DarkGray, modifier = Modifier.weight(1f))
                Text("текущая", style = MaterialTheme.typography.labelSmall, color = DarkGray, modifier = Modifier.weight(1f))
            }

            // Все подходы прошлой тренировки для этого упражнения (по
            // ИМЕНИ — это общая история упражнения, а не конкретной
            // карточки) — нужны для "превью" ещё не введённых сегодня
            // подходов
            val previousSets = remember(exercise.name) {
                val list = mutableListOf<com.vkasport.app.data.model.WorkoutSet>()
                var i = 0
                while (true) {
                    val s = viewModel.getPreviousSet(exercise.name, i) ?: break
                    list.add(s)
                    i++
                }
                list
            }

            val totalRows = maxOf(exercise.sets.size, previousSets.size)

            if (totalRows > 0) {
                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = SoftGray)
                Spacer(Modifier.height(6.dp))
            }

            for (index in 0 until totalRows) {
                val currentSet = exercise.sets.getOrNull(index)
                val previousSet = previousSets.getOrNull(index)
                val isNextSlot = currentSet == null && index == exercise.sets.size
                val showInlinePlus = isNextSlot && previousSet != null
                val rowAlpha = if (currentSet != null || isNextSlot) 1f else 0.4f

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .alpha(rowAlpha),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("${index + 1}", color = Black, fontWeight = FontWeight.Bold, modifier = Modifier.width(28.dp))

                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = previousSet?.let { SetFormat.value(exercise.measureType, it) } ?: "—",
                            color = DarkGray, fontSize = 14.sp
                        )
                        if (previousSet != null &&
                            viewModel.isSetRecord(exercise.name, exercise.measureType, previousSet)) {
                            Spacer(Modifier.width(4.dp))
                            Text("🏆", fontSize = 11.sp)
                        }
                    }

                    Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                        if (currentSet != null) {
                            // Строка кликабельна — можно отредактировать уже
                            // введённый подход. Специально без визуальных
                            // подсказок (карандаша и т.п.) — так попросили.
                            Row(
                                modifier = Modifier.clickable { openEditForm(index, currentSet) },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(SetFormat.value(exercise.measureType, currentSet), color = Black, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                                if (viewModel.isSetRecord(exercise.name, exercise.measureType, currentSet)) {
                                    Spacer(Modifier.width(4.dp))
                                    Text("🏆", fontSize = 11.sp)
                                }
                            }
                        } else if (showInlinePlus) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .background(SoftGray, RoundedCornerShape(8.dp))
                                    .clickable { openAddForm() },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = DarkGray, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            if (!addingSet && editingIndex == null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .background(SoftGray, RoundedCornerShape(10.dp))
                        .clickable { openAddForm() },
                    contentAlignment = Alignment.Center
                ) {
                    Text("+ подход", color = Black, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Набор полей зависит от того, как считается упражнение
                    when (exercise.measureType) {
                        MeasureType.WEIGHT_REPS -> {
                            SetField(weightInput, { weightInput = it }, "кг", "без веса",
                                KeyboardType.Decimal, ImeAction.Next, { repsFocusRequester.requestFocus() },
                                Modifier.weight(1f))
                            SetField(repsInput, { repsInput = it }, "повт.", null,
                                KeyboardType.Number, ImeAction.Done, { confirmForm() },
                                Modifier.weight(1f).focusRequester(repsFocusRequester))
                        }
                        MeasureType.REPS -> {
                            SetField(repsInput, { repsInput = it }, "повт.", null,
                                KeyboardType.Number, ImeAction.Done, { confirmForm() },
                                Modifier.weight(1f))
                        }
                        MeasureType.TIME -> {
                            SetField(minInput, { minInput = it }, "мин", null,
                                KeyboardType.Number, ImeAction.Next, { repsFocusRequester.requestFocus() },
                                Modifier.weight(1f))
                            SetField(secInput, { secInput = it }, "сек", null,
                                KeyboardType.Number, ImeAction.Done, { confirmForm() },
                                Modifier.weight(1f).focusRequester(repsFocusRequester))
                        }
                        MeasureType.DISTANCE -> {
                            SetField(kmInput, { kmInput = it }, "км", null,
                                KeyboardType.Decimal, ImeAction.Next, { repsFocusRequester.requestFocus() },
                                Modifier.weight(1f))
                            SetField(minInput, { minInput = it }, "мин", null,
                                KeyboardType.Number, ImeAction.Next, {},
                                Modifier.weight(1f).focusRequester(repsFocusRequester))
                            SetField(secInput, { secInput = it }, "сек", null,
                                KeyboardType.Number, ImeAction.Done, { confirmForm() },
                                Modifier.weight(1f))
                        }
                        MeasureType.CARDIO -> {
                            SetField(loadInput, { loadInput = it }, "нагр.", null,
                                KeyboardType.Decimal, ImeAction.Next, {},
                                Modifier.weight(1f))
                            SetField(speedInput, { speedInput = it }, "км/ч", null,
                                KeyboardType.Decimal, ImeAction.Next, {},
                                Modifier.weight(1f))
                            SetField(minInput, { minInput = it }, "мин", null,
                                KeyboardType.Number, ImeAction.Done, { confirmForm() },
                                Modifier.weight(1f))
                        }
                    }
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Black, RoundedCornerShape(12.dp))
                            .clickable { confirmForm() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text("✓", color = White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
@Composable
private fun SetField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String?,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    onImeAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange,
        label = { Text(label, color = DarkGray) },
        placeholder = placeholder?.let { p ->
            { Text(p, color = DarkGray.copy(alpha = 0.6f), fontSize = 12.sp) }
        },
        modifier = modifier,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onNext = { onImeAction() }, onDone = { onImeAction() }),
        singleLine = true,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Black, unfocusedBorderColor = SoftGray, cursorColor = Black,
            focusedTextColor = Black, unfocusedTextColor = Black, focusedLabelColor = Black
        )
    )
}