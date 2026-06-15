package com.abhijit.docscanpro.utils

object RegexExtractors {

    // Aadhaar: 12 digits, optionally grouped with spaces or hyphens
    private val AADHAAR_PATTERN = Regex("""(?<!\d)(\d{4}[\s\-]?\d{4}[\s\-]?\d{4})(?!\d)""")

    // PAN: 5 uppercase letters + 4 digits + 1 uppercase letter
    private val PAN_PATTERN = Regex("""[A-Z]{5}[0-9]{4}[A-Z]""")

    // GSTIN: 15-char GST identification number
    private val GSTIN_PATTERN = Regex("""[0-3][0-9][A-Z]{5}[0-9]{4}[A-Z][1-9A-Z]Z[0-9A-Z]""")

    // Indian mobile: 10 digits starting with 6–9
    private val MOBILE_PATTERN = Regex("""(?<!\d)[6-9]\d{9}(?!\d)""")

    // Email
    private val EMAIL_PATTERN = Regex("""[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}""")

    // PIN code (India): 6 digits starting with non-zero
    private val PINCODE_PATTERN = Regex("""(?<!\d)[1-9]\d{5}(?!\d)""")

    fun extractAadhaar(text: String): String? {
        return AADHAAR_PATTERN.find(text)?.value?.replace(Regex("""[\s\-]"""), "")
    }

    fun extractPan(text: String): String? {
        return PAN_PATTERN.find(text.uppercase())?.value
    }

    fun extractGstin(text: String): String? {
        val match = GSTIN_PATTERN.find(text.uppercase())?.value ?: return null
        return if (validateGstin(match)) match else null
    }

    fun extractMobileNumbers(text: String): List<String> {
        return MOBILE_PATTERN.findAll(text).map { it.value }.toList()
    }

    fun extractEmails(text: String): List<String> {
        return EMAIL_PATTERN.findAll(text).map { it.value.lowercase() }.toList()
    }

    fun extractPinCodes(text: String): List<String> {
        return PINCODE_PATTERN.findAll(text).map { it.value }.toList()
    }

    fun maskAadhaar(aadhaar: String): String {
        if (aadhaar.length != 12) return aadhaar
        return "XXXX XXXX ${aadhaar.takeLast(4)}"
    }

    fun validateGstin(gstin: String): Boolean {
        if (gstin.length != 15) return false
        val stateCode = gstin.substring(0, 2).toIntOrNull() ?: return false
        if (stateCode < 1 || stateCode > 38) return false
        val chars = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        var sum = 0
        for (i in 0 until 14) {
            val digit = chars.indexOf(gstin[i])
            if (digit == -1) return false
            val product = digit * (if (i % 2 == 0) 1 else 2)
            sum += product / 36 + product % 36
        }
        val checkDigit = chars[(36 - sum % 36) % 36]
        return gstin[14] == checkDigit
    }

    fun extractAll(text: String): ExtractionResult {
        val upper = text.uppercase()
        return ExtractionResult(
            aadhaar = extractAadhaar(text),
            pan = extractPan(upper),
            gstin = extractGstin(upper),
            mobileNumbers = extractMobileNumbers(text),
            emails = extractEmails(text),
            pinCodes = extractPinCodes(text)
        )
    }
}

data class ExtractionResult(
    val aadhaar: String? = null,
    val pan: String? = null,
    val gstin: String? = null,
    val mobileNumbers: List<String> = emptyList(),
    val emails: List<String> = emptyList(),
    val pinCodes: List<String> = emptyList()
) {
    fun hasAnyResult() = aadhaar != null || pan != null || gstin != null ||
        mobileNumbers.isNotEmpty() || emails.isNotEmpty()
}
