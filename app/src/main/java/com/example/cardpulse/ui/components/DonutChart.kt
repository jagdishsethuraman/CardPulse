package com.example.cardpulse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cardpulse.theme.CardPulseTheme
import com.example.cardpulse.theme.ChartColors
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DonutChart(
    data: List<Pair<String, Double>>,
    colors: List<Color> = ChartColors,
    modifier: Modifier = Modifier,
    size: Dp = 200.dp,
    strokeWidth: Dp = 28.dp,
    animationDuration: Int = 1000,
    showCenterTotal: Boolean = true
) {
    val total = data.sumOf { it.second }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDuration)
        )
    }

    val proportions = data.map { it.second / total.coerceAtLeast(1.0) }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        Canvas(modifier = Modifier.size(size)) {
            val canvasSize = this.size.minDimension
            val strokePx = strokeWidth.toPx()
            val arcSize = canvasSize - strokePx
            val topLeft = Offset(strokePx / 2f, strokePx / 2f)

            var startAngle = -90f

            proportions.take(12).forEachIndexed { index, proportion ->
                val sweepAngle = (proportion * 360f * animationProgress.value).toFloat()
                val color = colors[index % colors.size]

                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = topLeft,
                    size = Size(arcSize, arcSize),
                    style = Stroke(
                        width = strokePx,
                        cap = StrokeCap.Round
                    )
                )
                startAngle += sweepAngle
            }
        }

        if (showCenterTotal) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Total",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = formatCurrency(total),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

internal fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
    formatter.maximumFractionDigits = 0
    return formatter.format(amount)
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun DonutChartPreview() {
    CardPulseTheme(darkTheme = true) {
        DonutChart(
            data = listOf(
                "Food" to 8500.0,
                "Transport" to 3200.0,
                "Shopping" to 12400.0,
                "Bills" to 5600.0,
                "Entertainment" to 2100.0
            )
        )
    }
}
