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
| Navigation setup (NavGraph, Screen) | ✅ DONE | `ui/navigation/` | — | |
| Screen skeletons (Home, Scanner, Library, Viewer, Settings) | ✅ DONE | `ui/screens/` | — | Design pending |

### Camera & Scanning
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| CameraX integration (skeleton) | ✅ DONE | `camera/CameraManager.kt` | — | Preview + capture wired |
| Auto edge detection (OpenCV) | 📋 TODO | `camera/EdgeDetector.kt` | Yes | Use OpenCV Canny + findContours |
| Perspective correction (OpenCV) | 📋 TODO | `camera/PerspectiveCorrector.kt` | Yes | getPerspectiveTransform + warpPerspective |
| 5 color modes (OpenCV) | 📋 TODO | `camera/ColorProcessor.kt` | Yes | Original, B&W, Grayscale, Magic, Enhanced |
| Deblur + sharpen (OpenCV) | 📋 TODO | `camera/ImageEnhancer.kt` | Yes | Laplacian sharpening, Gaussian blur |
| Shadow removal (OpenCV) | 📋 TODO | `camera/ImageEnhancer.kt` | Yes | Normalize illumination |
| Batch scanning session | 📋 TODO | `ui/screens/scanner/ScannerViewModel.kt` | Yes | List of captured pages in memory |
| QR + barcode scanner (ZXing) | 📋 TODO | `camera/QrScanner.kt` | Yes | ZXing BarcodeScanner wrapper |
| Whiteboard mode | 📋 TODO | `camera/ColorProcessor.kt` | 🚫 | Needs color modes done first |
| Real-time border preview overlay | 🚫 BLOCKED | `ui/screens/scanner/` | No | Needs design + EdgeDetector done |

### OCR
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| OCR engine (ML Kit skeleton) | ✅ DONE | `ocr/OcrEngine.kt` | — | Latin + Devanagari |
| Store OCR text in Room | 📋 TODO | `data/repository/DocumentRepository.kt` | Yes | Save OcrResult.fullText to Page.ocrText |
| Searchable PDF creation | 📋 TODO | `pdf/PdfCreator.kt` | Yes | Overlay invisible text layer on PDF |
| Export OCR as TXT | 📋 TODO | `export/ExportManager.kt` | Yes | |
| Export OCR as DOCX | 📋 TODO | `export/ExportManager.kt` | Yes | Use Apache POI or simple XML |
| Copy to clipboard | 📋 TODO | `export/ExportManager.kt` | Yes | ClipboardManager |

### PDF
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| Create PDF from images | ✅ DONE | `pdf/PdfCreator.kt` | — | Uses Android PdfDocument |
| PDF compression (4 levels) | 📋 TODO | `pdf/PdfEditor.kt` | Yes | Re-render pages as JPEG at lower quality |
| Password-protect PDF | 📋 TODO | `pdf/PdfEditor.kt` | Yes | PDFBox-Android StandardProtectionPolicy |
| Merge PDFs | 📋 TODO | `pdf/PdfEditor.kt` | Yes | PDFBox-Android PDFMergerUtility |
| Split PDF | 📋 TODO | `pdf/PdfEditor.kt` | Yes | PDFBox-Android Splitter |
| Image to PDF (from gallery) | 📋 TODO | `pdf/PdfCreator.kt` | Yes | Pick images via gallery, convert |
| PDF to images | 📋 TODO | `pdf/PdfEditor.kt` | Yes | PDFBox-Android PDFRenderer |
| Add page numbers | 📋 TODO | `pdf/PdfEditor.kt` | Yes | |
| Add watermark overlay | 📋 TODO | `pdf/PdfEditor.kt` | Yes | |

### Document Management
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| Room CRUD (save/load/delete docs) | ✅ DONE | `data/db/`, `data/repository/` | — | |
| Full-text search across docs | 📋 TODO | `data/db/DocumentDao.kt` | Yes | Room FTS4 or LIKE query |
| Sort by date/name/size | 📋 TODO | `data/db/DocumentDao.kt` | Yes | Add ORDER BY queries |
| Pin / favorite documents | 📋 TODO | `data/repository/DocumentRepository.kt` | Yes | Toggle `isFavorite` |
| Recycle bin (soft delete) | 📋 TODO | `data/repository/DocumentRepository.kt` | Yes | Toggle `isInRecycleBin` |
| Folder/tag organization | 📋 TODO | `data/model/Document.kt` | Yes | Tags already in model |
| Batch delete / move | 📋 TODO | `data/db/DocumentDao.kt` | Yes | |
| Page reorder (drag & drop) | 🚫 BLOCKED | `ui/screens/viewer/` | No | Needs design |
| Duplicate scan | 📋 TODO | `data/repository/DocumentRepository.kt` | Yes | |
| Document merge (two docs → one) | 📋 TODO | `data/repository/DocumentRepository.kt` | Yes | Combine pages + re-create PDF |

### Export & Sharing
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| Export PDF | ✅ DONE | `export/ExportManager.kt` | — | Share via FileProvider |
| Quick Share (no save) | 📋 TODO | `export/ExportManager.kt` | Yes | Compress bitmap → share Intent without saving |
| 3 quality presets for share | 📋 TODO | `export/ExportManager.kt` | Yes | HIGH/MEDIUM/COMPRESSED JPEG |
| Share to WhatsApp/Gmail/Telegram | 📋 TODO | `export/ExportManager.kt` | Yes | Share sheet Intent with package filter |
| Export as Markdown | 📋 TODO | `export/ExportManager.kt` | Yes | Uses MarkdownExporter |
| QR code export | 📋 TODO | `export/ExportManager.kt` | Yes | ZXing QR generator |

### Rule-Based "AI" Features (Zero API cost)
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| Document classification (keyword) | ✅ DONE | `utils/DocumentClassifier.kt` | — | |
| Aadhaar extraction (regex) | ✅ DONE | `utils/RegexExtractors.kt` | — | |
| PAN extraction (regex) | ✅ DONE | `utils/RegexExtractors.kt` | — | |
| GST extraction + validation | ✅ DONE | `utils/RegexExtractors.kt` | — | |
| Email + mobile extraction | ✅ DONE | `utils/RegexExtractors.kt` | — | |
| Aadhaar masking (last 8 → XXXX) | ✅ DONE | `utils/RegexExtractors.kt` | — | |
| Markdown export logic | ✅ DONE | `utils/MarkdownExporter.kt` | — | |
| Business card extraction (ML Kit Entity) | 📋 TODO | `ocr/BusinessCardExtractor.kt` | Yes | ML Kit Entity Extraction |
| QR code generator | 📋 TODO | `utils/QrGenerator.kt` | Yes | ZXing MultiFormatWriter |

### Security & Privacy
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| PIN lock (app-level) | 📋 TODO | `ui/screens/settings/` | No | Needs design for PIN entry screen |
| Biometric lock | 📋 TODO | `ui/screens/settings/` | No | Needs design |
| Password-protected PDF | 📋 TODO | `pdf/PdfEditor.kt` | Yes | PDFBox |
| Auto-lock timeout | 📋 TODO | `ui/screens/settings/` | No | Needs design |

### Quick Access
| Feature | Status | Files | Agent Ready | Notes |
|---|---|---|---|---|
| Homescreen widget | 📋 TODO | `widget/ScanWidget.kt` | Yes | AppWidgetProvider |
| Long-press app shortcuts | 📋 TODO | `res/xml/shortcuts.xml` | Yes | Static shortcuts |
| Quick Settings tile | 📋 TODO | `service/ScanTileService.kt` | Yes | TileService |

---

## Phase 2 — Planned (Month 3–4)

| Feature | Notes |
|---|---|
| AdMob rewarded ads integration | AdMob SDK, gate OCR export + high-res export |
| One-time IAP (billing) | Google Play Billing Library |
| Image deblur (OpenCV) | Already in ImageEnhancer scope |
| Low-light enhancement | OpenCV CLAHE |
| Finger removal from scan | OpenCV inpainting (cv2.inpaint) |

---

## 🔴 Deferred — AI API required (DO NOT BUILD)

- AI summarization (needs LLM)
- Ask AI / Chat with document (needs LLM)
- Handwriting OCR beyond ML Kit (needs GOT-OCR2 / SmolDocling)
- Table extraction to XLSX from complex scans (needs layout model)
- Formula/equation recognition
- AI Math Solver
- Translation (revisit Google Translate free tier in Phase 3)
