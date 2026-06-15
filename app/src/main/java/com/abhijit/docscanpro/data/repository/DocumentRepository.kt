package com.abhijit.docscanpro.data.repository

import com.abhijit.docscanpro.data.db.AppDatabase
import com.abhijit.docscanpro.data.model.Document
import com.abhijit.docscanpro.data.model.Page
import kotlinx.coroutines.flow.Flow

class DocumentRepository(private val db: AppDatabase) {

    private val docDao = db.documentDao()
    private val pageDao = db.pageDao()

    // Documents

    fun getAllDocuments(): Flow<List<Document>> = docDao.getAllDocuments()

    fun getFavorites(): Flow<List<Document>> = docDao.getFavorites()

    fun getRecycleBin(): Flow<List<Document>> = docDao.getRecycleBin()

    fun getDocumentsByFolder(folderPath: String): Flow<List<Document>> =
        docDao.getDocumentsByFolder(folderPath)

    fun getDocumentsByType(type: String): Flow<List<Document>> =
        docDao.getDocumentsByType(type)

    fun searchDocuments(query: String): Flow<List<Document>> =
        docDao.searchDocuments(query)

    fun getTotalDocumentCount(): Flow<Int> = docDao.getTotalDocumentCount()

    suspend fun getDocumentById(id: Long): Document? = docDao.getDocumentById(id)

    suspend fun createDocument(name: String, folderPath: String = ""): Long {
        val doc = Document(name = name, folderPath = folderPath)
        return docDao.insert(doc)
    }

    suspend fun updateDocument(document: Document) = docDao.update(document)

    suspend fun setFavorite(id: Long, isFavorite: Boolean) =
        docDao.setFavorite(id, isFavorite)

    suspend fun moveToRecycleBin(id: Long) = docDao.setInRecycleBin(id, true)

    suspend fun restoreFromRecycleBin(id: Long) = docDao.setInRecycleBin(id, false)

    suspend fun moveToRecycleBinBatch(ids: List<Long>) =
        docDao.setInRecycleBinBatch(ids, true)

    suspend fun permanentlyDelete(document: Document) = docDao.delete(document)

    suspend fun permanentlyDeleteBatch(ids: List<Long>) = docDao.deleteByIds(ids)

    suspend fun updatePdfPath(id: Long, path: String) = docDao.updatePdfPath(id, path)

    suspend fun updateThumbnailPath(id: Long, path: String) = docDao.updateThumbnailPath(id, path)

    suspend fun updateDocumentType(id: Long, type: String) = docDao.updateDocumentType(id, type)

    suspend fun updateSize(id: Long, sizeBytes: Long) = docDao.updateSize(id, sizeBytes)

    // Pages

    fun getPagesForDocument(documentId: Long): Flow<List<Page>> =
        pageDao.getPagesForDocument(documentId)

    suspend fun getPagesOnce(documentId: Long): List<Page> =
        pageDao.getPagesForDocumentOnce(documentId)

    suspend fun addPage(page: Page): Long {
        val id = pageDao.insert(page)
        val count = pageDao.getPageCount(page.documentId)
        docDao.updatePageCount(page.documentId, count)
        return id
    }

    suspend fun addPages(pages: List<Page>) {
        if (pages.isEmpty()) return
        pageDao.insertAll(pages)
        val count = pageDao.getPageCount(pages.first().documentId)
        docDao.updatePageCount(pages.first().documentId, count)
    }

    suspend fun updateOcrText(pageId: Long, text: String) =
        pageDao.updateOcrText(pageId, text)

    suspend fun deletePage(page: Page) {
        pageDao.delete(page)
        val count = pageDao.getPageCount(page.documentId)
        docDao.updatePageCount(page.documentId, count)
    }

    suspend fun reorderPages(documentId: Long, orderedPageIds: List<Long>) {
        orderedPageIds.forEachIndexed { index, pageId ->
            pageDao.updatePageNumber(pageId, index + 1)
        }
    }

    fun searchInOcrText(query: String): Flow<List<Page>> =
        pageDao.searchInOcrText(query)
}
