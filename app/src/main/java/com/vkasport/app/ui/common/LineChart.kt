package com.vkasport.app.ui.common

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vkasport.app.ui.theme.Black
import com.vkasport.app.ui.theme.DarkGray
import com.vkasport.app.ui.theme.SoftGray

/**
 * Простой линейный график без сторонних библиотек (Compose Canvas).
 * Используется в профиле для веса и замеров тела; позже — для прогресса
 * по упражнениям.
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
    val range = (maxV - minV).takeIf { it > 0f } ?: 1f

    Column(modifier = modifier) {
        // Подписи максимума/минимума
        Text(SetFormat.num(maxV), color = DarkGray, fontSize = 10.sp)

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
        ) {
            val w = size.width
            val h = size.height
            val stepX = w / (values.size - 1)

            fun pointAt(i: Int): Offset {
                // 6% отступ сверху и снизу, чтобы точки не липли к краям
                val norm = (values[i] - minV) / range
                val y = h * (0.94f - 0.88f * norm)
                return Offset(stepX * i, y)
            }

            // Сетка: три горизонтальные линии
            for (frac in listOf(0.06f, 0.5f, 0.94f)) {
                drawLine(
                    color = SoftGray,
                    start = Offset(0f, h * frac),
                    end = Offset(w, h * frac),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Линия графика
            val path = Path()
            for (i in values.indices) {
                val p = pointAt(i)
                if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
            }
            drawPath(path, color = Black, style = Stroke(width = 2.dp.toPx()))

            // Точки
            for (i in values.indices) {
                drawCircle(color = Black, radius = 3.dp.toPx(), center = pointAt(i))
            }
        }

        Text(SetFormat.num(minV), color = DarkGray, fontSize = 10.sp)

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