# DocScan Pro

Android document scanner — free, no-sign-in, on-device OCR + PDF. Monetized via AdMob rewarded ads + one-time IAP ("Ad-Free Forever").

**Package:** `com.abhijit.docscanpro`  
**Min SDK:** 26 | **Target SDK:** 36 | **Language:** Kotlin 2.2.10 + Jetpack Compose  
**Project path:** `D:\Android_Studio\DocScanPro`  
**GitHub:** https://github.com/abhijitdalal26/DocScanPro

---

## Core constraint: no AI API, no cloud

Everything runs on-device using free libraries. No cloud calls, no API keys for core features. AI/LLM features (summarization, Ask AI, advanced handwriting OCR) are deferred to Phase 4+.

---

## Tech Stack

| Layer | Library | Version |
|---|---|---|
| Camera | CameraX (camera2 + lifecycle + view) | 1.4.0 |
| OCR | ML Kit Text Recognition v2 (Latin + Devanagari) | bundled |
| Entity extraction | ML Kit Entity Extraction | bundled |
| Barcode | ML Kit Barcode Scanning | 17.3.0 |
| QR generation | ZXing Core | 3.5.3 |
| PDF create | Android PdfDocument (built-in) | — |
| PDF edit | PDFBox-Android (merge/split/password AES-256) | 2.0.27.0 |
| Image processing | OpenCV Android | 4.10.0 |
| Database | Room + KAPT (not KSP — Kotlin 2.2.x compat) | 2.6.1 |
| Preferences | DataStore Preferences | 1.1.1 |
| Security | Biometric (BIOMETRIC_STRONG / DEVICE_CREDENTIAL) | 1.1.0 |
| UI | Jetpack Compose BOM | 2026.02.01 |
| UI components | Material 3 | via BOM |
| Navigation | Navigation Compose | 2.8.0 |
| ViewModel | ViewModel Compose | 2.8.0 |
| Image loading | Coil 3 (io.coil-kt.coil3) | 3.0.4 |
| DI | None — manual construction in ViewModels | — |

---

## Architecture

Single Activity (`MainActivity`) + Jetpack Navigation Compose with sealed class routes.

```
app/src/main/java/com/abhijit/docscanpro/
  camera/
    CameraManager.kt           ← CameraX setup, lifecycle binding
    DocumentBorderDetector.kt  ← OpenCV: Canny → dilate → contours → quad
    ImageEnhancer.kt           ← warpPerspective, color modes, CLAHE, deskew
  ocr/
    OcrEngine.kt               ← ML Kit Text Recognition v2 wrapper
    BusinessCardExtractor.kt   ← Entity extraction + regex; vCard export
  pdf/
    PdfCreator.kt              ← Android PdfDocument, 4 quality levels
    PdfEditor.kt               ← PDFBox: merge, split, extract, compress,
                                  page numbers, watermark, PDF-to-images
  export/
    ExportManager.kt           ← Share PDF/image/text/markdown, FileProvider
  utils/
    FileUtils.kt
    WatermarkCamera.kt         ← Canvas timestamp + GPS stamp
    QrGenerator.kt             ← ZXing QR + barcode generation
    MarkdownExporter.kt        ← Heading/bullet/table detection
    DocumentClassifier.kt      ← 8 types, keyword scoring
    DocumentMetadataExtractor.kt
  security/
    AppLockManager.kt          ← BiometricPrompt, session lock state
  service/
    ScanTileService.kt         ← Quick Settings tile (Android 14 compat)
  widget/
    ScanWidget.kt              ← 4×1 homescreen widget → scanner deep-link
  data/
    model/
      Document.kt              ← Room entity; tagList() helper; LockType/DocumentType enums
      Page.kt                  ← Room entity; FK → Document (CASCADE)
    db/
      AppDatabase.kt
      DocumentDao.kt / PageDao.kt
    repository/
      DocumentRepository.kt    ← single source of truth; reorderPages() exists
    preferences/
      AppPreferences.kt        ← DataStore: PIN hash, lock, theme, OCR, watermark...
  ui/
    theme/
      Theme.kt                 ← DocScanProTheme(darkThemeOverride: String)
    navigation/
      Screen.kt                ← sealed class, 11 routes
      NavGraph.kt              ← AppNavGraph; onboarding LaunchedEffect; deep-link handling
    screens/
      home/        HomeScreen + HomeViewModel (speed-dial FAB, recent docs grid)
      scanner/     ScannerScreen + ScannerViewModel (CameraX, batch pages, overlay)
                   BarcodeScannerScreen (ML Kit barcode, result sheet, copy/open)
                   ImagesToPdfScreen + ImagesToPdfViewModel (gallery → PDF)
      library/     LibraryScreen + LibraryViewModel (search, sort, batch select)
                   RecycleBinScreen + RecycleBinViewModel
      onboarding/  OnboardingScreen (4-page HorizontalPager, animated dots)
      viewer/      DocumentViewerScreen + DocumentViewerViewModel
                     (HorizontalPager, OCR panel, QR export, vCard export, rename)
                   PdfToolsScreen + PdfToolsViewModel
                     (compress, password, merge, split, page numbers, watermark, PDF→images)
      lock/        LockScreen + LockViewModel (PIN dots, numpad, biometric auto-prompt)
      settings/    SettingsScreen + SettingsViewModel (11 DataStore flows → UiState)
```

---

## Key patterns

- **AndroidViewModel** everywhere (needs `application.applicationContext`)
- **StateFlow UiState** — one data class per screen, `.update {}` pattern
- **Separate `viewModelScope.launch` per preference** in SettingsViewModel — avoids `combine()` type limit with 6+ flows
- **`@file:OptIn`** at the top of every screen file using experimental APIs (do NOT add `@OptIn` at function level if file-level opt-in already covers it)
- **Lazy delegate pattern** for closeable resources: `val xDelegate = lazy { X() }; val x by xDelegate` then `xDelegate.isInitialized()` in `onCleared()` — standard `::x.isInitialized` only works for `lateinit`
- **Job cancellation** for search: `ocrSearchJob?.cancel()` before starting new OCR search coroutine
- **PathFillType.EvenOdd** Canvas technique for scanner viewfinder overlay (dim outside the guide frame)
- **Speed-dial FAB**: `AnimatedVisibility` + `fadeIn/slideInVertically` + `SmallFloatingActionButton`

---

## Multi-agent workflow

Each terminal runs Claude in `D:\Android_Studio\DocScanPro`. Before starting work, check `docs/features.md` for task status — pick a `📋 TODO` row with **Agent Ready: Yes**.

- Mark it `🔄 IN PROGRESS` in features.md, do the work, mark `✅ DONE` when done.
- Don't add AI API calls — everything must run offline on-device.

---

## Docs

| File | Contents |
|---|---|
| `docs/features.md` | **Feature tracker** — single source of truth for what's done and what's next |
| `docs/dependencies.md` | All libraries, versions, why each was chosen |

---

## Build & run

1. Open `D:\Android_Studio\DocScanPro` in Android Studio
2. Let Gradle sync (AGP 9.2.1, compileSdk 36.1)
3. Run on physical device (API 26+) — emulator camera unreliable for scanning

---

## Phase status

- **Phase 1 (MVP):** ~90% complete — camera, OCR, PDF, all core screens built and wired. Remaining: real-time border preview overlay, folder/tag UI (data model exists), page reorder drag & drop (repository method exists)
- **Phase 2:** AdMob + IAP wiring, barcode scan enhancements
- **Phase 3+:** Annotations, signatures, DigiLocker, multi-language UI
- **Phase 4+:** AI API features (LLM summarization, etc.) — deferred until revenue supports it
