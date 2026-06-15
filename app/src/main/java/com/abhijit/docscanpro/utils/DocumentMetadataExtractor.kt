package com.abhijit.docscanpro.utils

import com.abhijit.docscanpro.data.model.DocumentType

data class DocumentMetadata(
    val documentType: DocumentType,
    val typeConfidence: Float,
    val aadhaar: String? = null,
    val aadhaarMasked: String? = null,
    val pan: String? = null,
    val gstin: String? = null,
    val gstinValid: Boolean = false,
    val mobileNumbers: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val pinCodes: List<String> = emptyList(),
    val dates: List<String> = emptyList(),
    val amounts: List<String> = emptyList()
) {
    fun hasIdFields() = aadhaar != null || pan != null
    fun hasFinancialFields() = gstin != null || amounts.isNotEmpty()
    fun hasSummary() = documentType != DocumentType.UNKNOWN || hasIdFields() || hasFinancialFields()
}

object DocumentMetadataExtractor {

    // Common date formats: DD/MM/YYYY, YYYY-MM-DD, 15 Jun 2024, etc.
    private val DATE_PATTERNS = listOf(
        Regex("""\b\d{2}[/\-]\d{2}[/\-]\d{4}\b"""),
        Regex("""\b\d{4}[/\-]\d{2}[/\-]\d{2}\b"""),
        Regex("""\b\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{4}\b""", RegexOption.IGNORE_CASE),
        Regex("""\b(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)[a-z]*\.?\s+\d{1,2},?\s+\d{4}\b""", RegexOption.IGNORE_CASE)
    )

    // Currency amounts: ₹1,234.56 / Rs. 500 / INR 2000 / $99.99
    private val AMOUNT_PATTERN = Regex("""(?:₹|Rs\.?\s*|INR\s*|\$|€|£)\s*[\d,]+(?:\.\d{1,2})?""")

    fun extract(ocrText: String): DocumentMetadata {
        if (ocrText.isBlank()) return DocumentMetadata(DocumentType.UNKNOWN, 0f)

        val (type, confidence) = DocumentClassifier.classifyWithConfidence(ocrText)
        val extraction = RegexExtractors.extractAll(ocrText)

        val dates = DATE_PATTERNS.flatMap { it.findAll(ocrText).map { m -> m.value } }.distinct()
        val amounts = AMOUNT_PATTERN.findAll(ocrText).map { it.value.trim() }.toList().distinct()

        val aadhaar = extraction.aadhaar
        return DocumentMetadata(
            documentType = type,
            typeConfidence = confidence,
            aadhaar = aadhaar,
            aadhaarMasked = aadhaar?.let { RegexExtractors.maskAadhaar(it) },
            pan = extraction.pan,
            gstin = extraction.gstin,
            gstinValid = extraction.gstin?.let { RegexExtractors.validateGstin(it) } ?: false,
            mobileNumbers = extraction.mobileNumbers,
            emails = extraction.emails,
            pinCodes = extraction.pinCodes,
            dates = dates,
            amounts = amounts
        )
    }
}
