package com.abhijit.docscanpro.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.abhijit.docscanpro.data.model.ColorMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.security.MessageDigest

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "docscan_pro_prefs")

class AppPreferences(private val context: Context) {

    companion object {
        val DEFAULT_COLOR_MODE = stringPreferencesKey("default_color_mode")
        val APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        val PIN_HASH = stringPreferencesKey("pin_hash")
        val LOCK_TYPE = stringPreferencesKey("lock_type")  // "PIN" | "BIOMETRIC" | "NONE"
        val AUTO_LOCK_TIMEOUT_MIN = intPreferencesKey("auto_lock_timeout_minutes")
        val DEFAULT_EXPORT_FORMAT = stringPreferencesKey("default_export_format")
        val SHOW_OCR_AFTER_SCAN = booleanPreferencesKey("show_ocr_after_scan")
        val AD_FREE_PURCHASED = booleanPreferencesKey("ad_free_purchased")
        val WATERMARK_ENABLED = booleanPreferencesKey("watermark_enabled")
        val WATERMARK_GPS_ENABLED = booleanPreferencesKey("watermark_gps_enabled")
        val DEFAULT_PDF_QUALITY = stringPreferencesKey("default_pdf_quality")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val SCAN_SOUND = booleanPreferencesKey("scan_sound")
        val FIRST_LAUNCH = booleanPreferencesKey("first_launch")
    }

    private val dataStore = context.dataStore

    // ─── Reads ────────────────────────────────────────────────────────────────

    val defaultColorMode: Flow<ColorMode> = dataStore.data
        .catchIo()
        .map { prefs -> ColorMode.valueOf(prefs[DEFAULT_COLOR_MODE] ?: ColorMode.ORIGINAL.name) }

    val isAppLockEnabled: Flow<Boolean> = dataStore.data
        .catchIo()
        .map { prefs -> prefs[APP_LOCK_ENABLED] ?: false }

    val lockType: Flow<String> = dataStore.data
        .catchIo()
        .map { prefs -> prefs[LOCK_TYPE] ?: "NONE" }

    val autoLockTimeoutMinutes: Flow<Int> = dataStore.data
        .catchIo()
        .map { prefs -> prefs[AUTO_LOCK_TIMEOUT_MIN] ?: 5 }

    val isAdFreePurchased: Flow<Boolean> = dataStore.data
        .catchIo()
        .map { prefs -> prefs[AD_FREE_PURCHASED] ?: false }

    val showOcrAfterScan: Flow<Boolean> = dataStore.data
        .catchIo()
        .map { prefs -> prefs[SHOW_OCR_AFTER_SCAN] ?: true }

    val isWatermarkEnabled: Flow<Boolean> = dataStore.data
        .catchIo()
        .map { prefs -> prefs[WATERMARK_ENABLED] ?: false }

    val isWatermarkGpsEnabled: Flow<Boolean> = dataStore.data
        .catchIo()
        .map { prefs -> prefs[WATERMARK_GPS_ENABLED] ?: false }

    val isHapticEnabled: Flow<Boolean> = dataStore.data
        .catchIo()
        .map { prefs -> prefs[HAPTIC_FEEDBACK] ?: true }

    val isScanSoundEnabled: Flow<Boolean> = dataStore.data
        .catchIo()
        .map { prefs -> prefs[SCAN_SOUND] ?: true }

    val isFirstLaunch: Flow<Boolean> = dataStore.data
        .catchIo()
        .map { prefs -> prefs[FIRST_LAUNCH] ?: true }

    // ─── Writes ───────────────────────────────────────────────────────────────

    suspend fun setDefaultColorMode(mode: ColorMode) {
        dataStore.edit { it[DEFAULT_COLOR_MODE] = mode.name }
    }

    suspend fun setAppLockEnabled(enabled: Boolean) {
        dataStore.edit { it[APP_LOCK_ENABLED] = enabled }
    }

    suspend fun setPin(pin: String) {
        dataStore.edit {
            it[PIN_HASH] = hashPin(pin)
            it[LOCK_TYPE] = "PIN"
            it[APP_LOCK_ENABLED] = true
        }
    }

    suspend fun clearPin() {
        dataStore.edit {
            it.remove(PIN_HASH)
            it[LOCK_TYPE] = "NONE"
            it[APP_LOCK_ENABLED] = false
        }
    }

    fun verifyPin(pin: String, context: Context): Flow<Boolean> = dataStore.data
        .catchIo()
        .map { prefs -> prefs[PIN_HASH] == hashPin(pin) }

    suspend fun setAutoLockTimeout(minutes: Int) {
        dataStore.edit { it[AUTO_LOCK_TIMEOUT_MIN] = minutes }
    }

    suspend fun setAdFreePurchased(purchased: Boolean) {
        dataStore.edit { it[AD_FREE_PURCHASED] = purchased }
    }

    suspend fun setShowOcrAfterScan(show: Boolean) {
        dataStore.edit { it[SHOW_OCR_AFTER_SCAN] = show }
    }

    suspend fun setWatermarkEnabled(enabled: Boolean) {
        dataStore.edit { it[WATERMARK_ENABLED] = enabled }
    }

    suspend fun setWatermarkGpsEnabled(enabled: Boolean) {
        dataStore.edit { it[WATERMARK_GPS_ENABLED] = enabled }
    }

    suspend fun setHapticEnabled(enabled: Boolean) {
        dataStore.edit { it[HAPTIC_FEEDBACK] = enabled }
    }

    suspend fun setScanSoundEnabled(enabled: Boolean) {
        dataStore.edit { it[SCAN_SOUND] = enabled }
    }

    suspend fun markFirstLaunchDone() {
        dataStore.edit { it[FIRST_LAUNCH] = false }
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun Flow<Preferences>.catchIo() = catch { e ->
        if (e is IOException) emit(emptyPreferences()) else throw e
    }
}
