package com.vkasport.app.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.focus.focusRequester
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import com.vkasport.app.ui.theme.Gold
import com.vkasport.app.ui.theme.LightGray
import com.vkasport.app.ui.theme.SoftGray
import com.vkasport.app.ui.theme.White
import com.vkasport.app.viewmodel.TrainingSessionViewModel
import android.content.Intent
import android.net.Uri
import java.time.LocalDate
import java.time.format.TextStyle as JTextStyle
import java.util.Locale

// ═══════════════════════════════════════════════════════════════════
//  ДАННЫЕ
// ═══════════════════════════════════════════════════════════════════

private data class Tip(val icon: String, val title: String, val body: String)

private val TIPS = listOf(
    Tip("🔥", "Разминка перед тренировкой",
        "Перед рабочими подходами сделайте 5–10 минут лёгкого кардио и 1–2 разминочных подхода с небольшим весом на каждое упражнение. Это снижает риск травм и улучшает качество движений в рабочих подходах."),
    Tip("📈", "Прогрессия нагрузки",
        "Мышцы растут только когда нагрузка постепенно увеличивается. Раз в 1–2 недели старайтесь добавлять немного веса или повторений — но только если предыдущий вес давался уверенно, без потери техники."),
    Tip("🍗", "Питание для роста",
        "Для набора массы держите небольшой профицит калорий и употребляйте около 1.6–2.2 г белка на кг веса тела в день. Без достаточного белка и калорий прогресс в силе и объёме мышц замедляется."),
    Tip("🛌", "Восстановление и сон",
        "Мышцы растут не во время тренировки, а во время отдыха. Старайтесь спать 7–9 часов и давайте каждой группе мышц минимум 48 часов отдыха перед следующей тяжёлой тренировкой."),
    Tip("🎯", "Техника важнее веса",
        "Слишком тяжёлый вес с нарушением техники почти всегда приводит к травмам и меньше нагружает целевую мышцу. Сначала закрепите правильную технику на умеренном весе, и только потом увеличивайте нагрузку."),
    Tip("💧", "Вода и разогрев суставов",
        "Обезвоживание снижает силовые показатели и выносливость. Пейте воду небольшими порциями на протяжении всей тренировки, а не только до или после неё.")
)

private data class ProgramDay(val label: String, val muscleGroup: String, val exercises: List<String>)
private data class Program(val title: String, val subtitle: String, val days: List<ProgramDay>)

private val PROGRAMS = listOf(
    Program(
        title = "3-дневный сплит для начинающих",
        subtitle = "Всё тело за тренировку, 3 раза в неделю",
        days = listOf(
            ProgramDay("День 1", "Ноги", listOf("Приседания со штангой", "Жим ногами", "Румынская тяга")),
            ProgramDay("День 2", "Спина, бицепс", listOf("Тяга верхнего блока", "Тяга штанги в наклоне", "Подъем штанги на бицепс")),
            ProgramDay("День 3", "Грудь, плечи, трицепс", listOf("Жим штанги лёжа", "Жим штанги стоя", "Французский жим"))
        )
    ),
    Program(
        title = "Тренировочный план для новичков",
        subtitle = "С чего начать и как построить первые недели",
        days = listOf(
            ProgramDay("День 1", "Грудь, трицепс", listOf("Жим штанги лёжа", "Жим в тренажёре сидя", "Разгибание рук на блоке")),
            ProgramDay("День 2", "Спина, бицепс", listOf("Тяга верхнего блока", "Тяга гантели одной рукой", "Подъем гантелей на бицепс")),
            ProgramDay("День 3", "Ноги, плечи", listOf("Приседания со штангой", "Выпады с гантелями", "Жим гантелей сидя"))
        )
    ),
    Program(
        title = "Сплит на массу — 3 дня в неделю",
        subtitle = "Проработка крупных групп мышц по дням",
        days = listOf(
            ProgramDay("День 1", "Грудь, ноги", listOf("Жим штанги лёжа", "Приседания со штангой", "Жим ногами")),
            ProgramDay("День 2", "Спина", listOf("Становая тяга", "Тяга штанги в наклоне", "Подтягивания")),
            ProgramDay("День 3", "Плечи, руки", listOf("Жим штанги стоя", "Подъем штанги на бицепс", "Французский жим"))
        )
    )
)

private data class VideoItem(val title: String, val subtitle: String, val videoId: String) {
    val watchUrl get() = "https://www.youtube.com/watch?v=$videoId"
    val thumbnailUrl get() = "https://img.youtube.com/vi/$videoId/hqdefault.jpg"
}

private val VIDEOS = listOf(
    VideoItem("Жим штанги лёжа", "Правильная техника", "nWo_m0REGMA"),
    VideoItem("Становая тяга", "Разбор техники", "ofe8YeSF4F0"),
    VideoItem("Приседания со штангой", "Техника и программа", "aXh2nVAq6-c"),
    VideoItem("Подъём на бицепс", "Техника выполнения", "4VbAyt64r18")
)

// ═══════════════════════════════════════════════════════════════════
//  ROOT
// ═══════════════════════════════════════════════════════════════════

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InfoScreen(viewModel: TrainingSessionViewModel) {
    var selectedTab by remember { mutableStateOf(0) }
    var planningDay by remember { mutableStateOf<ProgramDay?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Экран всегда с белой шапкой — статус-бар с тёмными иконками
    com.vkasport.app.ui.theme.SystemBarsAppearance(statusBarColor = White, darkIcons = true)

    Column(Modifier.fillMaxSize().background(White)) {
        Row(Modifier.fillMaxWidth().background(White).padding(horizontal = 20.dp)) {
            InfoTab("ТРЕНИРОВКИ И СОВЕТЫ", selectedTab == 0) { selectedTab = 0 }
            Spacer(Modifier.width(20.dp))
            InfoTab("ВИДЕО", selectedTab == 1) { selectedTab = 1 }
        }
        HorizontalDivider(color = SoftGray)

        when (selectedTab) {
            0 -> TipsTab(onPlanDay = { planningDay = it })
            else -> VideoTab()
        }
    }

    // ── SHEET: ЗАПЛАНИРОВАТЬ ДЕНЬ ПРОГРАММЫ ─────────────────────
    planningDay?.let { day ->
        ModalBottomSheet(
            onDismissRequest = { planningDay = null },
            sheetState = sheetState,
            containerColor = White,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            QuickPlanSheet(
                day = day,
                onSave = { date, hour, minute ->
                    viewModel.addPlannedWorkout(date, hour, minute, day.muscleGroup, day.exercises)
                    planningDay = null
                },
                onDismiss = { planningDay = null }
            )
        }
    }
}

@Composable
private fun InfoTab(title: String, selected: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.clickable { onClick() }.padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Black else DarkGray)
        Spacer(Modifier.height(4.dp))
        Box(Modifier.width(if (selected) 48.dp else 0.dp).height(3.dp).background(Black, RoundedCornerShape(2.dp)))
    }
}

// ═══════════════════════════════════════════════════════════════════
//  ВКЛАДКА: СОВЕТЫ + ПРОГРАММЫ (полностью внутри приложения)
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun TipsTab(onPlanDay: (ProgramDay) -> Unit) {
    var expandedTip by remember { mutableStateOf<Int?>(null) }
    var expandedProgram by remember { mutableStateOf<Int?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item { SectionLabel("СОВЕТЫ") }

        items(TIPS.size) { index ->
            val tip = TIPS[index]
            val expanded = expandedTip == index
            TipCard(tip, expanded) { expandedTip = if (expanded) null else index }
        }

        item { Spacer(Modifier.height(6.dp)) }
        item { SectionLabel("ГОТОВЫЕ ПРОГРАММЫ") }

        items(PROGRAMS.size) { index ->
            val program = PROGRAMS[index]
            val expanded = expandedProgram == index
            ProgramCard(
                program = program,
                expanded = expanded,
                onToggle = { expandedProgram = if (expanded) null else index },
                onPlanDay = onPlanDay
            )
        }

        // ── ВЕРСИЯ ПРИЛОЖЕНИЯ ────────────────────────────────────────
        item {
            val context = LocalContext.current
            val versionName = remember {
                try {
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName
                } catch (e: Exception) {
                    null
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "VKA SPORT" + (versionName?.let { " · v$it" } ?: ""),
                fontSize = 11.sp,
                color = DarkGray,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun TipCard(tip: Tip, expanded: Boolean, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(SoftGray, RoundedCornerShape(14.dp))
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(36.dp).background(Gold.copy(alpha = 0.18f), CircleShape), Alignment.Center) {
                Text(tip.icon, fontSize = 17.sp)
            }
            Spacer(Modifier.width(12.dp))
            Text(tip.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Black, modifier = Modifier.weight(1f))
            Text(if (expanded) "▲" else "▼", fontSize = 11.sp, color = DarkGray)
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Text(tip.body, fontSize = 13.sp, color = DarkGray, lineHeight = 19.sp)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  КАРТОЧКА ПРОГРАММЫ — раскрывается, показывает дни с "+"
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun ProgramCard(
    program: Program,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPlanDay: (ProgramDay) -> Unit
) {
    Column(Modifier.fillMaxWidth().background(SoftGray, RoundedCornerShape(14.dp))) {

        // Шапка — тап раскрывает/сворачивает
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onToggle() }
                .padding(horizontal = 14.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(program.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Black)
                Spacer(Modifier.height(3.dp))
                Text(program.subtitle, fontSize = 12.sp, color = DarkGray)
            }
            Spacer(Modifier.width(10.dp))
            Text(if (expanded) "▲" else "▼", fontSize = 12.sp, color = DarkGray)
        }

        if (expanded) {
            HorizontalDivider(color = LightGray)
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                program.days.forEachIndexed { idx, day ->
                    ProgramDayRow(day = day, onPlanClick = { onPlanDay(day) })
                    if (idx < program.days.lastIndex) Spacer(Modifier.height(10.dp))
                }
            }
        }
    }
}

@Composable
private fun ProgramDayRow(day: ProgramDay, onPlanClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .background(White, RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(day.label, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
                Spacer(Modifier.width(6.dp))
                Text("• ${day.muscleGroup}", fontSize = 12.sp, color = DarkGray)
            }
            Spacer(Modifier.height(6.dp))
            day.exercises.forEach { ex ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(Modifier.size(5.dp).background(DarkGray, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(ex, fontSize = 13.sp, color = Black)
                }
            }
        }

        Spacer(Modifier.width(10.dp))

        // Кнопка "+" — запланировать этот день
        Box(
            modifier = Modifier.size(34.dp)
                .background(Black, CircleShape)
                .clickable { onPlanClick() },
            contentAlignment = Alignment.Center
        ) {
            Text("+", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// ═══════════════════════════════════════════════════════════════════
//  SHEET: БЫСТРОЕ ПЛАНИРОВАНИЕ ДНЯ ПРОГРАММЫ
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun QuickPlanSheet(
    day: ProgramDay,
    onSave: (LocalDate, Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val today = LocalDate.now()
    val next14Days = remember { (0..13).map { today.plusDays(it.toLong()) } }
    var selDate by remember { mutableStateOf(today) }
    var hourStr by remember { mutableStateOf("19") }
    var minStr  by remember { mutableStateOf("00") }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Text("ЗАПЛАНИРОВАТЬ ТРЕНИРОВКУ", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Black,
            modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center)

        Spacer(Modifier.height(6.dp))

        // Превью того что планируем
        Column(
            modifier = Modifier.fillMaxWidth().background(SoftGray, RoundedCornerShape(12.dp)).padding(12.dp)
        ) {
            Text("${day.label} • ${day.muscleGroup}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black)
            Spacer(Modifier.height(4.dp))
            Text(day.exercises.joinToString(", "), fontSize = 12.sp, color = DarkGray)
        }

        Spacer(Modifier.height(16.dp))
        Text("Дата", fontSize = 12.sp, color = DarkGray, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            items(next14Days.size) { i ->
                val date = next14Days[i]
                val sel = date == selDate
                val dayName = date.dayOfWeek.getDisplayName(JTextStyle.SHORT, Locale("ru")).replaceFirstChar { it.uppercase() }
                Box(
                    modifier = Modifier.width(52.dp).height(56.dp)
                        .background(if (sel) DarkGray else SoftGray, RoundedCornerShape(10.dp))
                        .clickable { selDate = date },
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(dayName, fontSize = 10.sp, color = if (sel) White.copy(.7f) else DarkGray)
                        Text("${date.dayOfMonth}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (sel) White else Black)
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text("Время", fontSize = 12.sp, color = DarkGray, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            val minuteFocusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
            val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
            OutlinedTextField(hourStr, { hourStr = it.filter(Char::isDigit).take(2) },
                label = { Text("Час", color = DarkGray) }, modifier = Modifier.width(90.dp), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Next),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onNext = { minuteFocusRequester.requestFocus() }),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Black, unfocusedBorderColor = LightGray,
                    focusedTextColor = Black, unfocusedTextColor = Black, cursorColor = Black))
            Text(":", color = Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(minStr, { minStr = it.filter(Char::isDigit).take(2) },
                label = { Text("Мин", color = DarkGray) },
                modifier = Modifier.width(90.dp).focusRequester(minuteFocusRequester), singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = androidx.compose.ui.text.input.ImeAction.Done),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(onDone = { focusManager.clearFocus() }),
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Black, unfocusedBorderColor = LightGray,
                    focusedTextColor = Black, unfocusedTextColor = Black, cursorColor = Black))
        }

        Spacer(Modifier.height(20.dp))

        val hour = hourStr.toIntOrNull()?.coerceIn(0, 23) ?: 19
        val min  = minStr.toIntOrNull()?.coerceIn(0, 59)  ?: 0

        Box(
            modifier = Modifier.fillMaxWidth().height(50.dp)
                .background(Black, RoundedCornerShape(12.dp))
                .clickable { onSave(selDate, hour, min) },
            contentAlignment = Alignment.Center
        ) {
            Text("СОХРАНИТЬ", color = White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
        }

        Spacer(Modifier.height(24.dp))
    }
}

// ═══════════════════════════════════════════════════════════════════
//  ВКЛАДКА: ВИДЕО (с превью-картинками)
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun VideoTab() {
    val context = LocalContext.current

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(VIDEOS.size) { index ->
            val video = VIDEOS[index]
            VideoCard(video) {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.watchUrl)))
            }
        }
    }
}

@Composable
private fun VideoCard(video: VideoItem, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(SoftGray, RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        // Превью с YouTube-обложкой
        Box(
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            AsyncImage(
                model = video.thumbnailUrl,
                contentDescription = video.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().background(DarkGray)
            )
            // Затемнение + play-кнопка поверх превью
            Box(
                modifier = Modifier.fillMaxSize().background(Black.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier.size(40.dp).background(White.copy(alpha = 0.92f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("▶", color = Black, fontSize = 15.sp)
                }
            }
        }

        // Подписи под превью
        Column(modifier = Modifier.fillMaxWidth().padding(10.dp)) {
            Text(video.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Black, lineHeight = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text(video.subtitle, fontSize = 11.sp, color = DarkGray)
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