package com.vkasport.app.ui.profile

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.data.local.entity.BodyMetricEntity
import com.vkasport.app.data.model.StrengthStandard
import com.vkasport.app.data.model.StrengthStandards
import com.vkasport.app.ui.common.SetFormat
import com.vkasport.app.ui.common.SimpleLineChart
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import com.vkasport.app.ui.theme.SoftGray
import com.vkasport.app.ui.theme.White
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit

/**
 * Вкладка «Профиль» (страница 4 Pager).
 *
 * Разделы: сводная статистика, ТЕЛО (вес и замеры с графиками),
 * ДАННЫЕ (бэкап/восстановление), О приложении.
 * Дальше добавятся: прогресс, сравнение с нормативами, пожертвования.
 */
@Composable
fun ProfileScreen(viewModel: com.vkasport.app.viewmodel.TrainingSessionViewModel) {

    val workouts by viewModel.completedWorkouts.collectAsState()
    val records by viewModel.exerciseHistory.collectAsState()
    val bodyMetrics by viewModel.bodyMetrics.collectAsState()
    val context = LocalContext.current
    val dateFmt = remember { DateTimeFormatter.ofPattern("dd.MM.yy") }

    LaunchedEffect(Unit) { viewModel.loadBodyMetrics() }

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

    val weightHistory = remember(workouts, bodyMetrics) { viewModel.getWeightHistory() }
    val currentWeight = weightHistory.lastOrNull()?.second

    // ===== ЛАУНЧЕРЫ БЭКАПА (SAF) =====
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.exportBackup(context, it) { _, msg ->
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        }
    }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { pendingImportUri = it } }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Восстановить из бэкапа?") },
            text = {
                Text(
                    "Все текущие данные (архив, рекорды, план, свои упражнения, " +
                            "журнал тела) будут ЗАМЕНЕНЫ данными из файла. Отменить это будет нельзя."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    pendingImportUri = null
                    viewModel.importBackup(context, uri) { _, msg ->
                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    }
                }) { Text("ВОССТАНОВИТЬ") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Отмена") }
            }
        )
    }

    // ===== ДИАЛОГ ДОБАВЛЕНИЯ ЗАМЕРОВ =====
    var showMeasureDialog by remember { mutableStateOf(false) }
    if (showMeasureDialog) {
        MeasureDialog(
            onDismiss = { showMeasureDialog = false },
            onSave = { entity ->
                viewModel.addBodyMetric(entity)
                showMeasureDialog = false
                Toast.makeText(context, "Замеры записаны", Toast.LENGTH_SHORT).show()
            }
        )
    }

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
            StatCard(workouts.size.toString(), "тренировок всего", Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            StatCard(last30.toString(), "за 30 дней", Modifier.weight(1f))
        }
        Spacer(Modifier.height(12.dp))
        Row(Modifier.fillMaxWidth()) {
            StatCard(records.size.toString(), "рекордов", Modifier.weight(1f))
            Spacer(Modifier.width(12.dp))
            StatCard(daysSinceStart?.let { "$it" } ?: "—", "дней с первой тренировки", Modifier.weight(1f))
        }

        Spacer(Modifier.height(12.dp))

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
                Text("общий поднятый объём", color = White.copy(alpha = 0.65f), fontSize = 12.sp)
            }
        }

        Spacer(Modifier.height(24.dp))

        // ===== ТЕЛО: ВЕС И ЗАМЕРЫ =====
        Text("ТЕЛО", color = Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))

        // График веса
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftGray, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("ВЕС, КГ", color = Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.weight(1f))
                currentWeight?.let {
                    Text("%.1f".format(it), color = Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(Modifier.height(8.dp))
            SimpleLineChart(
                values = weightHistory.map { it.second },
                startLabel = weightHistory.firstOrNull()?.first?.format(dateFmt),
                endLabel = weightHistory.lastOrNull()?.first?.format(dateFmt)
            )
        }

        Spacer(Modifier.height(12.dp))

        // График выбранного замера
        val metricOptions: List<Pair<String, (BodyMetricEntity) -> Float?>> = remember {
            listOf(
                "Грудь" to { m: BodyMetricEntity -> m.chest },
                "Талия" to { m: BodyMetricEntity -> m.waist },
                "Бёдра" to { m: BodyMetricEntity -> m.hips },
                "Бицепс" to { m: BodyMetricEntity -> m.biceps },
                "Предплечье" to { m: BodyMetricEntity -> m.forearm },
                "Бедро" to { m: BodyMetricEntity -> m.thigh },
                "Икра" to { m: BodyMetricEntity -> m.calf },
                "Шея" to { m: BodyMetricEntity -> m.neck },
                "Плечи" to { m: BodyMetricEntity -> m.shoulders }
            )
        }
        var selectedMetric by remember { mutableStateOf(0) }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(SoftGray, RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text("ЗАМЕРЫ, СМ", color = Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // Выбор метрики (горизонтальная прокрутка)
            Row(Modifier.horizontalScroll(rememberScrollState())) {
                metricOptions.forEachIndexed { index, (label, _) ->
                    val selected = index == selectedMetric
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(
                                if (selected) Black else White,
                                RoundedCornerShape(20.dp)
                            )
                            .clickable { selectedMetric = index }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            label,
                            color = if (selected) White else Black,
                            fontSize = 12.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
            Spacer(Modifier.height(10.dp))

            val selector = metricOptions[selectedMetric].second
            val metricPoints = bodyMetrics.mapNotNull { m ->
                selector(m)?.let { LocalDate.ofEpochDay(m.date) to it }
            }
            SimpleLineChart(
                values = metricPoints.map { it.second },
                startLabel = metricPoints.firstOrNull()?.first?.format(dateFmt),
                endLabel = metricPoints.lastOrNull()?.first?.format(dateFmt)
            )
        }

        Spacer(Modifier.height(10.dp))

        // Кнопка добавления замеров
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Black, RoundedCornerShape(14.dp))
                .clickable { showMeasureDialog = true }
                .padding(vertical = 14.dp),
            contentAlignment = Alignment.Center
        ) {
            Text("+ ЗАПИСАТЬ ЗАМЕРЫ", color = White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        // Последние записи журнала (новые сверху)
        if (bodyMetrics.isNotEmpty()) {
            Spacer(Modifier.height(10.dp))
            bodyMetrics.takeLast(5).reversed().forEach { m ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .background(SoftGray, RoundedCornerShape(10.dp))
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        LocalDate.ofEpochDay(m.date).format(dateFmt),
                        color = Black, fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(10.dp))
                    val summary = listOfNotNull(
                        m.weight?.let { "вес ${SetFormat.num(it)}" },
                        m.chest?.let { "грудь ${SetFormat.num(it)}" },
                        m.waist?.let { "талия ${SetFormat.num(it)}" },
                        m.biceps?.let { "бицепс ${SetFormat.num(it)}" }
                    ).joinToString(" · ").ifEmpty { "запись" }
                    Text(summary, color = DarkGray, fontSize = 11.sp, modifier = Modifier.weight(1f))
                    Text(
                        "✕", color = DarkGray, fontSize = 13.sp,
                        modifier = Modifier
                            .clickable { viewModel.deleteBodyMetric(m.id) }
                            .padding(4.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        // ===== СРАВНЕНИЕ С НОРМАТИВАМИ =====
        Text("СРАВНЕНИЕ", color = Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text(
            "Ориентиры силовых нормативов относительно веса тела (мужчины). " +
                    "«Мастер» — уровень занимающихся 5+ лет, не официальное звание. " +
                    "1ПМ оценивается по вашему лучшему подходу (формула Эпли).",
            color = DarkGray, fontSize = 11.sp, lineHeight = 15.sp
        )
        Spacer(Modifier.height(10.dp))

        if (currentWeight == null) {
            Text(
                "Введите свой вес (на старте тренировки или в журнале тела), " +
                        "чтобы увидеть сравнение",
                color = DarkGray, fontSize = 12.sp
            )
        } else {
            var shownAny = false
            StrengthStandards.standards.forEach { std ->
                val rec = records[std.exerciseName] ?: return@forEach
                if (rec.maxWeight <= 0f) return@forEach
                shownAny = true
                StandardCard(std, rec.maxWeight, rec.maxWeightReps, currentWeight)
                Spacer(Modifier.height(10.dp))
            }
            if (!shownAny) {
                Text(
                    "Сделайте жим лёжа, присед, становую или жим стоя — " +
                            "здесь появится сравнение с нормативами",
                    color = DarkGray, fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        // ===== ДАННЫЕ: БЭКАП / ВОССТАНОВЛЕНИЕ =====
        Text("ДАННЫЕ", color = Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))

        ActionButton(
            emoji = "💾",
            title = "Сохранить бэкап",
            subtitle = "Все данные в один файл — архив, рекорды, план, свои упражнения, журнал тела"
        ) {
            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            exportLauncher.launch("vka_sport_backup_$today.json")
        }
        Spacer(Modifier.height(10.dp))
        ActionButton(
            emoji = "📂",
            title = "Восстановить из бэкапа",
            subtitle = "Заменит текущие данные данными из файла (работает и со старыми версиями бэкапов)"
        ) {
            importLauncher.launch(arrayOf("*/*"))
        }

        Spacer(Modifier.height(24.dp))

        // ===== ПОДДЕРЖАТЬ АВТОРА =====
        Text("ПОДДЕРЖКА", color = Black, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Black, RoundedCornerShape(16.dp))
                .clickable {
                    // Открываем страницу поддержки во внешнем браузере/приложении.
                    // try/catch на случай, если на устройстве нет браузера.
                    try {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse(DONATION_URL))
                        )
                    } catch (e: Exception) {
                        Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
                    }
                }
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("❤️", fontSize = 22.sp)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text("Поддержать автора", color = White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(2.dp))
                Text(
                    "Приложение развивается силами одного человека. Любая поддержка помогает добавлять новые функции.",
                    color = White.copy(alpha = 0.7f), fontSize = 11.sp, lineHeight = 15.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Text("→", color = White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.weight(1f, fill = true))
        Spacer(Modifier.height(28.dp))

        // ===== О ПРИЛОЖЕНИИ =====
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
                        "календарь с напоминаниями, таймер отдыха и журнал тела.",
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

// ===== ДИАЛОГ ЗАМЕРОВ =====
@Composable
private fun MeasureDialog(
    onDismiss: () -> Unit,
    onSave: (BodyMetricEntity) -> Unit
) {
    var weightIn by remember { mutableStateOf("") }
    var chestIn by remember { mutableStateOf("") }
    var waistIn by remember { mutableStateOf("") }
    var hipsIn by remember { mutableStateOf("") }
    var bicepsIn by remember { mutableStateOf("") }
    var forearmIn by remember { mutableStateOf("") }
    var thighIn by remember { mutableStateOf("") }
    var calfIn by remember { mutableStateOf("") }
    var neckIn by remember { mutableStateOf("") }
    var shouldersIn by remember { mutableStateOf("") }

    fun parse(s: String): Float? = s.replace(",", ".").toFloatOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Замеры на сегодня") },
        text = {
            Column(
                Modifier
                    .heightIn(max = 380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Заполните только то, что измерили — остальное можно оставить пустым.",
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))
                MeasureField("Вес, кг", weightIn) { weightIn = it }
                MeasureField("Грудь, см", chestIn) { chestIn = it }
                MeasureField("Талия, см", waistIn) { waistIn = it }
                MeasureField("Бёдра (таз), см", hipsIn) { hipsIn = it }
                MeasureField("Бицепс, см", bicepsIn) { bicepsIn = it }
                MeasureField("Предплечье, см", forearmIn) { forearmIn = it }
                MeasureField("Бедро, см", thighIn) { thighIn = it }
                MeasureField("Икра, см", calfIn) { calfIn = it }
                MeasureField("Шея, см", neckIn) { neckIn = it }
                MeasureField("Плечи, см", shouldersIn) { shouldersIn = it }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val entity = BodyMetricEntity(
                    date = LocalDate.now().toEpochDay(),
                    weight = parse(weightIn),
                    chest = parse(chestIn),
                    waist = parse(waistIn),
                    hips = parse(hipsIn),
                    biceps = parse(bicepsIn),
                    forearm = parse(forearmIn),
                    thigh = parse(thighIn),
                    calf = parse(calfIn),
                    neck = parse(neckIn),
                    shoulders = parse(shouldersIn)
                )
                val hasAnything = listOf(
                    entity.weight, entity.chest, entity.waist, entity.hips,
                    entity.biceps, entity.forearm, entity.thigh, entity.calf,
                    entity.neck, entity.shoulders
                ).any { it != null }
                if (hasAnything) onSave(entity)
            }) { Text("СОХРАНИТЬ") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Отмена") }
        }
    )
}

@Composable
private fun MeasureField(label: String, value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        label = { Text(label, fontSize = 12.sp) },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Black, cursorColor = Black,
            focusedTextColor = Black, unfocusedTextColor = Black
        )
    )
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
        Text(label, color = DarkGray, fontSize = 11.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ActionButton(
    emoji: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftGray, RoundedCornerShape(16.dp))
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(title, color = Black, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, color = DarkGray, fontSize = 11.sp, lineHeight = 15.sp)
        }
    }
}

// ===== КАРТОЧКА НОРМАТИВА =====
@Composable
private fun StandardCard(
    std: StrengthStandard,
    maxWeight: Float,
    maxWeightReps: Int,
    bodyWeight: Float
) {
    val orm = StrengthStandards.estimate1RM(maxWeight, maxWeightReps)
    val ratio = orm / bodyWeight
    val fraction = (ratio / std.master).coerceIn(0f, 1f)
    val level = StrengthStandards.levelName(ratio, std)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(SoftGray, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                std.exerciseName, color = Black, fontSize = 13.sp,
                fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f)
            )
            Text(level, color = Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(
            "Ваш 1ПМ ≈ ${SetFormat.num(orm)} кг (×${"%.2f".format(ratio)} веса тела)",
            color = DarkGray, fontSize = 11.sp
        )
        Spacer(Modifier.height(8.dp))
        // Шкала: заполнение до уровня «Мастер (5+ лет)»
        Box(
            Modifier
                .fillMaxWidth()
                .height(8.dp)
                .background(White, RoundedCornerShape(4.dp))
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(8.dp)
                    .background(Black, RoundedCornerShape(4.dp))
            )
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("нов. ${SetFormat.num(std.beginner * bodyWeight)}", color = DarkGray, fontSize = 10.sp)
            Text("люб. ${SetFormat.num(std.intermediate * bodyWeight)}", color = DarkGray, fontSize = 10.sp)
            Text("прод. ${SetFormat.num(std.advanced * bodyWeight)}", color = DarkGray, fontSize = 10.sp)
            Text("мастер ${SetFormat.num(std.master * bodyWeight)}", color = DarkGray, fontSize = 10.sp)
        }
    }
}

// Ссылка на страницу поддержки автора (Boosty). Поменять здесь при
// необходимости — единственное место, где задаётся адрес.
private const val DONATION_URL =
    "https://boosty.to/rstrtrt1/purchase/4022729?ssource=DIRECT&share=subscription_link"