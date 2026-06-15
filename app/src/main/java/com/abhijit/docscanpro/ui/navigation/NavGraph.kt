package com.abhijit.docscanpro.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.abhijit.docscanpro.ui.screens.home.HomeScreen
import com.abhijit.docscanpro.ui.screens.library.LibraryScreen
import com.abhijit.docscanpro.ui.screens.scanner.ScannerScreen
import com.abhijit.docscanpro.ui.screens.settings.SettingsScreen
import com.abhijit.docscanpro.ui.screens.viewer.DocumentViewerScreen

@Composable
fun AppNavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route
    ) {
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
    }
}
