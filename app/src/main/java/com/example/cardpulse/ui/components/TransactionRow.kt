package com.example.cardpulse.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardpulse.theme.CardPulseTheme
import com.example.cardpulse.theme.Emerald500
import com.example.cardpulse.theme.Red400
import java.text.NumberFormat
import java.util.Locale

@Composable
fun TransactionRow(
    emoji: String,
    categoryName: String,
    description: String,
    amount: Double,
    isDebit: Boolean,
    formattedDate: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Emoji in colored circle
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    )
            ) {
                Text(
                    text = emoji,
                    fontSize = 20.sp
                )
            }

            // Description and category
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = categoryName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Amount and date
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val amountColor = if (isDebit) Red400 else Emerald500
                val prefix = if (isDebit) "- " else "+ "
                val formatter = NumberFormat.getCurrencyInstance(Locale("en", "IN"))
                formatter.maximumFractionDigits = 0

                Text(
                    text = "$prefix${formatter.format(amount)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0E17)
@Composable
private fun TransactionRowPreview() {
    CardPulseTheme(darkTheme = true) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(16.dp)
        ) {
            TransactionRow(
                emoji = "🍕",
                categoryName = "Food & Dining",
                description = "Swiggy Order",
                amount = 450.0,
                isDebit = true,
                formattedDate = "13 Jun 2026"
            )
            TransactionRow(
                emoji = "💰",
                categoryName = "Refund",
                description = "Amazon Refund",
                amount = 1299.0,
                isDebit = false,
                formattedDate = "12 Jun 2026"
            )
            TransactionRow(
                emoji = "🛒",
                categoryName = "Shopping",
                description = "Flipkart - Electronics",
                amount = 15999.0,
                isDebit = true,
                formattedDate = "11 Jun 2026"
            )
        }
    }
}
