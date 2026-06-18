package com.example.cardpulse.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.material.icons.rounded.BarChart
import java.util.Date
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.cardpulse.data.db.CreditCard
import com.example.cardpulse.ui.components.BillingPeriodSelector
import com.example.cardpulse.ui.components.DonutChart
import com.example.cardpulse.ui.components.TransactionRow
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    userName: String,
    onViewAllTransactions: () -> Unit,
    onNavigateToImport: () -> Unit,
    modifier: Modifier = Modifier
) {
    val creditCards by viewModel.creditCards.collectAsState()
    val selectedCardId by viewModel.selectedCardId.collectAsState()
    val availablePeriods by viewModel.availablePeriods.collectAsState()
    val selectedPeriodIndex by viewModel.selectedPeriodIndex.collectAsState()
    val currentPeriod by viewModel.currentPeriod.collectAsState()
    val categorySpends by viewModel.categorySpends.collectAsState()
    val periodTransactions by viewModel.periodTransactions.collectAsState()
    val topMerchants by viewModel.topMerchants.collectAsState()
    val totalLimit by viewModel.totalLimit.collectAsState()

    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale("en", "IN")) }
    val dateFormatter = remember { SimpleDateFormat("dd MMM yyyy", Locale.ENGLISH) }

    val scrollState = rememberScrollState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (userName.isNotEmpty()) userName.take(1).uppercase() else "U",
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                fontSize = 14.sp
                            )
                        }
                        Column {
                            Text(
                                text = "Hello, ${userName.ifEmpty { "Guest" }}!",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Text(
                                text = "Your spending at a glance",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToImport) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Import PDF Statement")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Horizontal Card Filter Row
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                contentPadding = PaddingValues(horizontal = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // "All Cards" Chip
                item {
                    val isSelected = selectedCardId == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { viewModel.selectCard(null) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "All Cards",
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Individual Cards Chips
                items(creditCards) { card ->
                    val isSelected = selectedCardId == card.id
                    val cardColor = remember(card.colorHex) {
                        try {
                            Color(android.graphics.Color.parseColor(card.colorHex))
                        } catch (e: Exception) {
                            Color.Gray
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .border(
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (isSelected) cardColor else MaterialTheme.colorScheme.outlineVariant
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            .background(
                                if (isSelected) cardColor.copy(alpha = 0.15f)
                                else MaterialTheme.colorScheme.surface
                            )
                            .clickable { viewModel.selectCard(card.id) }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${card.bankName} • ${card.cardName} (${card.lastFourDigits})",
                            color = if (isSelected) cardColor else MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }

            // Period Selector
            if (currentPeriod != null) {
                BillingPeriodSelector(
                    periodLabel = currentPeriod!!.label,
                    onPrevious = { viewModel.selectPreviousPeriod() },
                    onNext = { viewModel.selectNextPeriod() },
                    isPreviousEnabled = selectedPeriodIndex < availablePeriods.size - 1,
                    isNextEnabled = selectedPeriodIndex > 0,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
                )
            }

            // Main Scrollable Dashboard Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                val totalSpent = categorySpends.sumOf { it.totalAmount }

                // Summary Spend Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Spent",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currencyFormatter.format(totalSpent),
                                    style = MaterialTheme.typography.displayLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    )
                                )
                            }

                            // Optional Import CTA when empty
                            if (totalSpent == 0.0) {
                                Button(
                                    onClick = onNavigateToImport,
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Text("Upload PDF")
                                }
                            }
                        }

                        // Progress bar for credit limits
                        totalLimit?.let { limit ->
                            if (limit > 0) {
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val progress = (totalSpent / limit).coerceIn(0.0, 1.0).toFloat()
                                    val limitColor = if (progress > 0.8f) MaterialTheme.colorScheme.error 
                                                     else MaterialTheme.colorScheme.primary

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${(progress * 100).toInt()}% of credit limit used",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "Limit: ${currencyFormatter.format(limit)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }

                                    LinearProgressIndicator(
                                        progress = { progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(8.dp)
                                            .clip(CircleShape),
                                        color = limitColor,
                                        trackColor = MaterialTheme.colorScheme.outlineVariant
                                    )
                                }
                            }
                        }
                    }
                }

                if (totalSpent > 0.0) {
                    // Charts Section (Row with Donut and Category legend)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left: Donut Chart
                        val donutData = categorySpends.map { it.categoryName to it.totalAmount }
                        val colorsList = categorySpends.map { Color(android.graphics.Color.parseColor(it.colorHex)) }
                        
                        Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                            DonutChart(
                                data = donutData,
                                colors = colorsList,
                                size = 160.dp,
                                strokeWidth = 22.dp,
                                showCenterTotal = false
                            )
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Right: Legend
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            categorySpends.take(4).forEach { cat ->
                                val pct = ((cat.totalAmount / totalSpent) * 100).toInt()
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color(android.graphics.Color.parseColor(cat.colorHex)))
                                    )
                                    Column {
                                        Text(
                                            text = "${cat.emoji} ${cat.categoryName}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$pct% • ${currencyFormatter.format(cat.totalAmount)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Top Merchants Section
                    if (topMerchants.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "Top Merchants",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                            )
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    topMerchants.forEachIndexed { idx, merchant ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "${idx + 1}.",
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.primary
                                                )
                                                Text(
                                                    text = merchant.merchantName,
                                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                                                )
                                            }
                                            Text(
                                                text = currencyFormatter.format(merchant.totalAmount),
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Recent Transactions Section
                    if (periodTransactions.isNotEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Recent Spends",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                                )
                                TextButton(onClick = onViewAllTransactions) {
                                    Text("View All")
                                }
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                periodTransactions.take(3).forEach { tx ->
                                    val formattedDate = dateFormatter.format(Date(tx.transaction.transactionDate))
                                    TransactionRow(
                                        emoji = tx.category?.emoji ?: "📦",
                                        categoryName = tx.category?.name ?: "Miscellaneous",
                                        description = tx.transaction.merchantName ?: tx.transaction.description,
                                        amount = tx.transaction.amount,
                                        isDebit = tx.transaction.isDebit,
                                        formattedDate = formattedDate
                                    )
                                }
                            }
                        }
                    }

                } else {
                    // Empty Period State
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BarChart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = "No Spending Data Available",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Import credit card statements covering this period to display analytical breakdowns.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
