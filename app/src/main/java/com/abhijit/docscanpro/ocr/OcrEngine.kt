package com.abhijit.docscanpro.ocr

import android.graphics.Bitmap
import android.graphics.Rect
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class OcrEngine {

    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(bitmap: Bitmap): OcrResult {
        val image = InputImage.fromBitmap(bitmap, 0)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    cont.resume(parseVisionText(visionText))
                }
                .addOnFailureListener { e ->
                    cont.resumeWithException(e)
                }
        }
    }

    fun recognizeTextAsync(
        bitmap: Bitmap,
        onSuccess: (OcrResult) -> Unit,
        onFailure: (Exception) -> Unit
    ) {
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { visionText -> onSuccess(parseVisionText(visionText)) }
            .addOnFailureListener(onFailure)
    }

    private fun parseVisionText(visionText: Text): OcrResult {
        val blocks = visionText.textBlocks.map { block ->
            OcrBlock(
                text = block.text,
                boundingBox = block.boundingBox,
                lines = block.lines.map { line ->
                    OcrLine(
                        text = line.text,
                        boundingBox = line.boundingBox,
                        elements = line.elements.map { el ->
                            OcrElement(text = el.text, boundingBox = el.boundingBox)
                        }
                    )
                }
            )
        }
        return OcrResult(fullText = visionText.text, blocks = blocks)
    }

    fun close() = recognizer.close()
}

data class OcrResult(
    val fullText: String,
    val blocks: List<OcrBlock> = emptyList()
) {
    fun isEmpty() = fullText.isBlank()
}

data class OcrBlock(
    val text: String,
    val boundingBox: Rect?,
    val lines: List<OcrLine> = emptyList()
)

data class OcrLine(
    val text: String,
    val boundingBox: Rect?,
    val elements: List<OcrElement> = emptyList()
)

data class OcrElement(
    val text: String,
    val boundingBox: Rect?
)
