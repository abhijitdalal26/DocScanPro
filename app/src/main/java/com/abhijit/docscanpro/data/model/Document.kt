package com.abhijit.docscanpro.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val folderPath: String = "",
    val pageCount: Int = 0,
    val thumbnailPath: String? = null,
    val pdfPath: String? = null,
    val tags: String = "",
    val documentType: String = DocumentType.UNKNOWN.name,
    val ocrLanguage: String = "en",
    val isLocked: Boolean = false,
    val lockType: String = LockType.NONE.name,
    val isFavorite: Boolean = false,
    val isInRecycleBin: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val totalSizeBytes: Long = 0
) {
    fun tagList(): List<String> = if (tags.isBlank()) emptyList() else tags.split(",").map { it.trim() }
}

enum class DocumentType(val displayName: String) {
    INVOICE("Invoice"),
    RECEIPT("Receipt"),
    ID_CARD("ID Card"),
    CERTIFICATE("Certificate"),
    CONTRACT("Contract"),
    FORM("Form"),
    LETTER("Letter"),
    BANK("Bank Statement"),
    UNKNOWN("Document")
}

enum class LockType { NONE, PIN, BIOMETRIC }

enum class ColorMode { ORIGINAL, BLACK_WHITE, GRAYSCALE, MAGIC_COLOR, ENHANCED }
