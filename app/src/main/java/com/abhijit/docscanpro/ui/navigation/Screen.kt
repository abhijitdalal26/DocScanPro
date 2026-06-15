package com.abhijit.docscanpro.ui.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Scanner : Screen("scanner")
    object Library : Screen("library")
    object Settings : Screen("settings")

    object DocumentViewer : Screen("document_viewer/{documentId}") {
        fun createRoute(documentId: Long) = "document_viewer/$documentId"
    }

    object ScanResult : Screen("scan_result/{documentId}") {
        fun createRoute(documentId: Long) = "scan_result/$documentId"
    }
}
