package com.abhijit.docscanpro.data.db

import androidx.room.*
import com.abhijit.docscanpro.data.model.Document
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(document: Document): Long

    @Update
    suspend fun update(document: Document)

    @Delete
    suspend fun delete(document: Document)

    @Query("DELETE FROM documents WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("SELECT * FROM documents WHERE isInRecycleBin = 0 ORDER BY updatedAt DESC")
    fun getAllDocuments(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE isInRecycleBin = 1 ORDER BY updatedAt DESC")
    fun getRecycleBin(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getDocumentById(id: Long): Document?

    @Query("SELECT * FROM documents WHERE isFavorite = 1 AND isInRecycleBin = 0 ORDER BY updatedAt DESC")
    fun getFavorites(): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE folderPath = :folderPath AND isInRecycleBin = 0 ORDER BY updatedAt DESC")
    fun getDocumentsByFolder(folderPath: String): Flow<List<Document>>

    @Query("SELECT * FROM documents WHERE documentType = :type AND isInRecycleBin = 0 ORDER BY updatedAt DESC")
    fun getDocumentsByType(type: String): Flow<List<Document>>

    @Query("""
        SELECT * FROM documents
        WHERE isInRecycleBin = 0
        AND (name LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun searchDocuments(query: String): Flow<List<Document>>

    @Query("UPDATE documents SET isFavorite = :isFavorite, updatedAt = :now WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET isInRecycleBin = :inBin, updatedAt = :now WHERE id = :id")
    suspend fun setInRecycleBin(id: Long, inBin: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET isInRecycleBin = :inBin, updatedAt = :now WHERE id IN (:ids)")
    suspend fun setInRecycleBinBatch(ids: List<Long>, inBin: Boolean, now: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET pageCount = :count, updatedAt = :now WHERE id = :id")
    suspend fun updatePageCount(id: Long, count: Int, now: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET pdfPath = :path, updatedAt = :now WHERE id = :id")
    suspend fun updatePdfPath(id: Long, path: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET thumbnailPath = :path, updatedAt = :now WHERE id = :id")
    suspend fun updateThumbnailPath(id: Long, path: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET documentType = :type, updatedAt = :now WHERE id = :id")
    suspend fun updateDocumentType(id: Long, type: String, now: Long = System.currentTimeMillis())

    @Query("UPDATE documents SET totalSizeBytes = :size, updatedAt = :now WHERE id = :id")
    suspend fun updateSize(id: Long, size: Long, now: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM documents WHERE isInRecycleBin = 0")
    fun getTotalDocumentCount(): Flow<Int>
}
