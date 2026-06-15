package com.abhijit.docscanpro.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "pages",
    foreignKeys = [
        ForeignKey(
            entity = Document::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["documentId"])]
)
data class Page(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val pageNumber: Int,
    val imagePath: String,
    val ocrText: String? = null,
    val colorMode: String = ColorMode.ORIGINAL.name,
    val imageWidth: Int = 0,
    val imageHeight: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
