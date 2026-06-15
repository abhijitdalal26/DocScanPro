# DocScan Pro

Android document scanner — free, no-sign-in, on-device OCR + PDF. Monetized via AdMob rewarded ads + one-time IAP ("Ad-Free Forever").

**Notion spec:** PRJ-14 in Project Tracker  
**Package:** `com.abhijit.docscanpro`  
**Min SDK:** 26 | **Target SDK:** 36 | **Language:** Kotlin + Jetpack Compose  
**Project path:** `D:\Android_Studio\DocScanPro`

---

## What we build (no AI API, no on-device models)

Everything on-device using free libraries: CameraX, ML Kit Text Recognition, OpenCV, ZXing, PDFBox-Android. No cloud calls, no API keys needed for core features.

AI/LLM features (summarization, Ask AI, handwriting OCR beyond ML Kit) are explicitly deferred to Phase 4+.

---

## Tech Stack

| Layer | Library |
|---|---|
| Camera | CameraX (camera2 + lifecycle + view) |
| OCR | ML Kit Text Recognition v2 (100+ langs + Devanagari) |
| QR/Barcode | ZXing |
| PDF | Android PdfDocument (create) + PDFBox-Android (merge/split/password) |
| Image processing | OpenCV Android (edge detect, perspective correct, color modes) |
| Database | Room (SQLite) |
| UI | Jetpack Compose + Material 3 |
| Navigation | Navigation Compose |
| State | ViewModel + StateFlow + Coroutines |
| Image loading | Coil |
| DI | None (manual for now, add Hilt in Phase 2 if needed) |

---

## Architecture

```
app/src/main/java/com/abhijit/docscanpro/
  camera/         ← CameraX setup, edge detection, color modes
  ocr/            ← ML Kit OCR wrapper
  pdf/            ← PDF creation, merge, split, compress, password
  export/         ← Export to PDF/TXT/DOCX/Markdown, sharing
  data/
    model/        ← Room entities (Document, Page)
    db/           ← DAOs, AppDatabase, Converters
    repository/   ← DocumentRepository (single source of truth)
  utils/          ← RegexExtractors, DocumentClassifier, MarkdownExporter, FileUtils
  ui/
    theme/        ← Color, Type, Theme (Compose theme — design pending)
    navigation/   ← Screen sealed class, NavGraph
    screens/
      home/       ← HomeScreen + HomeViewModel
      scanner/    ← ScannerScreen + ScannerViewModel
      library/    ← LibraryScreen + LibraryViewModel
      viewer/     ← DocumentViewerScreen + ViewerViewModel
      settings/   ← SettingsScreen
```

---

## Multi-agent workflow

Each terminal runs Claude in `D:\Android_Studio\DocScanPro`. Before starting work, check `docs/features.md` for task status and pick a `📋 TODO` item that has **Agent Ready: Yes**.

- Mark it `🔄 IN PROGRESS` in features.md, do the work, mark `✅ DONE` when done.
- Don't touch UI screens (HomeScreen, ScannerScreen etc.) until the design is finalized.
- Don't add AI API calls — everything must run offline on-device.

---

## Docs

| File | Contents |
|---|---|
| `docs/architecture.md` | Package structure, data flow, design decisions |
| `docs/features.md` | **Feature tracker** — pick work from here |
| `docs/dependencies.md` | All libraries, versions, why each was chosen |

---

## How to build & run

1. Open `D:\Android_Studio\DocScanPro` in Android Studio
2. Let Gradle sync complete
3. Run on device or emulator (API 26+)
4. Camera requires real device (emulator camera is not reliable for scanning)

---

## Phase plan

- **Phase 1 (MVP):** Camera scan → multi-page PDF → local storage → basic OCR → AdMob + IAP wiring
- **Phase 2:** Rule-based AI (doc classify, Aadhaar/PAN/GST extract, business card, Markdown export)
- **Phase 3:** Annotations, signatures, India-specific features, multi-language UI
- **Phase 4+:** AI API features if revenue supports it
