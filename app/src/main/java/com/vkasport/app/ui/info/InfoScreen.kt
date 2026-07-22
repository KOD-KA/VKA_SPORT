package com.vkasport.app.ui.info

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
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
        "Обезвоживание снижает силовые показатели и выносливость. Пейте воду небольшими порциями на протяжении всей тренировки, а не только до или после неё."),
    Tip("🏃", "Кардио и жиросжигание",
        "Жир уходит от дефицита калорий, а кардио помогает его создать. 2–3 сессии в неделю по 20–40 минут (дорожка, велотренажёр, скакалка) — после силовой или в отдельный день. Ориентир по темпу: можете говорить, но петь уже тяжело."),
    Tip("📏", "Замеряйте прогресс",
        "Вес на весах — не вся правда: мышцы тяжелее жира. Раз в 2–4 недели записывайте замеры (талия, грудь, бицепс) в профиле — графики покажут реальные изменения, которых не видно в зеркале изо дня в день."),
    Tip("⚠️", "Перетренированность",
        "Постоянная усталость, падение рабочих весов, плохой сон и раздражительность — сигналы, что организм не успевает восстанавливаться. Сделайте лёгкую неделю: те же упражнения, но веса на 40–50% меньше. Рост происходит в отдыхе, а не на тренировке."),
    Tip("🌬️", "Дыхание под нагрузкой",
        "Усилие — выдох, опускание — вдох. На тяжёлых базовых допустимо короткое натуживание в момент подъёма, но не задерживайте дыхание на всю амплитуду — резко скачет давление.")
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
    ),
    Program(
        title = "Фулбади 2 раза в неделю",
        subtitle = "Всё тело за тренировку — когда мало времени",
        days = listOf(
            ProgramDay("День 1", "Всё тело", listOf("Приседания со штангой", "Жим штанги лёжа", "Тяга верхнего блока", "Планка")),
            ProgramDay("День 2", "Всё тело", listOf("Румынская тяга", "Жим гантелей сидя", "Горизонтальная тяга блока", "Скручивания"))
        )
    ),
    Program(
        title = "Уличный воркаут",
        subtitle = "Турник и брусья — зал не нужен",
        days = listOf(
            ProgramDay("День 1", "Улица", listOf("Подтягивания", "Отжимания на брусьях", "Отжимания")),
            ProgramDay("День 2", "Улица", listOf("Австралийские подтягивания", "Уголок на турнике", "Берпи")),
            ProgramDay("День 3", "Улица", listOf("Подтягивания", "Пистолетик (присед на одной ноге)", "Скакалка"))
        )
    ),
    Program(
        title = "Домашняя без оборудования",
        subtitle = "Только собственный вес",
        days = listOf(
            ProgramDay("День 1", "Дом", listOf("Отжимания", "Приседания без веса", "Планка")),
            ProgramDay("День 2", "Дом", listOf("Выпады на месте", "Обратные отжимания от стула", "Скалолаз")),
            ProgramDay("День 3", "Дом", listOf("Берпи", "Ягодичный мостик", "Боковая планка"))
        )
    ),
    Program(
        title = "Жиросжигание",
        subtitle = "Кардио 3 раза в неделю",
        days = listOf(
            ProgramDay("День 1", "Жиросжигание", listOf("Беговая дорожка", "Скакалка")),
            ProgramDay("День 2", "Жиросжигание", listOf("Велотренажёр", "Ходьба в горку")),
            ProgramDay("День 3", "Жиросжигание", listOf("Бег на улице", "Степпер"))
        )
    )
)

private data class VideoItem(val title: String, val subtitle: String, val watchUrl: String, val thumbnailUrl: String)

// YouTube-видео (техника)
private fun yt(title: String, subtitle: String, id: String) = VideoItem(
    title, subtitle,
    "https://www.youtube.com/watch?v=$id",
    "https://img.youtube.com/vi/$id/hqdefault.jpg"
)

// RUTUBE-видео (зарубежные атлеты). Обложку отдаёт API рутуба по id —
// Coil загружает её по редиректу так же, как ютубовские превью.
private fun rt(title: String, subtitle: String, id: String) = VideoItem(
    title, subtitle,
    "https://rutube.ru/video/$id/",
    "https://rutube.ru/api/video/$id/thumbnail/?redirect=1"
)

private data class VideoSection(val title: String, val videos: List<VideoItem>)

private val VIDEO_SECTIONS = listOf(
    VideoSection("ТЕХНИКА УПРАЖНЕНИЙ", listOf(
        yt("Жим штанги лёжа", "Правильная техника", "nWo_m0REGMA"),
        yt("Становая тяга", "Разбор техники", "ofe8YeSF4F0"),
        yt("Приседания со штангой", "Техника и программа", "aXh2nVAq6-c"),
        yt("Подъём на бицепс", "Техника выполнения", "4VbAyt64r18")
    )),
    VideoSection("ЗВЁЗДЫ ЖЕЛЕЗА · RUTUBE", listOf(
        rt("Арнольд: тренировка груди", "Классика от 7× Мистера Олимпия", "cb1bdb52558e62f54e0ff6777b7ac404"),
        rt("Арнольд: программа, день 1", "Как тренировался Железный Арни", "94dc5afcb4af0d0e04f9b93596707899"),
        rt("Арнольд: бицепс и трицепс", "Продвинутая программа рук", "9ee06938966af90ad0fa330b0dc18731"),
        rt("Крис Бамстед: верх тела", "5× Мистер Олимпия Classic", "245f3dae535f69ffc23aa4808e245cd6"),
        rt("Программа Криса Бамстеда", "Полный разбор на русском", "2ceb2ed3f2aecbc7e574b0571226d706"),
        rt("Бамстед и Дэвид Лейд", "Совместная тренировка в Майами", "a468011ea28e81d106d6219bcb6e294a"),
        rt("Ронни Коулмэн: плечи", "Убойная тренировка плеч", "ff6c33e2883685326360fda06b4ede42"),
        rt("Коулмэн: жим ногами 1044 кг", "Легендарный рекорд", "a95ae4d511a36875884bfd4f9cfac440")
    )),
    VideoSection("ВОРКАУТ · RUTUBE", listOf(
        rt("Программа Ганнибала", "Дойдёшь до конца?", "1ea40c9966fc0bf904c3ce3e945a68ee"),
        rt("Ганнибал: 580 повторений", "Тренировка за 45 минут", "56c8011862e03ed585f24ea38d0f036a"),
        rt("Ганнибал: база для новичков", "С чего начать воркаут", "a01b0cd700408ea79ecfe29146d4e453"),
        rt("Крис Хериа: калистеника", "Тренировка с собственным весом", "d7a13190c9e0a746b292f1b4f0a4a095"),
        rt("Крис Хериа: выход силой", "Обучение, часть 1", "cd408669f2c9fbbb2fd717dfb2218d87"),
        rt("Крис Хериа: стойка на руках", "4 шага к стойке", "ba482e3f4621ce8ac7b29c109c522bf8"),
        rt("Андрей Смаев: подтягивания +170 кг", "Рекорд стритлифтинга", "470740558460e985bfe299e5966d7d31"),
        rt("Метод тренировок Смаева", "Как тренируется богатырь", "d28d9b2e808c617234a0612dc12af7d4")
    )),
    VideoSection("МОТИВАЦИЯ · RUTUBE", listOf(
        rt("Дэвид Гоггинс: 10 минут", "Речь, меняющая жизнь", "f65bbf14a09262e6872dcc2b98e3c97b"),
        rt("Гоггинс бежит и мотивирует", "Час дисциплины", "40748036de8f910aa521ddfcfee11d3e"),
        rt("Гоггинс: жёсткая речь", "Мотивация для жизни", "abb95412f53d988cce8c277dbc6d7884"),
        rt("Бамстед: мотивация в зале", "Настрой чемпиона", "f2309a14d320d2cb32ce6f67eed7fd15"),
        rt("Коулмэн: light weight baby!", "Классика мотивации", "9a2e03afcca6c1ae3d083f5e3dde1aa0"),
        rt("Ганнибал: король воркаута", "Street Workout мотивация", "af474b52390b061cdf8e5b022a334c9d")
    ))
)

// ═══════════════════════════════════════════════════════════════════
//  ТРЕНИРОВКИ ЛЕГЕНД
// ═══════════════════════════════════════════════════════════════════

private data class Legend(
    val emoji: String,
    val name: String,
    val tagline: String,
    val description: String,
    val workout: List<String>,
    // Для планирования «попробовать повторить» — как у готовых программ:
    // группа-ярлык и чистые названия упражнений из библиотеки
    val muscleGroup: String,
    val plannableExercises: List<String>
)

private val LEGENDS = listOf(
    Legend("🏆", "Арнольд Шварценеггер", "7× Мистер Олимпия",
        "Тренировался шесть дней в неделю, иногда дважды в день. Фирменный приём — " +
                "суперсеты грудь+спина: жим лёжа сразу после подтягиваний, без отдыха. " +
                "Верил в «памп» и полную ментальную концентрацию на рабочей мышце.",
        listOf(
            "Жим штанги лёжа — 5×8-12 (суперсет с подтягиваниями)",
            "Подтягивания — 5× до отказа",
            "Жим штанги на наклонной скамье — 4×10",
            "Разводка гантелей лёжа — 4×10",
            "Пуловер с гантелей — 4×12",
            "Тяга штанги в наклоне — 4×10"
        ),
        muscleGroup = "Грудь, спина",
        plannableExercises = listOf("Жим штанги лёжа", "Подтягивания", "Жим штанги на наклонной скамье", "Разводка гантелей лёжа", "Пуловер с гантелей", "Тяга штанги в наклоне")
    ),
    Legend("👑", "Ганнибал Фор Кинг", "Легенда стрит-воркаута",
        "Начал тренироваться на улицах Нью-Йорка, потому что не было денег на зал — " +
                "так родился современный воркаут. Круговой метод: отжимания → турник → " +
                "брусья без отдыха между упражнениями, 500+ повторений за тренировку.",
        listOf(
            "Круги-лесенки без отдыха:",
            "Отжимания — 30, 29, 28 … 20",
            "Подтягивания — 10, 9, 8 … 5",
            "Отжимания на брусьях — 20, 19 … 10",
            "Подтягивания обратным хватом — 10, 9 … 5",
            "Итого за тренировку ~580 повторений"
        ),
        muscleGroup = "Улица",
        plannableExercises = listOf("Отжимания", "Подтягивания", "Отжимания на брусьях")
    ),
    Legend("💪", "Ронни Коулмэн", "8× Мистер Олимпия",
        "«Все хотят быть бодибилдерами, но никто не хочет поднимать тяжёлое железо». " +
                "Тренировался как пауэрлифтер: тяжёлая база — присед 365 кг, " +
                "жим ногами больше тонны. Yeah buddy! Light weight baby!",
        listOf(
            "Приседания со штангой — 5×10-12 (тяжёлые)",
            "Жим ногами — 4×12",
            "Выпады с гантелями — 3×12",
            "Сгибание ног в тренажёре — 4×12",
            "Румынская тяга — 4×10"
        ),
        muscleGroup = "Ноги",
        plannableExercises = listOf("Приседания со штангой", "Жим ногами", "Выпады с гантелями", "Сгибание ног в тренажёре", "Румынская тяга")
    ),
    Legend("🐺", "Дориан Йейтс", "6× Мистер Олимпия",
        "Метод «кровь и кишки»: после разминочных — одна-единственная рабочая серия " +
                "до полного отказа. Тренировки короткие (45-50 минут) и предельно тяжёлые. " +
                "Всю карьеру вёл дневник тренировок — как вы в этом приложении.",
        listOf(
            "Тяга верхнего блока — разминка + 1×6-8 до отказа",
            "Тяга штанги в наклоне — 1×6-8 до отказа",
            "Тяга гантели одной рукой — 1×8 до отказа",
            "Гиперэкстензия — 1×10-12",
            "Становая тяга — 1×6-8"
        ),
        muscleGroup = "Спина",
        plannableExercises = listOf("Тяга верхнего блока", "Тяга штанги в наклоне", "Тяга гантели одной рукой", "Гиперэкстензия", "Становая тяга")
    ),
    Legend("🇷🇺", "Андрей Смаев", "МСМК по стритлифтингу",
        "Русский богатырь из посёлка Сатис. Начинал с дворовых турников, а сейчас " +
                "подтягивается с дополнительным весом +170 кг и жмёт лёжа за 300 кг. " +
                "Доказательство, что база и турник с брусьями важнее модных тренажёров.",
        listOf(
            "Подтягивания с весом — 5×5 (тяжёлые)",
            "Отжимания на брусьях с весом — 5×5",
            "Жим штанги лёжа — 5×5",
            "Становая тяга — 5×5",
            "Гиперэкстензия — 3×15",
            "Плюс объёмный день: 300 подтягиваний + 180 брусья"
        ),
        muscleGroup = "Улица",
        plannableExercises = listOf("Подтягивания", "Отжимания на брусьях", "Жим штанги лёжа", "Становая тяга", "Гиперэкстензия")
    )
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
    var expandedLegend by remember { mutableStateOf<Int?>(null) }

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

        item { Spacer(Modifier.height(6.dp)) }
        item { SectionLabel("ТРЕНИРОВКИ ЛЕГЕНД") }

        items(LEGENDS.size) { index ->
            val legend = LEGENDS[index]
            val expanded = expandedLegend == index
            LegendCard(
                legend = legend,
                expanded = expanded,
                onPlan = { onPlanDay(ProgramDay(legend.name, legend.muscleGroup, legend.plannableExercises)) }
            ) { expandedLegend = if (expanded) null else index }
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
                text = "STYRK" + (versionName?.let { " · v$it" } ?: ""),
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
        VIDEO_SECTIONS.forEach { section ->
            item(span = { GridItemSpan(2) }) {
                Column {
                    Spacer(Modifier.height(4.dp))
                    SectionLabel(section.title)
                }
            }
            items(section.videos.size) { index ->
                val video = section.videos[index]
                VideoCard(video) {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(video.watchUrl)))
                }
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
// ═══════════════════════════════════════════════════════════════════
//  КАРТОЧКА ЛЕГЕНДЫ
// ═══════════════════════════════════════════════════════════════════

@Composable
private fun LegendCard(legend: Legend, expanded: Boolean, onPlan: () -> Unit, onClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Black, RoundedCornerShape(14.dp))
            .clickable { onClick() }.padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(legend.emoji, fontSize = 20.sp)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(legend.name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = White)
                Text(legend.tagline, fontSize = 11.sp, color = White.copy(alpha = 0.6f))
            }
            // «+» — запланировать (попробовать повторить) тренировку легенды.
            // Своя clickable-зона, тап по ней не разворачивает карточку.
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .background(White, CircleShape)
                    .clickable { onPlan() },
                contentAlignment = Alignment.Center
            ) {
                Text("+", color = Black, fontSize = 18.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(10.dp))
            Text(if (expanded) "▲" else "▼", fontSize = 11.sp, color = White.copy(alpha = 0.6f))
        }
        if (expanded) {
            Spacer(Modifier.height(10.dp))
            Text(legend.description, fontSize = 12.sp, color = White.copy(alpha = 0.85f), lineHeight = 18.sp)
            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = White.copy(alpha = 0.15f))
            Spacer(Modifier.height(10.dp))
            legend.workout.forEach { line ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 2.dp)) {
                    Box(Modifier.size(5.dp).background(Gold, CircleShape))
                    Spacer(Modifier.width(8.dp))
                    Text(line, fontSize = 12.sp, color = White, lineHeight = 17.sp)
                }
            }
        }
    }
}