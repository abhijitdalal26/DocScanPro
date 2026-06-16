package com.abhijit.docscanpro.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import com.abhijit.docscanpro.utils.FileUtils
import java.io.File
import java.io.FileInputStream

class PdfEditor {

    fun mergePdfs(inputPaths: List<String>, outputPath: String): Result<File> {
        if (inputPaths.isEmpty()) return Result.failure(IllegalArgumentException("No input files"))
        return try {
            val merger = PDFMergerUtility()
            merger.destinationFileName = outputPath
            inputPaths.forEach { path -> merger.addSource(File(path)) }
            merger.mergeDocuments(null)
            Result.success(File(outputPath))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun splitPdf(inputPath: String, outputDir: String): Result<List<File>> {
        return try {
            val document = PDDocument.load(File(inputPath))
            val splitter = Splitter()
            val pages = splitter.split(document)
            val outputFiles = mutableListOf<File>()

            pages.forEachIndexed { index, pageDoc ->
                val outputFile = File(outputDir, "page_${index + 1}.pdf")
                pageDoc.save(outputFile)
                pageDoc.close()
                outputFiles.add(outputFile)
            }
            document.close()
            Result.success(outputFiles)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun extractPages(inputPath: String, outputPath: String, fromPage: Int, toPage: Int): Result<File> {
        return try {
            val document = PDDocument.load(File(inputPath))
            val splitter = Splitter().apply {
                setStartPage(fromPage)
                setEndPage(toPage)
                setSplitAtPage(toPage - fromPage + 1)
            }
            val pages = splitter.split(document)
            val outputFile = File(outputPath)
            if (pages.isNotEmpty()) {
                pages.first().save(outputFile)
                pages.forEach { it.close() }
            }
            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Convenience overload: same password for owner and user
    fun protectWithPassword(inputPath: String, outputPath: String, password: String): Result<File> =
        protectWithPassword(inputPath, outputPath, password, password)

    fun protectWithPassword(
        inputPath: String,
        outputPath: String,
        ownerPassword: String,
        userPassword: String
    ): Result<File> {
        return try {
            val document = PDDocument.load(File(inputPath))
            val permissions = AccessPermission().apply {
                setCanPrint(true)
                setCanExtractContent(false)
                setCanModify(false)
            }
            val policy = StandardProtectionPolicy(ownerPassword, userPassword, permissions).apply {
                encryptionKeyLength = 256
            }
            document.protect(policy)
            val outputFile = File(outputPath)
            document.save(outputFile)
            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun removePassword(inputPath: String, outputPath: String, password: String): Result<File> {
        return try {
            val document = PDDocument.load(FileInputStream(File(inputPath)), password)
            document.isAllSecurityToBeRemoved = true
            val outputFile = File(outputPath)
            document.save(outputFile)
            document.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Recompresses all pages of a PDF at a lower JPEG quality to reduce file size.
     * Uses Android PdfRenderer to render each page → re-encode as JPEG → rebuild PDF.
     */
    fun compressPdf(context: Context, inputPath: String, outputPath: String, jpegQuality: Int = 60): Result<File> {
        return try {
            val inputFile = File(inputPath)
            val pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pdfDoc = android.graphics.pdf.PdfDocument()

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val pdfPage = pdfDoc.startPage(
                    android.graphics.pdf.PdfDocument.PageInfo.Builder(page.width * 2, page.height * 2, i + 1).create()
                )
                // Re-encode at lower quality via canvas drawBitmap
                val tempFile = File(context.cacheDir, "compress_page_$i.jpg")
                tempFile.outputStream().use { bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, it) }
                val reDecoded = android.graphics.BitmapFactory.decodeFile(tempFile.absolutePath)
                pdfPage.canvas.drawBitmap(reDecoded, 0f, 0f, null)
                pdfDoc.finishPage(pdfPage)
                bitmap.recycle()
                reDecoded?.recycle()
                tempFile.delete()
            }

            renderer.close()
            pfd.close()

            val outputFile = File(outputPath)
            pdfDoc.writeTo(outputFile.outputStream())
            pdfDoc.close()
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addPageNumbers(context: Context, inputPath: String, outputPath: String): Result<File> {
        return try {
            val inputFile = File(inputPath)
            val pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pdfDoc = android.graphics.pdf.PdfDocument()
            val paint = Paint().apply {
                color = android.graphics.Color.DKGRAY
                textSize = 36f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val w = page.width * 2; val h = page.height * 2
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                Canvas(bmp).drawText(
                    "${i + 1} / ${renderer.pageCount}",
                    w / 2f, h - 24f, paint
                )

                val pdfPage = pdfDoc.startPage(
                    android.graphics.pdf.PdfDocument.PageInfo.Builder(w, h, i + 1).create()
                )
                pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                pdfDoc.finishPage(pdfPage)
                bmp.recycle()
            }

            renderer.close(); pfd.close()
            val out = File(outputPath)
            pdfDoc.writeTo(out.outputStream())
            pdfDoc.close()
            Result.success(out)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun addWatermarkToPdf(
        context: Context,
        inputPath: String,
        outputPath: String,
        watermarkText: String,
        opacity: Int = 60
    ): Result<File> {
        return try {
            val inputFile = File(inputPath)
            val pfd = ParcelFileDescriptor.open(inputFile, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val pdfDoc = android.graphics.pdf.PdfDocument()
            val paint = Paint().apply {
                color = android.graphics.Color.argb(opacity, 128, 128, 128)
                textSize = 80f
                typeface = Typeface.DEFAULT_BOLD
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val w = page.width * 2; val h = page.height * 2
                val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val canvas = Canvas(bmp)
                canvas.save()
                canvas.rotate(-45f, w / 2f, h / 2f)
                canvas.drawText(watermarkText, w / 2f, h / 2f, paint)
                canvas.restore()

                val pdfPage = pdfDoc.startPage(
                    android.graphics.pdf.PdfDocument.PageInfo.Builder(w, h, i + 1).create()
                )
                pdfPage.canvas.drawBitmap(bmp, 0f, 0f, null)
                pdfDoc.finishPage(pdfPage)
                bmp.recycle()
            }

            renderer.close(); pfd.close()
            val out = File(outputPath)
            pdfDoc.writeTo(out.outputStream())
            pdfDoc.close()
            Result.success(out)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun extractPagesToImages(context: Context, inputPath: String, outputDir: String): Result<List<File>> {
        return try {
            val pfd = ParcelFileDescriptor.open(File(inputPath), ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val files = mutableListOf<File>()

            for (i in 0 until renderer.pageCount) {
                val page = renderer.openPage(i)
                val bmp = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                page.close()

                val file = File(outputDir, "page_${i + 1}.jpg")
                file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 90, it) }
                bmp.recycle()
                files.add(file)
            }

            renderer.close()
            pfd.close()
            Result.success(files)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPageCount(inputPath: String): Int {
        return try {
            val document = PDDocument.load(File(inputPath))
            val count = document.numberOfPages
            document.close()
            count
        } catch (e: Exception) {
            -1
        }
    }
}
