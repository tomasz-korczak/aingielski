package pl.aingielski.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import pl.aingielski.R
import pl.aingielski.domain.stats.ChartPoint
import pl.aingielski.ui.theme.LocalResultColors
import kotlin.math.ceil
import kotlin.math.max

/**
 * Line chart of the last sessions (FR-061, research R10): two series (correct / incorrect),
 * integer Y ticks, two-line X labels (date, time), a legend and a spoken summary.
 */
@Composable
fun LineChart(points: List<ChartPoint>, modifier: Modifier = Modifier) {
    if (points.isEmpty()) {
        Box(modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
            Text(stringResource(R.string.stats_no_data), style = MaterialTheme.typography.bodyLarge)
        }
        return
    }
    val resultColors = LocalResultColors.current
    val axisColor = MaterialTheme.colorScheme.outline
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val measurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = labelColor, textAlign = TextAlign.Center)
    val correctLabel = stringResource(R.string.stats_correct)
    val incorrectLabel = stringResource(R.string.stats_incorrect)
    val summary = points.map { p ->
        stringResource(R.string.stats_chart_point_cd, p.label, p.correct, p.incorrect)
    }.joinToString(" ")
    val description = stringResource(R.string.stats_chart_cd, summary)

    Column(
        modifier.clearAndSetSemantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            val maxValue = max(1, points.maxOf { max(it.correct, it.incorrect) })
            val step = max(1, ceil(maxValue / 4.0).toInt())
            val top = step * ceil(maxValue.toDouble() / step).toInt()

            val yLabelWidth = 32.dp.toPx()
            val xLabelHeight = 36.dp.toPx()
            val chartLeft = yLabelWidth
            val chartRight = size.width - 12.dp.toPx()
            val chartTop = 8.dp.toPx()
            val chartBottom = size.height - xLabelHeight
            val chartHeight = chartBottom - chartTop

            fun yFor(value: Int) = chartBottom - chartHeight * value / top
            fun xFor(index: Int) = if (points.size == 1) {
                (chartLeft + chartRight) / 2
            } else {
                chartLeft + (chartRight - chartLeft) * index / (points.size - 1)
            }

            // Y grid and integer ticks.
            var tick = 0
            while (tick <= top) {
                val y = yFor(tick)
                drawLine(gridColor, Offset(chartLeft, y), Offset(chartRight, y), strokeWidth = 1.dp.toPx())
                val layout = measurer.measure(tick.toString(), labelStyle)
                drawText(layout, topLeft = Offset(chartLeft - layout.size.width - 6.dp.toPx(), y - layout.size.height / 2))
                tick += step
            }
            drawLine(axisColor, Offset(chartLeft, chartBottom), Offset(chartRight, chartBottom), strokeWidth = 1.5.dp.toPx())

            // X labels: "dd.MM" over "HH:mm" so ten labels fit on a phone.
            val showEvery = if (points.size > 6 && size.width < 360.dp.toPx()) 2 else 1
            points.forEachIndexed { index, point ->
                if (index % showEvery != 0 && index != points.lastIndex) return@forEachIndexed
                val layout = measurer.measure(point.label.replace(' ', '\n'), labelStyle)
                drawText(layout, topLeft = Offset(xFor(index) - layout.size.width / 2, chartBottom + 4.dp.toPx()))
            }

            fun series(values: List<Int>, color: Color) {
                val path = Path()
                values.forEachIndexed { index, value ->
                    val point = Offset(xFor(index), yFor(value))
                    if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
                }
                drawPath(path, color, style = Stroke(width = 3.dp.toPx()))
                values.forEachIndexed { index, value ->
                    drawCircle(color, radius = 4.dp.toPx(), center = Offset(xFor(index), yFor(value)))
                }
            }
            series(points.map { it.correct }, resultColors.correct)
            series(points.map { it.incorrect }, resultColors.incorrect)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            LegendItem(resultColors.correct, correctLabel)
            LegendItem(resultColors.incorrect, incorrectLabel)
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier
                .size(12.dp)
                .background(color, CircleShape),
        )
        Text(label, style = MaterialTheme.typography.labelLarge)
    }
}
