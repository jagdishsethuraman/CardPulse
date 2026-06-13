package com.example.cardpulse.ui.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCardSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    onAddCard: (cardName: String, bankName: String, lastFourDigits: String, colorHex: String, billingDay: Int, limit: Double?) -> Unit
) {
    var cardName by remember { mutableStateOf("") }
    var selectedBank by remember { mutableStateOf("SBI") }
    var lastFourDigits by remember { mutableStateOf("") }
    var selectedColorIndex by remember { mutableIntStateOf(0) }
    var billingDay by remember { mutableIntStateOf(15) }
    var creditLimitStr by remember { mutableStateOf("") }

    val colors = listOf(
        "#667EEA", // Indigo-Purple
        "#11998E", // Teal-Green
        "#F5576C", // Orange-Pink
        "#2C3E50", // Dark Slate
        "#A855F7", // Purple
        "#FF9F1C"  // Amber
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Add New Credit Card",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )

            // Bank Name Selection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Bank Name",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold)
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("SBI", "HDFC", "FEDERAL", "OTHER").forEach { bank ->
                        val isSelected = selectedBank == bank
                        InputChip(
                            selected = isSelected,
                            onClick = { selectedBank = bank },
                            label = { Text(text = bank) },
                            colors = InputChipDefaults.inputChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            )
                        )
                    }
                }
            }

            // Card Nickname
            OutlinedTextField(
                value = cardName,
                onValueChange = { cardName = it },
                label = { Text(text = "Card Nickname (e.g. SimplyClick, Millennia)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Last 4 Digits
                OutlinedTextField(
                    value = lastFourDigits,
                    onValueChange = { if (it.length <= 4) lastFourDigits = it },
                    label = { Text(text = "Last 4 Digits") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                )

                // Billing Day
                var isDayDropdownExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = isDayDropdownExpanded,
                    onExpandedChange = { isDayDropdownExpanded = !isDayDropdownExpanded },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = "Day $billingDay",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(text = "Billing Day") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDayDropdownExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = isDayDropdownExpanded,
                        onDismissRequest = { isDayDropdownExpanded = false }
                    ) {
                        for (day in 1..28) {
                            DropdownMenuItem(
                                text = { Text(text = "Day $day") },
                                onClick = {
                                    billingDay = day
                                    isDayDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Credit Limit
                OutlinedTextField(
                    value = creditLimitStr,
                    onValueChange = { creditLimitStr = it },
                    label = { Text(text = "Limit (Optional)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1.2f),
                    shape = RoundedCornerShape(12.dp)
                )

                // Color Picker
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = "Card Color", style = MaterialTheme.typography.labelSmall)
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        colors.forEachIndexed { index, hex ->
                            val isSelected = selectedColorIndex == index
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .clickable { selectedColorIndex = index }
                                    .padding(2.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(12.dp)
                                            .clip(CircleShape)
                                            .background(Color.White)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Add Card Button
            Button(
                onClick = {
                    val limit = creditLimitStr.toDoubleOrNull()
                    onAddCard(cardName, selectedBank, lastFourDigits, colors[selectedColorIndex], billingDay, limit)
                    onDismiss()
                },
                enabled = cardName.trim().isNotEmpty() && lastFourDigits.length == 4,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(text = "Add Card", style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
