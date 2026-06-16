package com.abhijit.docscanpro.ui.screens.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhijit.docscanpro.data.model.ColorMode
import com.abhijit.docscanpro.data.preferences.AppPreferences
import com.abhijit.docscanpro.security.AppLockManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
    val darkTheme: String = "SYSTEM",
    val biometricAvailability: AppLockManager.BiometricAvailability = AppLockManager.BiometricAvailability.UNAVAILABLE
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val lockManager = AppLockManager(application)

    private val _uiState = MutableStateFlow(
        SettingsUiState(biometricAvailability = lockManager.checkBiometricAvailability())
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { prefs.defaultColorMode.collect { v -> _uiState.update { it.copy(defaultColorMode = v) } } }
        viewModelScope.launch { prefs.isAppLockEnabled.collect { v -> _uiState.update { it.copy(isAppLockEnabled = v) } } }
        viewModelScope.launch { prefs.lockType.collect { v -> _uiState.update { it.copy(lockType = v) } } }
        viewModelScope.launch { prefs.autoLockTimeoutMinutes.collect { v -> _uiState.update { it.copy(autoLockMinutes = v) } } }
        viewModelScope.launch { prefs.showOcrAfterScan.collect { v -> _uiState.update { it.copy(showOcrAfterScan = v) } } }
        viewModelScope.launch { prefs.isWatermarkEnabled.collect { v -> _uiState.update { it.copy(isWatermarkEnabled = v) } } }
        viewModelScope.launch { prefs.isWatermarkGpsEnabled.collect { v -> _uiState.update { it.copy(isWatermarkGpsEnabled = v) } } }
        viewModelScope.launch { prefs.isHapticEnabled.collect { v -> _uiState.update { it.copy(isHapticEnabled = v) } } }
        viewModelScope.launch { prefs.isScanSoundEnabled.collect { v -> _uiState.update { it.copy(isScanSoundEnabled = v) } } }
        viewModelScope.launch { prefs.isAdFreePurchased.collect { v -> _uiState.update { it.copy(isAdFreePurchased = v) } } }
        viewModelScope.launch { prefs.darkTheme.collect { v -> _uiState.update { it.copy(darkTheme = v) } } }
    }

    fun setDefaultColorMode(mode: ColorMode) = viewModelScope.launch { prefs.setDefaultColorMode(mode) }
    fun setShowOcrAfterScan(show: Boolean) = viewModelScope.launch { prefs.setShowOcrAfterScan(show) }
    fun setWatermarkEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setWatermarkEnabled(enabled) }
    fun setWatermarkGpsEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setWatermarkGpsEnabled(enabled) }
    fun setHapticEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setHapticEnabled(enabled) }
    fun setScanSoundEnabled(enabled: Boolean) = viewModelScope.launch { prefs.setScanSoundEnabled(enabled) }
    fun setPin(pin: String) = viewModelScope.launch { prefs.setPin(pin) }
    fun clearPin() = viewModelScope.launch { prefs.clearPin() }
    fun setAutoLockTimeout(minutes: Int) = viewModelScope.launch { prefs.setAutoLockTimeout(minutes) }
    fun setDarkTheme(theme: String) = viewModelScope.launch { prefs.setDarkTheme(theme) }
}
