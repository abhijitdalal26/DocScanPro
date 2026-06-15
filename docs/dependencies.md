# Dependencies

All versions pinned in `gradle/libs.versions.toml`.

## Core Libraries

### CameraX `1.4.0`
- `androidx.camera:camera-core` — core CameraX APIs
- `androidx.camera:camera-camera2` — Camera2 implementation
- `androidx.camera:camera-lifecycle` — lifecycle-aware camera binding
- `androidx.camera:camera-view` — PreviewView composable helper
- **Why:** Official Jetpack camera library, handles lifecycle, rotation, flash automatically. Best choice for 2024+ Android cameras.

### ML Kit Text Recognition `16.0.1`
- `com.google.mlkit:text-recognition` — Latin script (covers 100+ languages)
- `com.google.mlkit:text-recognition-devanagari` — Hindi, Sanskrit, Nepali, Marathi
- **Why:** 100% on-device, no internet, free, fast. Returns bounding boxes per word — needed for table/heading detection in Markdown exporter.

### ML Kit Entity Extraction `16.0.0-beta5`
- `com.google.mlkit:entity-extraction` — extracts name, phone, email, URL, date from text
- **Why:** Powers business card scanner. Fully on-device, no API cost.

### ZXing Android Embedded `4.3.0`
- `com.journeyapps:zxing-android-embedded`
- **Why:** Industry-standard barcode/QR library for Android. Handles encode + decode. Smaller than ML Kit barcode for our use case.

### PDFBox-Android `2.0.27.0`
- `com.tom-roush:pdfbox-android`
- **Apache License 2.0** — commercial-friendly
- **Why:** Best open-source PDF manipulation on Android. Handles merge, split, password protection (AES-256), page extraction. Android port of Apache PDFBox.
- **Note:** For basic image→PDF creation we use Android's built-in `android.graphics.pdf.PdfDocument` (zero library size cost). PDFBox only for advanced ops.

### OpenCV Android
- Not in Gradle — add as AAR module manually or use `org.opencv:opencv:4.10.0` (Maven Central since 4.9.0)
- **Why:** Edge detection (Canny), perspective correction (warpPerspective), color mode processing (CLAHE, threshold, bilateral filter), shadow removal. No alternative for this quality of image processing on Android.
- **Setup:** Download OpenCV Android SDK from opencv.org, add as module or add Maven Central dependency.

### Room `2.6.1`
- `androidx.room:room-runtime` — core Room
- `androidx.room:room-ktx` — coroutines support (Flow queries)
- `androidx.room:room-compiler` — KAPT annotation processor
- **Why:** Official Jetpack SQLite ORM. Flow support means DB changes automatically update UI via StateFlow.

### Coroutines `1.8.1`
- `org.jetbrains.kotlinx:kotlinx-coroutines-android`
- **Why:** All async ops (OCR, PDF creation, DB queries) run in `viewModelScope` on IO dispatcher. Never blocks main thread.

### Navigation Compose `2.8.0`
- `androidx.navigation:navigation-compose`
- **Why:** Official Compose navigation. Handles back stack, deep links, argument passing between screens.

### Lifecycle ViewModel Compose `2.8.0`
- `androidx.lifecycle:lifecycle-viewmodel-compose`
- **Why:** `viewModel()` composable factory. Needed for screen-level ViewModel injection.

### Coil Compose `2.7.0`
- `io.coil-kt:coil-compose`
- **Why:** Async image loading for document thumbnails in library grid. AsyncImage composable integrates with Compose lifecycle. Faster than Glide for Compose.

---

## What we intentionally did NOT add

| Library | Why skipped |
|---|---|
| Hilt / Dagger | Overkill for MVP. Manual DI is fine with 1-2 screens. Add in Phase 2 if wiring grows complex. |
| Retrofit / OkHttp | No network calls in Phase 1. Everything offline. |
| Firebase | No backend, no auth, no Firestore. 100% local. |
| iText7 | AGPL license = incompatible with commercial app unless paid. Using PDFBox (Apache 2.0) instead. |
| Apache POI (DOCX) | 8MB+ size hit. DOCX export can be done with a minimal XML template writer instead. |
| Google Translate API | Has free tier (500K chars/month) but adds network dependency. Deferred to Phase 3. |

---

## Adding OpenCV

OpenCV is not in Maven Central in a stable form for all archs. Two options:

**Option A — Maven Central (simpler):**
```toml
opencv = "4.10.0"
opencv-android = { group = "org.opencv", name = "opencv", version.ref = "opencv" }
```
```kotlin
implementation(libs.opencv.android)
```

**Option B — AAR module (more control):**
1. Download OpenCV Android SDK from https://opencv.org/releases/
2. Extract, copy `sdk/` folder into project as `opencv/` module
3. Add `include(":opencv")` to settings.gradle.kts
4. Add `implementation(project(":opencv"))` to app/build.gradle.kts

Option A is simpler. Option B gives smaller APK (can exclude unused native libs). Start with Option A.
