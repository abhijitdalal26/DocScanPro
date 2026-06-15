package com.abhijit.docscanpro.utils

import com.abhijit.docscanpro.data.model.DocumentType

object DocumentClassifier {

    private val typeKeywords: Map<DocumentType, List<String>> = mapOf(
        DocumentType.INVOICE to listOf(
            "invoice", "bill", "payment due", "invoice number", "billing address",
            "tax invoice", "gst invoice", "amount due", "due date", "invoice date"
        ),
        DocumentType.RECEIPT to listOf(
            "receipt", "total amount", "subtotal", "cash paid", "change", "transaction id",
            "thank you for your purchase", "payment received", "order total", "grand total"
        ),
        DocumentType.ID_CARD to listOf(
            "aadhaar", "aadhar", "pan card", "driving licence", "driving license",
            "passport", "voter id", "identity card", "date of birth", "dob",
            "unique identification", "uidai", "election commission"
        ),
        DocumentType.CERTIFICATE to listOf(
            "certificate", "this is to certify", "awarded", "completion", "achievement",
            "hereby certify", "in witness whereof", "verified", "degree", "diploma"
        ),
        DocumentType.CONTRACT to listOf(
            "agreement", "contract", "terms and conditions", "party", "signatory",
            "whereas", "hereinafter", "obligations", "termination clause", "breach",
            "indemnify", "governing law", "dispute resolution"
        ),
        DocumentType.FORM to listOf(
            "application form", "applicant name", "fill in", "please complete",
            "date of birth", "gender", "address proof", "nomination", "declaration"
        ),
        DocumentType.LETTER to listOf(
            "dear sir", "dear madam", "to whom it may concern", "sincerely",
            "yours faithfully", "yours truly", "subject:", "ref:", "regards",
            "kind regards", "with reference to"
        ),
        DocumentType.BANK to listOf(
            "bank statement", "account number", "ifsc", "account balance", "closing balance",
            "opening balance", "credit", "debit", "transaction", "cheque", "neft", "rtgs",
            "imps", "upi", "passbook"
        )
    )

    fun classify(ocrText: String): DocumentType {
        if (ocrText.isBlank()) return DocumentType.UNKNOWN
        val lower = ocrText.lowercase()
        val scores = mutableMapOf<DocumentType, Int>()

        for ((type, keywords) in typeKeywords) {
            val score = keywords.count { keyword -> lower.contains(keyword) }
            if (score > 0) scores[type] = score
        }

        return scores.maxByOrNull { it.value }?.key ?: DocumentType.UNKNOWN
    }

    fun classifyWithConfidence(ocrText: String): Pair<DocumentType, Float> {
        if (ocrText.isBlank()) return Pair(DocumentType.UNKNOWN, 0f)
        val lower = ocrText.lowercase()
        val scores = mutableMapOf<DocumentType, Int>()

        for ((type, keywords) in typeKeywords) {
            val score = keywords.count { keyword -> lower.contains(keyword) }
            if (score > 0) scores[type] = score
        }

        if (scores.isEmpty()) return Pair(DocumentType.UNKNOWN, 0f)
        val best = scores.maxByOrNull { it.value }!!
        val totalKeywords = typeKeywords[best.key]?.size ?: 1
        val confidence = (best.value.toFloat() / totalKeywords).coerceIn(0f, 1f)
        return Pair(best.key, confidence)
    }
}
