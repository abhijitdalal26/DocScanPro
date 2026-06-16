package com.abhijit.docscanpro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.abhijit.docscanpro.data.preferences.AppPreferences
import com.abhijit.docscanpro.ui.screens.home.HomeScreen
import com.abhijit.docscanpro.ui.screens.library.LibraryScreen
import com.abhijit.docscanpro.ui.screens.library.RecycleBinScreen
import com.abhijit.docscanpro.ui.screens.lock.LockScreen
import com.abhijit.docscanpro.ui.screens.onboarding.OnboardingScreen
import com.abhijit.docscanpro.ui.screens.scanner.BarcodeScannerScreen
import com.abhijit.docscanpro.ui.screens.scanner.ImagesToPdfScreen
import com.abhijit.docscanpro.ui.screens.scanner.ScannerScreen
import com.abhijit.docscanpro.ui.screens.settings.SettingsScreen
import com.abhijit.docscanpro.ui.screens.viewer.DocumentViewerScreen
import com.abhijit.docscanpro.ui.screens.viewer.PdfToolsScreen
import kotlinx.coroutines.flow.first

@Composable
fun AppNavGraph(
    navController: NavHostController,
    startAction: String? = null
) {
    val context = LocalContext.current

    // Show onboarding on first launch
    LaunchedEffect(Unit) {
        val prefs = AppPreferences(context)
        if (prefs.isFirstLaunch.first()) {
            navController.navigate(Screen.Onboarding.route) {
                popUpTo(Screen.Home.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    // Deep-link from widget / QS tile / shortcut — go directly to scanner
    LaunchedEffect(startAction) {
        when (startAction) {
            "com.abhijit.docscanpro.ACTION_SCAN",
            "com.abhijit.docscanpro.ACTION_SCAN_QR" -> {
                navController.navigate(Screen.Scanner.route) {
                    launchSingleTop = true
                }
            }
        }
    }

    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
        composable(Screen.Onboarding.route) {
            OnboardingScreen(onFinished = {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Home.route) {
            HomeScreen(navController = navController)
        }

        composable(Screen.Scanner.route) {
            ScannerScreen(navController = navController)
        }

        composable(Screen.Library.route) {
            LibraryScreen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        composable(
            route = Screen.DocumentViewer.route,
            arguments = listOf(
                navArgument("documentId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: return@composable
            DocumentViewerScreen(documentId = documentId, navController = navController)
        }

        composable(
            route = Screen.ScanResult.route,
            arguments = listOf(
                navArgument("documentId") { type = NavType.LongType }
            )
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: return@composable
            DocumentViewerScreen(documentId = documentId, navController = navController)
        }

        composable(Screen.RecycleBin.route) {
            RecycleBinScreen(navController = navController)
        }

        composable(Screen.Lock.route) {
            LockScreen(onUnlocked = { navController.popBackStack() })
        }

        composable(Screen.BarcodeScanner.route) {
            BarcodeScannerScreen(navController = navController)
        }

        composable(
            route = Screen.PdfTools.route,
            arguments = listOf(navArgument("documentId") { type = NavType.LongType })
        ) { backStackEntry ->
            val documentId = backStackEntry.arguments?.getLong("documentId") ?: return@composable
            PdfToolsScreen(documentId = documentId, navController = navController)
        }

        composable(Screen.ImagesToPdf.route) {
            ImagesToPdfScreen(navController = navController)
        }
    }
}
