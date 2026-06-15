package com.abhijit.docscanpro.ui.screens.scanner

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

// UI design pending — camera preview + controls will be wired here
@Composable
fun ScannerScreen(
    navController: NavHostController,
    viewModel: ScannerViewModel = viewModel()
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Scanner — point camera at document")
    }
}
