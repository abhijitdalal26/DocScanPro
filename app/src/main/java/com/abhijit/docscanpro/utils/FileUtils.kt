package com.abhijit.docscanpro.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream

object FileUtils {

    fun getDocumentsDir(context: Context): File =
        File(context.filesDir, "documents").also { it.mkdirs() }

    fun getDocumentDir(context: Context, documentId: Long): File =
        File(getDocumentsDir(context), documentId.toString()).also { it.mkdirs() }

    fun getPageImagePath(context: Context, documentId: Long, pageNumber: Int): String =
        File(getDocumentDir(context, documentId), "page_$pageNumber.jpg").absolutePath

    fun getThumbnailPath(context: Context, documentId: Long): String =
        File(getDocumentDir(context, documentId), "thumbnail.jpg").absolutePath

    fun getPdfPath(context: Context, documentId: Long, documentName: String): String =
        File(getDocumentDir(context, documentId), "${sanitizeFileName(documentName)}.pdf").absolutePath

    fun getExportCacheDir(context: Context): File =
        File(context.cacheDir, "export").also { it.mkdirs() }

    fun saveBitmap(bitmap: Bitmap, path: String, quality: Int = 90): Boolean {
        return try {
            val file = File(path)
            file.parentFile?.mkdirs()
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    fun getContentUri(context: Context, file: File): Uri =
        FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

    fun deleteDocumentFiles(context: Context, documentId: Long) {
        getDocumentDir(context, documentId).deleteRecursively()
    }

    fun getFileSizeBytes(path: String): Long = File(path).length()

    fun formatFileSize(bytes: Long): String = when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }

    fun sanitizeFileName(name: String): String =
        name.replace(Regex("""[\\/:*?"<>|]"""), "_").trim()

    fun getDocumentTotalSize(context: Context, documentId: Long): Long =
        getDocumentDir(context, documentId).walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
}
