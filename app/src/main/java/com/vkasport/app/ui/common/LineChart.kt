package com.vkasport.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.pow

/**
 * Простой линейный график без сторонних библиотек (Compose Canvas).
 *
 * ОБНОВЛЕНО: сетка с «круглыми» значениями (1/2/2.5/5/10…) и подписями
 * СЛЕВА — так они не налезают на линию графика и легко читаются.
 *
 * @param values значения по порядку (слева направо, старые -> новые)
 * @param startLabel подпись слева под графиком (например, первая дата)
 * @param endLabel подпись справа под графиком (например, последняя дата)
 */
@Composable
fun SimpleLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    startLabel: String? = null,
    endLabel: String? = null
) {
    if (values.size < 2) {
        Text(
            "Нужно минимум две записи, чтобы построить график",
            color = DarkGray, fontSize = 12.sp
        )
        return
    }

    val minV = values.min()
    val maxV = values.max()

    // ── «Круглый» шаг сетки: 1, 2, 2.5, 5, 10, 20, 25, 50… ──
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

    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(color = DarkGray, fontSize = 9.sp)

    Column(modifier = modifier) {

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(top = 6.dp, bottom = 2.dp)
        ) {
            val h = size.height
            // Ширина колонки подписей слева — по самой длинной подписи
            val sampleText = SetFormat.num(highV)
            val labelW = textMeasurer.measure(sampleText, labelStyle).size.width.toFloat() + 8.dp.toPx()
            val chartLeft = labelW
            val chartW = (size.width - chartLeft).coerceAtLeast(1f)
            val stepX = chartW / (values.size - 1)

            fun yOf(v: Float): Float = h * (1f - (v - lowV) / span)

            // ── Сетка: полупрозрачные линии + подписи значений слева ──
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

            // ── Линия графика ──
            val path = Path()
            values.forEachIndexed { i, v ->
                val p = Offset(chartLeft + stepX * i, yOf(v))
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, color = Black, style = Stroke(width = 2.dp.toPx()))

            // ── Точки ──
            values.forEachIndexed { i, v ->
                drawCircle(
                    color = Black,
                    radius = 3.5.dp.toPx(),
                    center = Offset(chartLeft + stepX * i, yOf(v))
                )
            }
        }

        if (startLabel != null || endLabel != null) {
            Spacer(Modifier.height(2.dp))
            Row(Modifier.fillMaxWidth()) {
                Text(startLabel ?: "", color = DarkGray, fontSize = 10.sp)
                Spacer(Modifier.weight(1f))
                Text(endLabel ?: "", color = DarkGray, fontSize = 10.sp)
            }
        }
    }
}