package com.abhijit.docscanpro.export

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import com.abhijit.docscanpro.pdf.PdfQuality
import com.abhijit.docscanpro.utils.FileUtils
import com.abhijit.docscanpro.utils.MarkdownExporter
import java.io.File
import java.io.FileOutputStream

class ExportManager(private val context: Context) {

    private val markdownExporter = MarkdownExporter()

    fun shareFile(file: File, mimeType: String) {
        val uri = FileUtils.getContentUri(context, file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun shareMultipleFiles(files: List<File>, mimeType: String) {
        val uris = ArrayList(files.map { FileUtils.getContentUri(context, it) })
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = mimeType
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Share via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun quickShareBitmap(bitmap: Bitmap, quality: ShareQuality): Intent {
        val cacheFile = File(FileUtils.getExportCacheDir(context), "quick_share_${System.currentTimeMillis()}.jpg")
        FileOutputStream(cacheFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality.jpegQuality, out)
        }
        val uri = FileUtils.getContentUri(context, cacheFile)
        return Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    fun copyTextToClipboard(text: String, label: String = "DocScan Pro") {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    }

    fun exportToTxt(ocrText: String, outputPath: String): Result<File> {
        return try {
            val file = File(outputPath)
            file.parentFile?.mkdirs()
            file.writeText(ocrText, Charsets.UTF_8)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun exportToMarkdown(ocrText: String, documentName: String, outputPath: String): Result<File> {
        return try {
            val markdown = markdownExporter.convertToMarkdown(ocrText, documentName)
            val file = File(outputPath)
            file.parentFile?.mkdirs()
            file.writeText(markdown, Charsets.UTF_8)
            Result.success(file)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun copyMarkdownToClipboard(ocrText: String, documentName: String) {
        val markdown = markdownExporter.convertToMarkdown(ocrText, documentName)
        copyTextToClipboard(markdown, "Markdown — $documentName")
    }

    fun buildShareIntent(file: File, mimeType: String): Intent {
        val uri = FileUtils.getContentUri(context, file)
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
}

enum class ShareQuality(val jpegQuality: Int, val label: String) {
    HIGH(95, "High"),
    MEDIUM(75, "Medium"),
    COMPRESSED(50, "Compressed (< 1MB)")
}
