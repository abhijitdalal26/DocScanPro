@file:OptIn(ExperimentalMaterial3Api::class)

package com.abhijit.docscanpro.ui.screens.lock

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.abhijit.docscanpro.security.AppLockManager

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    viewModel: LockViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // Auto-prompt biometric on entry
    LaunchedEffect(Unit) {
        if (uiState.lockType == "BIOMETRIC" || uiState.biometricAvailable) {
            viewModel.promptBiometric(
                activity = context as? androidx.fragment.app.FragmentActivity ?: return@LaunchedEffect,
                onSuccess = onUnlocked
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                Icons.Default.Lock,
                null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                "DocScan Pro",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                if (uiState.errorMessage != null) uiState.errorMessage!! else "Enter your PIN",
                color = if (uiState.errorMessage != null) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 14.sp
            )

            // PIN dot display
            PinDots(enteredLength = uiState.enteredPin.length, maxLength = 4)

            // Number pad
            NumberPad(
                onDigit = viewModel::onDigitEntered,
                onDelete = viewModel::onDelete,
                onBiometric = if (uiState.biometricAvailable) {
                    {
                        viewModel.promptBiometric(
                            activity = context as? androidx.fragment.app.FragmentActivity ?: return@NumberPad,
                            onSuccess = onUnlocked
                        )
                    }
                } else null,
                onSubmit = {
                    viewModel.verifyPin { success ->
                        if (success) {
                            AppLockManager.setUnlocked(true)
                            onUnlocked()
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun PinDots(enteredLength: Int, maxLength: Int = 4) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        repeat(maxLength) { index ->
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(
                        if (index < enteredLength) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant
                    )
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
            )
        }
    }
}

@Composable
private fun NumberPad(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onSubmit: () -> Unit,
    onBiometric: (() -> Unit)?
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("bio", "0", "del")
    )

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                row.forEach { key ->
                    when (key) {
                        "bio" -> {
                            if (onBiometric != null) {
                                NumPadButton(
                                    content = { Icon(Icons.Default.Fingerprint, "Biometric", modifier = Modifier.size(28.dp)) },
                                    onClick = onBiometric
                                )
                            } else {
                                Spacer(Modifier.size(72.dp))
                            }
                        }
                        "del" -> NumPadButton(
                            content = { Icon(Icons.Default.Backspace, "Delete", modifier = Modifier.size(24.dp)) },
                            onClick = onDelete
                        )
                        else -> NumPadButton(
                            content = { Text(key, fontSize = 24.sp, fontWeight = FontWeight.Medium) },
                            onClick = { onDigit(key) }
                        )
                    }
                }
            }
        }
        // Submit button
        Button(
            onClick = onSubmit,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Unlock", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun NumPadButton(content: @Composable () -> Unit, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
