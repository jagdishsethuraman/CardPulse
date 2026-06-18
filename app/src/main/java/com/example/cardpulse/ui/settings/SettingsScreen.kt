package com.example.cardpulse.ui.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onWiped: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val userName by viewModel.userName.collectAsState()
    val currency by viewModel.currency.collectAsState()
    val billingDay by viewModel.defaultBillingCycleDay.collectAsState()
    val darkMode by viewModel.darkMode.collectAsState()
    val statementCount by viewModel.statementCount.collectAsState()
    val categories by viewModel.categories.collectAsState()

    var isEditNameOpen by remember { mutableStateOf(false) }
    var editNameInput by remember { mutableStateOf("") }
    
    var isBillingDayDropdownExpanded by remember { mutableStateOf(false) }
    var isImportDialogOpen by remember { mutableStateOf(false) }
    var importText by remember { mutableStateOf("") }

    var isWipeConfirmOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Settings", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Spacer to avoid top bar
            item {
                Spacer(modifier = Modifier.height(paddingValues.calculateTopPadding()))
            }

            // 1. Profile Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Profile Settings",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        // Name Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    editNameInput = userName
                                    isEditNameOpen = true
                                },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "User Name", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = userName.ifEmpty { "Not Set" }, style = MaterialTheme.typography.bodyLarge)
                            }
                            Text(text = "Edit", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                        }

                        // Currency display (Read-Only)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(text = "Default Currency", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(text = "INR (₹)", style = MaterialTheme.typography.bodyLarge)
                            }
                        }

                        // Default Billing Cycle Day
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(text = "Default Billing Day", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            
                            ExposedDropdownMenuBox(
                                expanded = isBillingDayDropdownExpanded,
                                onExpandedChange = { isBillingDayDropdownExpanded = !isBillingDayDropdownExpanded }
                            ) {
                                OutlinedTextField(
                                    value = "Day $billingDay",
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isBillingDayDropdownExpanded) },
                                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                ExposedDropdownMenu(
                                    expanded = isBillingDayDropdownExpanded,
                                    onDismissRequest = { isBillingDayDropdownExpanded = false }
                                ) {
                                    for (day in 1..28) {
                                        DropdownMenuItem(
                                            text = { Text(text = "Day $day") },
                                            onClick = {
                                                viewModel.updateBillingCycleDay(day)
                                                isBillingDayDropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Appearance Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Text(
                            text = "Choose app theme:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("dark" to "Dark", "light" to "Light", "system" to "System").forEach { (key, label) ->
                                val isSelected = darkMode == key
                                InputChip(
                                    selected = isSelected,
                                    onClick = { viewModel.updateDarkMode(key) },
                                    label = { Text(text = label) },
                                    modifier = Modifier.weight(1f),
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                )
                            }
                        }
                    }
                }
            }

            // 3. Data Backup Management Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Backup & Data",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Statements imported", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "$statementCount", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Export Data
                            Button(
                                onClick = {
                                    viewModel.exportBackup { json ->
                                        if (json.isNotEmpty()) {
                                            clipboardManager.setText(AnnotatedString(json))
                                            Toast.makeText(context, "Backup copied to clipboard!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Toast.makeText(context, "Export failed. No data to back up.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Export JSON")
                            }

                            // Import Data
                            OutlinedButton(
                                onClick = { isImportDialogOpen = true },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Restore JSON")
                            }
                        }
                    }
                }
            }

            // 4. Categories summary
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Spend Categories",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Parsed transactions are auto-grouped using these keywords:",
                            style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            categories.take(5).forEach { cat ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(text = cat.emoji, fontSize = 18.sp)
                                    Column {
                                        Text(text = cat.name, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold))
                                        Text(
                                            text = if (cat.keywords.isEmpty()) "Miscellaneous fallback" else cat.keywords.replace(",", ", "),
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                            if (categories.size > 5) {
                                Text(
                                    text = "+ ${categories.size - 5} more categories",
                                    style = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.primary),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 5. Danger Zone Section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Danger Zone",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                        )
                        Text(
                            text = "Wiping data deletes all credit cards, statements, transactions, and profile configurations. This action is irreversible.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Button(
                            onClick = { isWipeConfirmOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Wipe All Data", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                }
            }

            // 6. Footer Privacy promise
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "CardPulse v1.0.0",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "100% Local • Zero Internet • Your Data, Your Device",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }

        // Dialog: Edit Name
        if (isEditNameOpen) {
            AlertDialog(
                onDismissRequest = { isEditNameOpen = false },
                title = { Text("Edit User Name") },
                text = {
                    OutlinedTextField(
                        value = editNameInput,
                        onValueChange = { editNameInput = it },
                        placeholder = { Text("Enter your name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.updateUserName(editNameInput)
                            isEditNameOpen = false
                        },
                        enabled = editNameInput.trim().isNotEmpty()
                    ) {
                        Text("Save")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isEditNameOpen = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Dialog: Restore Backup
        if (isImportDialogOpen) {
            Dialog(onDismissRequest = { isImportDialogOpen = false }) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Restore Backup",
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "Paste the exported JSON backup text below. This will wipe all existing local data and replace it with the backup content.",
                            style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
                        )

                        OutlinedTextField(
                            value = importText,
                            onValueChange = { importText = it },
                            placeholder = { Text("Paste JSON here...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { isImportDialogOpen = false }) {
                                Text("Cancel")
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Button(
                                onClick = {
                                    viewModel.importBackup(importText) { success, msg ->
                                        Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                                        if (success) {
                                            isImportDialogOpen = false
                                            importText = ""
                                        }
                                    }
                                },
                                enabled = importText.isNotEmpty()
                            ) {
                                Text("Restore")
                            }
                        }
                    }
                }
            }
        }

        // Dialog: Wipe All Data Confirmation
        if (isWipeConfirmOpen) {
            AlertDialog(
                onDismissRequest = { isWipeConfirmOpen = false },
                title = { Text("Wipe All Local Data?", color = MaterialTheme.colorScheme.error) },
                text = {
                    Text("This will delete all credit cards, statements, transactions, and settings. This cannot be undone. Are you absolutely sure?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.wipeAllData {
                                isWipeConfirmOpen = false
                                onWiped() // Force navigate back to onboarding
                            }
                        }
                    ) {
                        Text("Wipe Everything", color = MaterialTheme.colorScheme.error)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { isWipeConfirmOpen = false }) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}
