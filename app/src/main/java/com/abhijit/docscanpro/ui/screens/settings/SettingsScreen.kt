@file:OptIn(ExperimentalMaterial3Api::class)

package com.abhijit.docscanpro.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.abhijit.docscanpro.data.model.ColorMode
import com.abhijit.docscanpro.security.AppLockManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavHostController,
    viewModel: SettingsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showPinDialog by remember { mutableStateOf(false) }
    var pinInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // Scanning section
            SettingsSection("Scanning") {
                SettingsDropdown(
                    title = "Default Color Mode",
                    subtitle = uiState.defaultColorMode.name.replace("_", " ").lowercase().replaceFirstChar { it.uppercase() },
                    icon = Icons.Default.Palette,
                    options = ColorMode.entries.map { it.name to it.name.replace("_", " ") },
                    selectedOption = uiState.defaultColorMode.name,
                    onSelect = { viewModel.setDefaultColorMode(ColorMode.valueOf(it)) }
                )
                SettingsToggle(
                    title = "Show OCR Text After Scan",
                    subtitle = "Automatically show extracted text",
                    icon = Icons.Default.TextFields,
                    checked = uiState.showOcrAfterScan,
                    onToggle = viewModel::setShowOcrAfterScan
                )
                SettingsToggle(
                    title = "Watermark",
                    subtitle = "Stamp date/time on scanned pages",
                    icon = Icons.Default.WaterDrop,
                    checked = uiState.isWatermarkEnabled,
                    onToggle = viewModel::setWatermarkEnabled
                )
                if (uiState.isWatermarkEnabled) {
                    SettingsToggle(
                        title = "Include GPS in Watermark",
                        subtitle = "Add location coordinates (needs permission)",
                        icon = Icons.Default.LocationOn,
                        checked = uiState.isWatermarkGpsEnabled,
                        onToggle = viewModel::setWatermarkGpsEnabled
                    )
                }
            }

            // Security section
            SettingsSection("Security") {
                when (uiState.biometricAvailability) {
                    AppLockManager.BiometricAvailability.AVAILABLE -> {
                        SettingsToggle(
                            title = "App Lock",
                            subtitle = "Lock app with biometric",
                            icon = Icons.Default.Fingerprint,
                            checked = uiState.isAppLockEnabled,
                            onToggle = { viewModel.setShowOcrAfterScan(it) } // TODO wire to lock toggle
                        )
                    }
                    AppLockManager.BiometricAvailability.NOT_ENROLLED -> {
                        SettingsItem(
                            title = "Biometric Not Enrolled",
                            subtitle = "Enroll fingerprint in device settings",
                            icon = Icons.Default.Fingerprint,
                            onClick = {}
                        )
                    }
                    else -> { /* no hardware — show PIN option */ }
                }
                SettingsItem(
                    title = if (uiState.lockType == "PIN") "Change PIN" else "Set PIN",
                    subtitle = if (uiState.lockType == "PIN") "PIN lock is enabled" else "Add 4-digit PIN lock",
                    icon = Icons.Default.Pin,
                    onClick = { showPinDialog = true }
                )
                if (uiState.lockType == "PIN") {
                    SettingsItem(
                        title = "Remove PIN",
                        subtitle = "Disable PIN lock",
                        icon = Icons.Default.LockOpen,
                        onClick = viewModel::clearPin
                    )
                }
            }

            // Sound & Haptic section
            SettingsSection("Feedback") {
                SettingsToggle(
                    title = "Haptic Feedback",
                    subtitle = "Vibrate on actions",
                    icon = Icons.Default.Vibration,
                    checked = uiState.isHapticEnabled,
                    onToggle = viewModel::setHapticEnabled
                )
                SettingsToggle(
                    title = "Scan Sound",
                    subtitle = "Play shutter sound when scanning",
                    icon = Icons.Default.VolumeUp,
                    checked = uiState.isScanSoundEnabled,
                    onToggle = viewModel::setScanSoundEnabled
                )
            }

            // Storage section
            SettingsSection("Storage") {
                SettingsItem(
                    title = "Recycle Bin",
                    subtitle = "View and restore deleted documents",
                    icon = Icons.Default.Delete,
                    onClick = { /* TODO navigate to recycle bin */ }
                )
                SettingsItem(
                    title = "Clear Cache",
                    subtitle = "Remove temporary files",
                    icon = Icons.Default.CleaningServices,
                    onClick = { /* TODO */ }
                )
            }

            // About section
            SettingsSection("About") {
                SettingsItem(
                    title = "Remove Ads",
                    subtitle = if (uiState.isAdFreePurchased) "Ad-Free activated" else "₹199 · one-time purchase",
                    icon = if (uiState.isAdFreePurchased) Icons.Default.CheckCircle else Icons.Default.Star,
                    onClick = { /* TODO trigger IAP */ }
                )
                SettingsItem(
                    title = "Version",
                    subtitle = "1.0.0 — DocScan Pro",
                    icon = Icons.Default.Info,
                    onClick = {}
                )
            }

            Spacer(Modifier.height(16.dp))
        }
    }

    // PIN dialog
    if (showPinDialog) {
        AlertDialog(
            onDismissRequest = { showPinDialog = false },
            title = { Text("Set PIN") },
            text = {
                OutlinedTextField(
                    value = pinInput,
                    onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) pinInput = it },
                    label = { Text("4-digit PIN") },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.NumberPassword
                    ),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    if (pinInput.length == 4) {
                        viewModel.setPin(pinInput)
                        showPinDialog = false
                        pinInput = ""
                    }
                }) { Text("Set PIN") }
            },
            dismissButton = {
                TextButton(onClick = { showPinDialog = false; pinInput = "" }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Text(
        title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
    Card(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column(content = content)
    }
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    icon: ImageVector,
    checked: Boolean,
    onToggle: (Boolean) -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, fontSize = 12.sp) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary) },
        trailingContent = { Switch(checked = checked, onCheckedChange = onToggle) }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsItem(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, fontSize = 12.sp) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary) },
        trailingContent = { Icon(Icons.Default.ChevronRight, null) },
        modifier = Modifier.clickable(onClick = onClick)
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}

@Composable
private fun SettingsDropdown(
    title: String,
    subtitle: String,
    icon: ImageVector,
    options: List<Pair<String, String>>,
    selectedOption: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = { Text(subtitle, fontSize = 12.sp) },
        leadingContent = { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary) },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(subtitle, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Icon(Icons.Default.ArrowDropDown, null)
                DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                    options.forEach { (value, label) ->
                        DropdownMenuItem(
                            text = { Text(label) },
                            onClick = { onSelect(value); expanded = false },
                            leadingIcon = { if (value == selectedOption) Icon(Icons.Default.Check, null) }
                        )
                    }
                }
            }
        },
        modifier = Modifier.clickable { expanded = true }
    )
    HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
}
