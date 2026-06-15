package com.abhijit.docscanpro.pdf

import com.tom_roush.pdfbox.multipdf.PDFMergerUtility
import com.tom_roush.pdfbox.multipdf.Splitter
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
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
