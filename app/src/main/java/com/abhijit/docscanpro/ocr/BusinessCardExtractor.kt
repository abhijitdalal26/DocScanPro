package com.abhijit.docscanpro.ocr

import android.content.Context
import com.google.mlkit.nl.entityextraction.*
import com.abhijit.docscanpro.utils.RegexExtractors
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class BusinessCard(
    val name: String? = null,
    val phones: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val urls: List<String> = emptyList(),
    val address: String? = null,
    val organization: String? = null,
    val jobTitle: String? = null,
    val rawText: String = ""
) {
    fun isEmpty() = name == null && phones.isEmpty() && emails.isEmpty()

    fun toVCard(): String {
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCARD")
        sb.appendLine("VERSION:3.0")
        name?.let { sb.appendLine("FN:$it") }
        organization?.let { sb.appendLine("ORG:$it") }
        jobTitle?.let { sb.appendLine("TITLE:$it") }
        phones.forEach { sb.appendLine("TEL:$it") }
        emails.forEach { sb.appendLine("EMAIL:$it") }
        urls.forEach { sb.appendLine("URL:$it") }
        address?.let { sb.appendLine("ADR:;;$it;;;;") }
        sb.appendLine("END:VCARD")
        return sb.toString()
    }
}

class BusinessCardExtractor(context: Context) {

    private val extractor = EntityExtraction.getClient(
        EntityExtractorOptions.Builder(EntityExtractorOptions.ENGLISH).build()
    )

    private var modelReady = false

    init {
        extractor.downloadModelIfNeeded()
            .addOnSuccessListener { modelReady = true }
    }

    suspend fun extract(ocrText: String): BusinessCard {
        if (ocrText.isBlank()) return BusinessCard()

        // Always run regex extractors (no model needed)
        val phones = RegexExtractors.extractMobileNumbers(ocrText)
        val emails = RegexExtractors.extractEmails(ocrText)

        // URL regex
        val urlPattern = Regex("""(?:https?://|www\.)\S+""")
        val urls = urlPattern.findAll(ocrText).map { it.value }.toList()

        // Name heuristic: first line of the card that's 2–4 words, title-cased, no digits
        val name = guessName(ocrText)

        // Organization heuristic: look for keywords like Pvt Ltd, Inc, Co., LLP
        val organization = guessOrganization(ocrText)

        // Job title: lines with common title keywords
        val jobTitle = guessJobTitle(ocrText)

        // ML Kit entity extraction for additional signals (if model is ready)
        val mlKitEntities = if (modelReady) {
            runCatching { extractWithMlKit(ocrText) }.getOrDefault(emptyList())
        } else emptyList()

        val mlPhones = mlKitEntities.filter { it.types.contains(Entity.TYPE_PHONE) }
            .map { it.annotatedString.string }
        val mlEmails = mlKitEntities.filter { it.types.contains(Entity.TYPE_EMAIL) }
            .map { it.annotatedString.string }
        val mlAddress = mlKitEntities.filter { it.types.contains(Entity.TYPE_ADDRESS) }
            .firstOrNull()?.annotatedString?.string

        return BusinessCard(
            name = name,
            phones = (phones + mlPhones).distinct(),
            emails = (emails + mlEmails).distinct().map { it.lowercase() },
            urls = urls,
            address = mlAddress,
            organization = organization,
            jobTitle = jobTitle,
            rawText = ocrText
        )
    }

    private suspend fun extractWithMlKit(text: String): List<EntityAnnotation> {
        return suspendCancellableCoroutine { cont ->
            val params = EntityExtractionParams.Builder(text).build()
            extractor.annotate(params)
                .addOnSuccessListener { cont.resume(it) }
                .addOnFailureListener { cont.resumeWithException(it) }
        }
    }

    private fun guessName(text: String): String? {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines.take(4)) {
            val words = line.split(" ").filter { it.isNotEmpty() }
            if (words.size in 2..4 &&
                words.all { w -> w[0].isUpperCase() && w.none { it.isDigit() } } &&
                !line.contains("@") && !line.contains("http")
            ) {
                return line
            }
        }
        return null
    }

    private fun guessOrganization(text: String): String? {
        val orgKeywords = Regex("""(?i)\b(?:pvt\.?\s*ltd|private\s+limited|inc\.?|llp|llc|corp\.?|co\.?\s*ltd|technologies|solutions|consultants|services|group|associates)\b""")
        return text.split("\n").firstOrNull { orgKeywords.containsMatchIn(it) }?.trim()
    }

    private fun guessJobTitle(text: String): String? {
        val titleKeywords = Regex("""(?i)\b(?:ceo|cto|coo|cfo|founder|co-founder|director|manager|engineer|developer|designer|consultant|analyst|executive|officer|head\s+of|vp\s+of|vice\s+president|president|lead)\b""")
        return text.split("\n").firstOrNull { titleKeywords.containsMatchIn(it) }?.trim()
    }

    fun close() = extractor.close()
}
