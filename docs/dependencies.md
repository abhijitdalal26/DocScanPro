# Dependencies

All versions pinned in `gradle/libs.versions.toml`.

## Core Libraries

### CameraX `1.4.0` ✅ Best stable
- `androidx.camera:camera-core` — core CameraX APIs
- `androidx.camera:camera-camera2` — Camera2 implementation
- `androidx.camera:camera-lifecycle` — lifecycle-aware camera binding
- `androidx.camera:camera-view` — PreviewView composable helper
- **Why:** Official Jetpack camera library, handles lifecycle, rotation, flash automatically.

### ML Kit Text Recognition `16.0.1` ✅ Best on-device OCR
- `com.google.mlkit:text-recognition` — Latin script (100+ languages)
- `com.google.mlkit:text-recognition-devanagari` — Hindi, Sanskrit, Marathi, Nepali
- **Why:** 100% on-device, free, zero APK size (model downloads via Play Services). Returns bounding boxes per word — critical for Markdown exporter and table detection.

### ML Kit Barcode Scanning `17.3.0` ✅ Replaced ZXing Android
- `com.google.mlkit:barcode-scanning`
- **Why replaced ZXing Android Embedded (4.3.0):** ZXing Android wrapper was last meaningfully updated in 2022 and is effectively unmaintained. ML Kit Barcode is GPU-accelerated, handles damaged/partial barcodes, works better in low light, and is actively maintained by Google. Same on-device, same zero APK size.
- **Formats:** QR, Data Matrix, PDF417, Aztec, EAN, UPC, Code128, Code39, ITF, Codabar.

### ZXing Core `3.5.3` ✅ QR generation only
- `com.google.zxing:core`
- **Why:** ML Kit only handles scanning, not generation. ZXing Core is the industry-standard QR/barcode encoder. Just the core library (~350KB), no Android UI wrapper.

### ML Kit Entity Extraction `16.0.0-beta5` ✅ Only on-device option
- `com.google.mlkit:entity-extraction`
- **Why:** Powers business card scanner. Extracts name, phone, email, URL, address from OCR text. Fully on-device.

### PDFBox-Android `2.0.27.0` ✅ Best Apache 2.0 PDF lib
- `com.tom-roush:pdfbox-android`
- **License:** Apache 2.0 — commercial-friendly (unlike iText which is AGPL)
- **Why:** Merge, split, password protection (AES-256), page extraction, compression.
- **Note:** For basic image→PDF we use Android's built-in `android.graphics.pdf.PdfDocument` (zero library cost). PDFBox only for advanced operations.

### Room `2.6.1` ✅ Official Jetpack ORM
- `androidx.room:room-runtime`, `room-ktx`, `room-compiler` (KAPT)
- **Why:** Flow queries auto-update UI when DB changes. KAPT generates boilerplate. Most battle-tested SQLite ORM on Android.

### Coil `3.0.4` ✅ Upgraded from 2.7.0
- `io.coil-kt.coil3:coil-compose`
- **Why upgraded from 2.7.0:** Coil 3.x is coroutines-first (no more callbacks), better memory management under Compose, multiplatform-ready. Package changed from `io.coil-kt` to `io.coil-kt.coil3`.
- **Why Coil over Glide:** Coil is Kotlin-first, designed for Compose. Glide is Java-legacy.

### DataStore Preferences `1.1.1` ✅ Added
- `androidx.datastore:datastore-preferences`
- **Why added:** Replaces SharedPreferences for storing app settings (default color mode, auto-lock timeout, PIN hash, ad-free status). DataStore uses coroutines + Flow — settings changes automatically propagate to UI. SharedPreferences is synchronous I/O on main thread.

### Biometric `1.1.0` ✅ Added
- `androidx.biometric:biometric`
- **Why added:** Needed for the biometric/fingerprint lock on documents and app. Handles all API levels, falls back to PIN on devices without biometrics.

### kotlinx-collections-immutable `0.3.7` ✅ Added
- `org.jetbrains.kotlinx:kotlinx-collections-immutable`
- **Why added:** `ImmutableList<T>` and `ImmutableSet<T>` prevent Compose from triggering recompositions when passing list/set state to composables. Standard `List<T>` doesn't implement structural equality, so Compose always recomposes even if contents didn't change.

### Splash Screen API `1.0.1` ✅ Added
- `androidx.core:core-splashscreen`
- **Why added:** Android 12+ has a system splash screen. Without this library it shows a generic white screen. This gives us a branded, animated launch screen that matches the app theme.

### Coroutines `1.8.1` ✅ Keep
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`
- All async ops (OCR, PDF creation, DB queries) run in `viewModelScope` on IO dispatcher.

### Navigation Compose `2.8.0` ✅ Keep
- `androidx.navigation:navigation-compose`
- Handles back stack, deep links, argument passing between screens.

### Lifecycle ViewModel Compose `2.8.0` ✅ Keep
- `androidx.lifecycle:lifecycle-viewmodel-compose`
- `viewModel()` composable factory for screen-level ViewModels.

---

## What we intentionally did NOT add

| Library | Why skipped |
|---|---|
| ZXing Android Embedded | Unmaintained since 2022 — replaced with ML Kit Barcode |
| iText7 | AGPL license = incompatible with commercial app. Using PDFBox (Apache 2.0). |
| Hilt / Dagger | Overkill for MVP. Add in Phase 2 if manual DI becomes messy. |
| Retrofit / OkHttp | No network calls in Phase 1. Everything offline. |
| Firebase | No backend, no auth, no Firestore. 100% local-first. |
| Apache POI (DOCX) | 8MB+ APK hit. DOCX export done with minimal XML template instead. |
| OpenCV | Not in Gradle yet. See below. |

---

## Adding OpenCV (next step)

OpenCV is needed for edge detection, perspective correction, color modes, shadow removal.

**Option A — Maven Central (simplest, recommended):**
```toml
# in [versions]
opencv = "4.10.0"
# in [libraries]
opencv-android = { group = "org.opencv", name = "opencv", version.ref = "opencv" }
```
```kotlin
// in app/build.gradle.kts
implementation(libs.opencv.android)
```

**Option B — AAR module (smaller APK, more control):**
1. Download OpenCV Android SDK from opencv.org/releases/
2. Copy `sdk/` folder into project as `opencv/` module
3. Add `include(":opencv")` to settings.gradle.kts
4. Add `implementation(project(":opencv"))` to app/build.gradle.kts
5. In `build.gradle.kts` of the opencv module, exclude native ABIs you don't need (saves 30-40MB)

Start with Option A. Switch to B only if APK size becomes a concern.
