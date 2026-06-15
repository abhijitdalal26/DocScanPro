package com.abhijit.docscanpro.data.db

import androidx.room.*
import com.abhijit.docscanpro.data.model.Page
import kotlinx.coroutines.flow.Flow

@Dao
interface PageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(page: Page): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pages: List<Page>)

    @Update
    suspend fun update(page: Page)

    @Delete
    suspend fun delete(page: Page)

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    fun getPagesForDocument(documentId: Long): Flow<List<Page>>

    @Query("SELECT * FROM pages WHERE documentId = :documentId ORDER BY pageNumber ASC")
    suspend fun getPagesForDocumentOnce(documentId: Long): List<Page>

    @Query("SELECT * FROM pages WHERE id = :id")
    suspend fun getPageById(id: Long): Page?

    @Query("DELETE FROM pages WHERE documentId = :documentId")
    suspend fun deletePagesForDocument(documentId: Long)

    @Query("UPDATE pages SET ocrText = :text WHERE id = :id")
    suspend fun updateOcrText(id: Long, text: String)

    @Query("UPDATE pages SET pageNumber = :pageNumber WHERE id = :id")
    suspend fun updatePageNumber(id: Long, pageNumber: Int)

    @Query("SELECT COUNT(*) FROM pages WHERE documentId = :documentId")
    suspend fun getPageCount(documentId: Long): Int

    @Query("""
        SELECT * FROM pages
        WHERE documentId IN (
            SELECT id FROM documents WHERE isInRecycleBin = 0
        )
        AND ocrText LIKE '%' || :query || '%'
    """)
    fun searchInOcrText(query: String): Flow<List<Page>>
}
