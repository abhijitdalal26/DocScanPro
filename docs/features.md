# Feature Tracker

## Legend
- ✅ DONE
- 🔄 IN PROGRESS  
- 📋 TODO
- 🚫 BLOCKED (dependency listed)
- 🔴 DEFERRED (needs AI API — do not build)

## Agent workflow
1. Pick a `📋 TODO` row where **Agent Ready = Yes**
2. Update its status to `🔄 IN PROGRESS` in this file
3. Do the work
4. Update to `✅ DONE` and fill in any notes

---

## Phase 1 — MVP Core

### Infrastructure
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| Room DB models (Document, Page) | ✅ DONE | `data/model/`, `data/db/` | — | |
| DocumentRepository | ✅ DONE | `data/repository/DocumentRepository.kt` | — | |
| FileUtils | ✅ DONE | `utils/FileUtils.kt` | — | |
| Navigation setup (NavGraph, Screen) | ✅ DONE | `ui/navigation/` | — | Deep-link from widget/tile wired |
| App theme (DocScanProTheme) | ✅ DONE | `ui/theme/` | — | Brand palette added, Material 3 |
| DataStore preferences | ✅ DONE | `data/preferences/AppPreferences.kt` | — | PIN hash, biometric, watermark, OCR, ad-free |
| Splash screen | ✅ DONE | `res/values/themes.xml`, `MainActivity.kt` | — | Deep navy #0D1117 |
| Homescreen widget | ✅ DONE | `widget/ScanWidget.kt`, `res/xml/widget_scan_info.xml` | — | 4×1, taps → scanner |
| Quick Settings tile | ✅ DONE | `service/ScanTileService.kt` | — | Android 14 API compat |
| Long-press app shortcuts | ✅ DONE | `res/xml/shortcuts.xml` | — | Scan + Scan QR shortcuts |

### Camera & Scanning
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| CameraX integration | ✅ DONE | `camera/CameraManager.kt`, `ui/screens/scanner/ScannerScreen.kt` | — | LivePreview + capture + torch + zoom |
| Auto edge detection (OpenCV) | ✅ DONE | `camera/DocumentBorderDetector.kt` | — | Canny→dilate→findContours→quad |
| Perspective correction (OpenCV) | ✅ DONE | `camera/ImageEnhancer.kt` | — | warpPerspective with ordered corners |
| 5 color modes (OpenCV) | ✅ DONE | `camera/ImageEnhancer.kt` | — | Original, B&W (adaptive threshold), Grayscale, Magic Color, Enhanced/CLAHE |
| Shadow removal (OpenCV) | ✅ DONE | `camera/ImageEnhancer.kt` | — | dilate→medianBlur→absdiff→normalize |
| Deblur + sharpen | ✅ DONE | `camera/ImageEnhancer.kt` | — | bilateral filter + unsharp masking |
| Deskew | ✅ DONE | `camera/ImageEnhancer.kt` | — | Hough lines → rotation correction |
| Batch scanning session | ✅ DONE | `ui/screens/scanner/ScannerViewModel.kt` | — | List of captured pages in memory |
| Watermark camera | ✅ DONE | `utils/WatermarkCamera.kt` | — | Canvas timestamp + GPS stamp |
| QR + barcode scanner (ML Kit) | ✅ DONE | `ui/screens/scanner/BarcodeScannerScreen.kt` | — | Full ML Kit barcode scanner with overlay, result sheet, copy/open |
| Real-time border preview overlay | 📋 TODO | `ui/screens/scanner/ScannerScreen.kt` | Yes | Draw detected quad over PreviewView |

### OCR
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| OCR engine (ML Kit) | ✅ DONE | `ocr/OcrEngine.kt` | — | Latin + Devanagari, returns OcrResult with blocks |
| Store OCR text in Room | ✅ DONE | `ui/screens/scanner/ScannerViewModel.kt` | — | OCR run per page on save |
| Business card extraction | ✅ DONE | `ocr/BusinessCardExtractor.kt` | — | ML Kit Entity + regex heuristics; vCard export |
| Export OCR as TXT | ✅ DONE | `export/ExportManager.kt` | — | exportToTxt |
| Export OCR as Markdown | ✅ DONE | `export/ExportManager.kt`, `utils/MarkdownExporter.kt` | — | Detects headings/bullets/tables |
| Copy to clipboard | ✅ DONE | `export/ExportManager.kt` | — | copyTextToClipboard |
| Full-text search across pages | ✅ DONE | `ui/screens/library/LibraryViewModel.kt` | — | searchInOcrText wired in search(); ocrDocumentIds cache merges with name filter |

### PDF
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| Create PDF from images | ✅ DONE | `pdf/PdfCreator.kt` | — | Android PdfDocument, 4 quality levels |
| Password-protect PDF | ✅ DONE | `pdf/PdfEditor.kt` | — | PDFBox AES-256 StandardProtectionPolicy |
| Merge PDFs | ✅ DONE | `pdf/PdfEditor.kt` | — | PDFBox PDFMergerUtility |
| Split PDF | ✅ DONE | `pdf/PdfEditor.kt` | — | PDFBox Splitter |
| Extract pages | ✅ DONE | `pdf/PdfEditor.kt` | — | PDFBox |
| PDF compression | ✅ DONE | `pdf/PdfEditor.kt`, `ui/screens/viewer/PdfToolsScreen.kt` | — | Re-render pages as JPEG, quality slider 20–90% |
| Image to PDF (from gallery) | ✅ DONE | `ui/screens/scanner/ImagesToPdfScreen.kt` | — | Gallery picker → image list → PdfCreator, navigates to viewer |
| PDF to images | ✅ DONE | `pdf/PdfEditor.kt`, `ui/screens/viewer/PdfToolsScreen.kt` | — | PdfRenderer → JPEG per page → ExportManager.shareMultipleFiles |
| Add page numbers | ✅ DONE | `pdf/PdfEditor.kt`, `ui/screens/viewer/PdfToolsScreen.kt` | — | Android PdfDocument Canvas stamp at bottom |
| Add watermark overlay to PDF | ✅ DONE | `pdf/PdfEditor.kt`, `ui/screens/viewer/PdfToolsScreen.kt` | — | Diagonal text watermark, custom text, 60% opacity |

### Document Management
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| Room CRUD (save/load/delete docs) | ✅ DONE | `data/db/`, `data/repository/` | — | |
| Pin / favorite documents | ✅ DONE | `data/repository/DocumentRepository.kt` | — | setFavorite |
| Recycle bin (soft delete) | ✅ DONE | `data/repository/DocumentRepository.kt` | — | moveToRecycleBin |
| Sort by date/name/size | ✅ DONE | `ui/screens/library/LibraryViewModel.kt` | — | 5 sort orders in-memory |
| Document type filter | ✅ DONE | `ui/screens/library/LibraryViewModel.kt` | — | filterByType |
| Batch delete | ✅ DONE | `ui/screens/library/LibraryViewModel.kt` | — | moveToRecycleBinBatch |
| Document metadata extraction | ✅ DONE | `utils/DocumentMetadataExtractor.kt` | — | Unified type+regex pipeline with dates/amounts |
| Document classifier | ✅ DONE | `utils/DocumentClassifier.kt` | — | 8 types, keyword scoring |
| Full-text search | ✅ DONE | `ui/screens/library/LibraryViewModel.kt` | — | Searches doc name + OCR text across all pages |
| Folder/tag organization | 📋 TODO | `ui/screens/library/` | Yes | Tags in model; UI not yet built |
| Page reorder (drag & drop) | 📋 TODO | `ui/screens/viewer/` | Yes | Use Compose drag APIs |

### Export & Sharing
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| Share PDF | ✅ DONE | `export/ExportManager.kt` | — | FileProvider + Intent.ACTION_SEND |
| Share page as image | ✅ DONE | `export/ExportManager.kt` | — | FileProvider |
| Quick Share (compressed bitmap) | ✅ DONE | `export/ExportManager.kt` | — | quickShareBitmap → cache → share |
| Share quality presets | ✅ DONE | `export/ExportManager.kt` | — | HIGH(95)/MEDIUM(75)/COMPRESSED(50) |
| Export as Markdown | ✅ DONE | `export/ExportManager.kt` | — | MarkdownExporter |
| Copy OCR text | ✅ DONE | `export/ExportManager.kt` | — | |
| QR code generation | ✅ DONE | `utils/QrGenerator.kt` | — | ZXing Core — QR + barcode formats |
| QR export flow (UI) | ✅ DONE | `ui/screens/viewer/DocumentViewerScreen.kt`, `DocumentViewerViewModel.kt` | — | Generate QR from OCR text, dialog with Share button |

### Security & Privacy
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| Biometric auth (BiometricPrompt) | ✅ DONE | `security/AppLockManager.kt` | — | BIOMETRIC_STRONG or DEVICE_CREDENTIAL |
| Session lock state | ✅ DONE | `security/AppLockManager.kt` | — | Auto-timeout in minutes |
| PIN lock (SHA-256 hash) | ✅ DONE | `data/preferences/AppPreferences.kt` | — | Hash stored in DataStore |
| Settings UI for security | ✅ DONE | `ui/screens/settings/SettingsScreen.kt` | — | PIN dialog + biometric toggle |
| Lock screen UI | ✅ DONE | `ui/screens/lock/LockScreen.kt` | — | Full PIN + biometric screen; MainActivity.onResume() enforces it |
| Password-protected PDF | ✅ DONE | `pdf/PdfEditor.kt` | — | AES-256 via PDFBox |

### UI Screens
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| HomeScreen | ✅ DONE | `ui/screens/home/HomeScreen.kt` | — | Recent docs grid + stats + FAB |
| ScannerScreen | ✅ DONE | `ui/screens/scanner/ScannerScreen.kt` | — | CameraX preview + capture + color modes + page strip |
| LibraryScreen | ✅ DONE | `ui/screens/library/LibraryScreen.kt` | — | Search + filter tabs + grid + batch select |
| DocumentViewerScreen | ✅ DONE | `ui/screens/viewer/DocumentViewerScreen.kt` | — | HorizontalPager + OCR panel + share/delete/password |
| SettingsScreen | ✅ DONE | `ui/screens/settings/SettingsScreen.kt` | — | All DataStore settings wired |
| DocumentViewerViewModel | ✅ DONE | `ui/screens/viewer/DocumentViewerViewModel.kt` | — | Load doc + pages, share, favorite, delete |
| SettingsViewModel | ✅ DONE | `ui/screens/settings/SettingsViewModel.kt` | — | 10 DataStore flows → UiState |
| Lock screen | ✅ DONE | `ui/screens/lock/LockScreen.kt`, `LockViewModel.kt` | — | PIN dots, numpad, biometric auto-prompt |
| Recycle bin screen | ✅ DONE | `ui/screens/library/RecycleBinScreen.kt` | — | Restore, permanent delete, empty bin dialog |
| Onboarding flow | ✅ DONE | `ui/screens/onboarding/OnboardingScreen.kt` | — | 4-page HorizontalPager, animated dots, skip/next, DataStore first-launch flag |

---

## Phase 2 — Planned (Month 3–4)

| Feature | Notes |
|---|---|
| AdMob rewarded ads integration | AdMob SDK, gate OCR export + high-res export |
| One-time IAP ("Ad-Free Forever" ₹199) | Google Play Billing Library |
| Barcode scan mode UI | ML Kit Barcode already included |
| Image deblur (OpenCV) | `ImageEnhancer.kt` already has sharpening |
| Low-light enhancement | OpenCV CLAHE already implemented |
| DigiLocker integration | Document import from DigiLocker |
| Finger removal from scan | OpenCV inpainting |

---

## 🔴 Deferred — AI API required (DO NOT BUILD)

- AI summarization (needs LLM)
- Ask AI / Chat with document (needs LLM)
- Handwriting OCR beyond ML Kit (needs GOT-OCR2 / SmolDocling)
- Table extraction to XLSX from complex scans (needs layout model)
- Formula/equation recognition
- AI Math Solver
- Translation (revisit Google Translate free tier in Phase 3)
