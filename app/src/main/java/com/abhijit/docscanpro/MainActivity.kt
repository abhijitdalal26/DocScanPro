package com.abhijit.docscanpro

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.rememberNavController
import com.abhijit.docscanpro.data.preferences.AppPreferences
import com.abhijit.docscanpro.security.AppLockManager
import com.abhijit.docscanpro.ui.navigation.AppNavGraph
import com.abhijit.docscanpro.ui.navigation.Screen
import com.abhijit.docscanpro.ui.theme.DocScanProTheme
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.opencv.android.OpenCVLoader

class MainActivity : ComponentActivity() {

    private var navController: androidx.navigation.NavController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        PDFBoxResourceLoader.init(applicationContext)
        OpenCVLoader.initLocal()

        enableEdgeToEdge()
        setContent {
            val prefs = AppPreferences(applicationContext)
            val darkThemePref by prefs.darkTheme.collectAsState(initial = "SYSTEM")
            DocScanProTheme(darkThemeOverride = darkThemePref) {
                val nc = rememberNavController()
                navController = nc
                AppNavGraph(
                    navController = nc,
                    startAction = intent?.action
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        checkAppLock()
    }

    private fun checkAppLock() {
        lifecycleScope.launch {
            val prefs = AppPreferences(applicationContext)
            val lockEnabled = prefs.isAppLockEnabled.first()
            val autoLockMinutes = prefs.autoLockTimeoutMinutes.first()
            if (lockEnabled && !AppLockManager.isSessionUnlocked(autoLockMinutes)) {
                navController?.navigate(Screen.Lock.route) {
                    launchSingleTop = true
                }
            }
        }
    }
}
