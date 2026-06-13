package com.example.cardpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardpulse.theme.CardGradientBlue
import com.example.cardpulse.theme.CardGradientDark
import com.example.cardpulse.theme.CardGradientGreen
import com.example.cardpulse.theme.CardGradientOrange
import com.example.cardpulse.theme.CardPulseTheme

@Composable
fun CreditCardWidget(
    cardName: String,
    bankName: String,
    lastFourDigits: String,
    colorHex: String,
    modifier: Modifier = Modifier
) {
    val gradientColors = getGradientForHex(colorHex)

    Card(
        modifier = modifier
            .width(340.dp)
            .height(210.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(gradientColors)
                )
                .padding(24.dp)
        ) {
            // Bank name - top left
            Text(
                text = bankName.uppercase(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 1.5.sp,
                modifier = Modifier.align(Alignment.TopStart)
            )

            // Contactless icon - top right
            Text(
                text = "📶",
                fontSize = 22.sp,
                modifier = Modifier.align(Alignment.TopEnd)
            )

            // Card number dots - center
            Row(
                modifier = Modifier.align(Alignment.Center),
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(3) {
                    Text(
                        text = "••••",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                }
                Text(
                    text = lastFourDigits,
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            // Bottom row: card name (left), last four (right)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "CARD HOLDER",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = cardName.uppercase(),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "LAST FOUR",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.6f),
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "•••• $lastFourDigits",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                }
            }
        }
    }
}

private fun getGradientForHex(hex: String): List<Color> {
    return try {
        val cleanHex = hex.removePrefix("#")
        val baseColor = Color(("FF$cleanHex").toLong(16))
        val hslShifted = baseColor.copy(
            red = (baseColor.red * 0.7f).coerceIn(0f, 1f),
            green = (baseColor.green * 0.8f).coerceIn(0f, 1f),
            blue = (baseColor.blue * 1.2f).coerceIn(0f, 1f)
        )
        listOf(baseColor, hslShifted)
    } catch (_: Exception) {
        // Fallback to predefined gradients based on first char
        when (hex.firstOrNull()?.lowercaseChar()) {
            'b', '6', '7' -> CardGradientBlue
            'g', '1', '3' -> CardGradientGreen
            'o', 'f', 'e' -> CardGradientOrange
            else -> CardGradientDark
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun CreditCardWidgetPreview() {
    CardPulseTheme(darkTheme = true) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            CreditCardWidget(
                cardName = "Ragavi J",
                bankName = "HDFC Bank",
                lastFourDigits = "4532",
                colorHex = "#667EEA"
            )
            CreditCardWidget(
                cardName = "Ragavi J",
                bankName = "ICICI Bank",
                lastFourDigits = "8891",
                colorHex = "#11998E"
            )
        }
    }
}
