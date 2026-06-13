package com.example.cardpulse.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.cardpulse.theme.CardPulseTheme
import com.example.cardpulse.theme.Indigo400
import com.example.cardpulse.theme.Indigo500
import java.text.NumberFormat
import java.util.Locale

@Composable
fun BarChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier,
    maxHeight: Dp = 160.dp,
    barWidth: Dp = 32.dp,
    barColor: Color = Indigo500,
    barGradient: List<Color> = listOf(Indigo400, Indigo500),
    animationDuration: Int = 800
) {
    val displayData = data.take(12)
    val maxValue = displayData.maxOfOrNull { it.second }?.coerceAtLeast(1.0) ?: 1.0
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(data) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = animationDuration)
        )
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        displayData.forEach { (label, value) ->
            val barHeightFraction = (value / maxValue).toFloat() * animationProgress.value
            val barHeightDp = maxHeight * barHeightFraction

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(barWidth + 12.dp)
            ) {
                // Value label on top
                Text(
                    text = formatBarValue(value),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                // Bar
                Box(
                    modifier = Modifier
                        .width(barWidth)
                        .height(barHeightDp.coerceAtLeast(2.dp))
                        .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                        .background(
                            brush = Brush.verticalGradient(barGradient)
                        )
                )

                // Category label below
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
        }
    }
}

private fun formatBarValue(amount: Double): String {
    return when {
        amount >= 100_000 -> {
            val lakhs = amount / 100_000
            "₹${String.format(Locale("en", "IN"), "%.1fL", lakhs)}"
        }
        amount >= 1_000 -> {
            val thousands = amount / 1_000
            "₹${String.format(Locale("en", "IN"), "%.1fK", thousands)}"
        }
        else -> {
            val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
            formatter.maximumFractionDigits = 0
            formatter.format(amount)
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun BarChartPreview() {
    CardPulseTheme(darkTheme = true) {
        BarChart(
            data = listOf(
                "Jan" to 15000.0,
                "Feb" to 22000.0,
                "Mar" to 18500.0,
                "Apr" to 31000.0,
                "May" to 27000.0,
                "Jun" to 19500.0
            ),
            modifier = Modifier.padding(16.dp)
        )
    }
}
