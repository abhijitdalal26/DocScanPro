package com.abhijit.docscanpro.ui.screens.lock

import android.app.Application
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.abhijit.docscanpro.data.preferences.AppPreferences
import com.abhijit.docscanpro.security.AppLockManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.MessageDigest

data class LockUiState(
    val enteredPin: String = "",
    val lockType: String = "NONE",
    val biometricAvailable: Boolean = false,
    val errorMessage: String? = null
)

class LockViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = AppPreferences(application)
    private val lockManager = AppLockManager(application)

    private val _uiState = MutableStateFlow(LockUiState())
    val uiState: StateFlow<LockUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val lockType = prefs.lockType.first()
            val biometricAvail = lockManager.checkBiometricAvailability() == AppLockManager.BiometricAvailability.AVAILABLE
            _uiState.update { it.copy(lockType = lockType, biometricAvailable = biometricAvail) }
        }
    }

    fun onDigitEntered(digit: String) {
        if (_uiState.value.enteredPin.length >= 4) return
        _uiState.update { it.copy(enteredPin = it.enteredPin + digit, errorMessage = null) }
    }

    fun onDelete() {
        val pin = _uiState.value.enteredPin
        if (pin.isNotEmpty()) _uiState.update { it.copy(enteredPin = pin.dropLast(1)) }
    }

    fun verifyPin(onResult: (Boolean) -> Unit) {
        val pin = _uiState.value.enteredPin
        if (pin.length != 4) {
            _uiState.update { it.copy(errorMessage = "Enter 4 digits") }
            onResult(false)
            return
        }
        viewModelScope.launch {
            val stored = prefs.verifyPin(pin, getApplication()).first()
            if (stored) {
                _uiState.update { it.copy(errorMessage = null, enteredPin = "") }
                onResult(true)
            } else {
                _uiState.update { it.copy(errorMessage = "Incorrect PIN", enteredPin = "") }
                onResult(false)
            }
        }
    }

    fun promptBiometric(activity: FragmentActivity, onSuccess: () -> Unit) {
        lockManager.showBiometricPrompt(
            activity = activity,
            title = "Unlock DocScan Pro",
            onSuccess = {
                AppLockManager.setUnlocked(true)
                onSuccess()
            }
        )
    }
}
