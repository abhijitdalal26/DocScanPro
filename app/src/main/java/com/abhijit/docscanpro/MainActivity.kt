package com.abhijit.docscanpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.compose.rememberNavController
import com.abhijit.docscanpro.ui.navigation.AppNavGraph
import com.abhijit.docscanpro.ui.theme.DocScanProTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen BEFORE super.onCreate()
        installSplashScreen()
        super.onCreate(savedInstanceState)

        // Initialize libraries that need a context
        PDFBoxResourceLoader.init(applicationContext)
        OpenCVLoader.initLocal()

        enableEdgeToEdge()
        setContent {
            DocScanProTheme {
                val navController = rememberNavController()
                AppNavGraph(
                    navController = navController,
                    startAction = intent?.action
                )
            }
        }
    }
}
