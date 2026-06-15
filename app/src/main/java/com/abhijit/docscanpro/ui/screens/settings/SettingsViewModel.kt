package com.abhijit.docscanpro.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhijit.docscanpro.data.model.ColorMode
import com.abhijit.docscanpro.data.preferences.AppPreferences
import com.abhijit.docscanpro.security.AppLockManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val defaultColorMode: ColorMode = ColorMode.ORIGINAL,
    val isAppLockEnabled: Boolean = false,
    val lockType: String = "NONE",
    val autoLockMinutes: Int = 5,
    val showOcrAfterScan: Boolean = true,
    val isWatermarkEnabled: Boolean = false,
    val isWatermarkGpsEnabled: Boolean = false,
    val isHapticEnabled: Boolean = true,
    val isScanSoundEnabled: Boolean = true,
    val isAdFreePurchased: Boolean = false,
    val biometricAvailability: AppLockManager.BiometricAvailability = AppLockManager.BiometricAvailability.UNAVAILABLE
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val lockManager = AppLockManager(application)

    val uiState: StateFlow<SettingsUiState> = combine(
        prefs.defaultColorMode,
        prefs.isAppLockEnabled,
        prefs.lockType,
        prefs.autoLockTimeoutMinutes,
        prefs.showOcrAfterScan,
        prefs.isWatermarkEnabled,
        prefs.isWatermarkGpsEnabled,
        prefs.isHapticEnabled,
        prefs.isScanSoundEnabled,
        prefs.isAdFreePurchased
    ) { values ->
        SettingsUiState(
            defaultColorMode = values[0] as ColorMode,
            isAppLockEnabled = values[1] as Boolean,
            lockType = values[2] as String,
            autoLockMinutes = values[3] as Int,
            showOcrAfterScan = values[4] as Boolean,
            isWatermarkEnabled = values[5] as Boolean,
            isWatermarkGpsEnabled = values[6] as Boolean,
            isHapticEnabled = values[7] as Boolean,
            isScanSoundEnabled = values[8] as Boolean,
            isAdFreePurchased = values[9] as Boolean,
            biometricAvailability = lockManager.checkBiometricAvailability()
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setDefaultColorMode(mode: ColorMode) = viewModelScope.launch {
        prefs.setDefaultColorMode(mode)
    }

    fun setShowOcrAfterScan(show: Boolean) = viewModelScope.launch {
        prefs.setShowOcrAfterScan(show)
    }

    fun setWatermarkEnabled(enabled: Boolean) = viewModelScope.launch {
        prefs.setWatermarkEnabled(enabled)
    }

    fun setWatermarkGpsEnabled(enabled: Boolean) = viewModelScope.launch {
        prefs.setWatermarkGpsEnabled(enabled)
    }

    fun setHapticEnabled(enabled: Boolean) = viewModelScope.launch {
        prefs.setHapticEnabled(enabled)
    }

    fun setScanSoundEnabled(enabled: Boolean) = viewModelScope.launch {
        prefs.setScanSoundEnabled(enabled)
    }

    fun setPin(pin: String) = viewModelScope.launch {
        prefs.setPin(pin)
    }

    fun clearPin() = viewModelScope.launch {
        prefs.clearPin()
    }

    fun setAutoLockTimeout(minutes: Int) = viewModelScope.launch {
        prefs.setAutoLockTimeout(minutes)
    }
}
