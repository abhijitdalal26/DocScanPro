# Architecture

## Data flow

```
Camera (CameraX)
    ↓
CameraManager.capturePhoto() → Bitmap
    ↓
[Optional] OpenCV processing (edge detect, perspective correct, color mode)
    ↓
FileUtils.saveBitmap() → JPEG on internal storage
    ↓
OcrEngine.recognizeText() → OcrResult (text + bounding boxes)
    ↓
DocumentRepository.savePage() → Room DB (Page entity)
    ↓
PdfCreator.createPdfFromImages() → PDF file
    ↓
ExportManager.share() / FileUtils.getContentUri() → share sheet
```

## Room schema

```
documents (Document)
  id            LONG PK autoGenerate
  name          TEXT
  folderPath    TEXT
  pageCount     INT
  thumbnailPath TEXT nullable
  pdfPath       TEXT nullable
  tags          TEXT (comma-separated)
  documentType  TEXT (DocumentType enum name)
  isLocked      BOOLEAN
  lockType      TEXT (NONE | PIN | BIOMETRIC)
  isFavorite    BOOLEAN
  isInRecycleBin BOOLEAN
  createdAt     LONG (epoch ms)
  updatedAt     LONG (epoch ms)
  totalSizeBytes LONG

pages (Page)
  id            LONG PK autoGenerate
  documentId    LONG FK→documents.id CASCADE DELETE
  pageNumber    INT
  imagePath     TEXT
  ocrText       TEXT nullable
  colorMode     TEXT (ColorMode enum name)
  createdAt     LONG (epoch ms)
```

## File storage layout

All files live in app internal storage (no external storage required):

```
/data/data/com.abhijit.docscanpro/files/
  documents/
    {documentId}/
      page_1.jpg
      page_2.jpg
      ...
      {documentName}.pdf
      thumbnail.jpg
```

## Key design decisions

**No external storage permission** — storing in `context.filesDir` means no READ/WRITE_EXTERNAL_STORAGE needed on API 29+. Files shared via FileProvider URI.

**Single Activity + NavGraph** — MainActivity hosts one NavHost. All screens are Composable destinations.

**Repository pattern** — all DB access goes through `DocumentRepository`. ViewModels never touch DAOs directly.

**StateFlow for UI state** — each ViewModel exposes a single `UiState` data class via `StateFlow`. Screens collect it and render.

**No Hilt yet** — manual dependency injection for now. Repository gets the database instance passed in. Add Hilt in Phase 2 if the wiring becomes complex.

**PDF creation strategy:**
- Android's built-in `android.graphics.pdf.PdfDocument` for creating new PDFs from images (no library size cost)
- PDFBox-Android for merge, split, password protection, compression

**OCR strategy:**
- ML Kit Text Recognition v2 (Latin) handles most global languages, runs fully on-device
- ML Kit Devanagari module added for Hindi and Indian scripts
- Entity Extraction (on-device) for business card fields (name, phone, email, URL)

**Color modes (OpenCV on-device):**
- Original: raw bitmap as-is
- B&W: threshold → binary image
- Grayscale: `Imgproc.cvtColor(mat, mat, Imgproc.COLOR_BGR2GRAY)`
- Magic Color: adaptive thresholding for clean document look
- Enhanced: CLAHE (Contrast Limited Adaptive Histogram Equalization)

## Permissions model

| Permission | When requested |
|---|---|
| `CAMERA` | On first launch / on tap of scan button |
| `USE_BIOMETRIC` | Only when user enables biometric lock in settings |
| `INTERNET` | Not requested (AdMob handles its own network) |
| External storage | Not needed (internal storage only) |
