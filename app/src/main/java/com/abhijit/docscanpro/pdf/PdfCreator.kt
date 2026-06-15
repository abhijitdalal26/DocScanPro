package com.abhijit.docscanpro.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.abhijit.docscanpro.utils.FileUtils
import java.io.File
import java.io.FileOutputStream

class PdfCreator(private val context: Context) {

    fun createPdfFromImages(
        images: List<Bitmap>,
        outputPath: String,
        quality: PdfQuality = PdfQuality.HIGH
    ): Result<File> {
        if (images.isEmpty()) return Result.failure(IllegalArgumentException("No images provided"))
        return try {
            val document = PdfDocument()
            val scaledImages = images.map { bitmap ->
                if (quality == PdfQuality.HIGH) bitmap
                else scaleBitmap(bitmap, quality.scaleFactor)
            }

            scaledImages.forEachIndexed { index, bitmap ->
                val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
                val page = document.startPage(pageInfo)
                val paint = Paint(Paint.FILTER_BITMAP_FLAG)
                page.canvas.drawBitmap(bitmap, 0f, 0f, paint)
                document.finishPage(page)
            }

            val file = File(outputPath)
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { document.writeTo(it) }
            document.close()

            scaledImages.forEach { if (it != images[scaledImages.indexOf(it)]) it.recycle() }

            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun createThumbnail(bitmap: Bitmap, outputPath: String, maxSize: Int = 400): Result<File> {
        return try {
            val thumbnail = scaleBitmapToFit(bitmap, maxSize)
            val saved = FileUtils.saveBitmap(thumbnail, outputPath, quality = 80)
            if (thumbnail != bitmap) thumbnail.recycle()
            if (saved) Result.success(File(outputPath))
            else Result.failure(Exception("Failed to save thumbnail"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun scaleBitmap(bitmap: Bitmap, factor: Float): Bitmap {
        val newWidth = (bitmap.width * factor).toInt()
        val newHeight = (bitmap.height * factor).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun scaleBitmapToFit(bitmap: Bitmap, maxSize: Int): Bitmap {
        val ratio = minOf(maxSize.toFloat() / bitmap.width, maxSize.toFloat() / bitmap.height)
        if (ratio >= 1f) return bitmap
        val newWidth = (bitmap.width * ratio).toInt()
        val newHeight = (bitmap.height * ratio).toInt()
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}

enum class PdfQuality(val scaleFactor: Float, val label: String) {
    HIGH(1.0f, "High"),
    MEDIUM(0.75f, "Medium"),
    COMPRESSED(0.5f, "Compressed"),
    MAXIMUM_COMPRESSION(0.35f, "Max Compression")
}
