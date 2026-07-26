package com.vkasport.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import com.vkasport.app.ui.theme.White
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Линейный график без сторонних библиотек (Compose Canvas).
 *
 * v1.6.1:
 *  – в компактном виде показываются только ПОСЛЕДНИЕ [maxPoints] точек
 *    (по умолчанию 10), чтобы график читался;
 *  – тап по графику открывает полноэкранный вид со ВСЕЙ историей
 *    (до [FULL_LIMIT] точек) с горизонтальной прокруткой.
 *
 * @param values значения по порядку (старые → новые)
 * @param pointLabels подписи (обычно даты) для КАЖДОГО значения — нужны
 *        для подписей осей в обоих режимах
 */
private const val FULL_LIMIT = 1000

@Composable
fun SimpleLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    startLabel: String? = null,
    endLabel: String? = null,
    pointLabels: List<String>? = null,
    maxPoints: Int = 10,
    title: String? = null
) {
    if (values.size < 2) {
        Text(
            "Нужно минимум две записи, чтобы построить график",
            color = DarkGray, fontSize = 12.sp
        )
        return
    }

    // Компактный вид: последние maxPoints точек
    val shown = if (values.size > maxPoints) values.takeLast(maxPoints) else values
    val shownLabels = pointLabels?.let { if (it.size > maxPoints) it.takeLast(maxPoints) else it }
    val hiddenCount = values.size - shown.size

    var showFull by remember { mutableStateOf(false) }
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier) {

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(top = 6.dp, bottom = 2.dp)
                .clickable { showFull = true }
        ) {
            drawChart(shown, textMeasurer)
        }

        Spacer(Modifier.height(2.dp))
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                shownLabels?.firstOrNull() ?: startLabel ?: "",
                color = DarkGray, fontSize = 10.sp
            )
            Spacer(Modifier.weight(1f))
            if (hiddenCount > 0) {
                Text(
                    "нажмите — вся история (" + values.size + ")",
                    color = DarkGray, fontSize = 10.sp
                )
                Spacer(Modifier.weight(1f))
            }
            Text(
                shownLabels?.lastOrNull() ?: endLabel ?: "",
                color = DarkGray, fontSize = 10.sp
            )
        }
    }

    // ── ПОЛНОЭКРАННЫЙ ВИД: вся история с прокруткой ──
    if (showFull) {
        val full = if (values.size > FULL_LIMIT) values.takeLast(FULL_LIMIT) else values
        val fullLabels = pointLabels?.let { if (it.size > FULL_LIMIT) it.takeLast(FULL_LIMIT) else it }
        Dialog(onDismissRequest = { showFull = false }) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(White, RoundedCornerShape(18.dp))
                    .padding(16.dp)
            ) {
                Text(
                    title ?: "Вся история",
                    color = Black, fontSize = 15.sp,
                    style = TextStyle(fontSize = 15.sp)
                )
                Spacer(Modifier.height(4.dp))
                Text("Точек: ${full.size}", color = DarkGray, fontSize = 11.sp)
                Spacer(Modifier.height(10.dp))

                // Каждой точке — минимум 26dp по ширине, иначе прокрутка
                val scrollState = rememberScrollState()
                Box(Modifier.fillMaxWidth().horizontalScroll(scrollState)) {
                    Canvas(
                        modifier = Modifier
                            .width((full.size * 26).coerceAtLeast(280).dp)
                            .height(260.dp)
                    ) {
                        drawChart(full, textMeasurer)
                    }
                }

                Spacer(Modifier.height(6.dp))
                Row(Modifier.fillMaxWidth()) {
                    Text(fullLabels?.firstOrNull() ?: "", color = DarkGray, fontSize = 10.sp)
                    Spacer(Modifier.weight(1f))
                    Text(fullLabels?.lastOrNull() ?: "", color = DarkGray, fontSize = 10.sp)
                }

                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End) {
                    TextButton(onClick = { showFull = false }) { Text("Закрыть") }
                }
            }
        }
    }
}

/**
 * Рисование графика: сетка «круглыми» значениями + линия + точки.
 * Вынесено, чтобы компактный и полноэкранный виды выглядели одинаково.
 */
private fun DrawScope.drawChart(values: List<Float>, textMeasurer: TextMeasurer) {
    if (values.size < 2) return

    val minV = values.min()
    val maxV = values.max()

    // «Круглый» шаг сетки: 1, 2, 2.5, 5, 10, 20, 25, 50…
    val rawSpan = (maxV - minV).takeIf { it > 0f } ?: (abs(maxV).coerceAtLeast(1f) * 0.1f)
    val rawStep = rawSpan / 3f
    val magnitude = 10f.pow(floor(log10(rawStep.toDouble())).toFloat())
    val norm = rawStep / magnitude
    val niceNorm = when {
        norm <= 1f   -> 1f
        norm <= 2f   -> 2f
        norm <= 2.5f -> 2.5f
        norm <= 5f   -> 5f
        else         -> 10f
    }
    val step = niceNorm * magnitude
    val lowV = floor(minV / step) * step
    val highV = ceil(maxV / step) * step
    val span = (highV - lowV).takeIf { it > 0f } ?: step
    val lineCount = (span / step).toInt() + 1

    val labelStyle = TextStyle(color = DarkGray, fontSize = 9.sp)
    val h = size.height
    val sampleW = textMeasurer.measure(SetFormat.num(highV), labelStyle).size.width.toFloat()
    val chartLeft = sampleW + 8.dp.toPx()
    val chartW = (size.width - chartLeft).coerceAtLeast(1f)
    val stepX = chartW / (values.size - 1)

    fun yOf(v: Float): Float = h * (1f - (v - lowV) / span)

    // Сетка + подписи слева
    for (i in 0 until lineCount) {
        val value = highV - step * i
        val y = yOf(value)
        drawLine(
            color = DarkGray.copy(alpha = 0.16f),
            start = Offset(chartLeft, y),
            end = Offset(size.width, y),
            strokeWidth = 1.dp.toPx()
        )
        val measured = textMeasurer.measure(SetFormat.num(value), labelStyle)
        drawText(
            textLayoutResult = measured,
            topLeft = Offset(
                (chartLeft - 6.dp.toPx() - measured.size.width).coerceAtLeast(0f),
                (y - measured.size.height / 2f).coerceIn(0f, h - measured.size.height)
            )
        )
    }

    // Линия
    val path = Path()
    values.forEachIndexed { i, v ->
        val p = Offset(chartLeft + stepX * i, yOf(v))
        if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
    }
    drawPath(path, color = Black, style = Stroke(width = 2.dp.toPx()))

    // Точки
    values.forEachIndexed { i, v ->
        drawCircle(color = Black, radius = 3.5.dp.toPx(), center = Offset(chartLeft + stepX * i, yOf(v)))
    }
}