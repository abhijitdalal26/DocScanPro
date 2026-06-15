package com.abhijit.docscanpro.utils

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.WriterException

object QrGenerator {

    fun generateQr(
        content: String,
        sizePx: Int = 512,
        foregroundColor: Int = Color.BLACK,
        backgroundColor: Int = Color.WHITE
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val matrix = MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx, hints)
            val pixels = IntArray(sizePx * sizePx) { i ->
                if (matrix[i % sizePx, i / sizePx]) foregroundColor else backgroundColor
            }
            Bitmap.createBitmap(pixels, sizePx, sizePx, Bitmap.Config.RGB_565)
        } catch (e: WriterException) {
            null
        }
    }

    fun generateBarcode(
        content: String,
        format: BarcodeFormat = BarcodeFormat.CODE_128,
        widthPx: Int = 600,
        heightPx: Int = 200
    ): Bitmap? {
        if (content.isBlank()) return null
        return try {
            val hints = mapOf(EncodeHintType.CHARACTER_SET to "UTF-8")
            val matrix = MultiFormatWriter().encode(content, format, widthPx, heightPx, hints)
            val pixels = IntArray(widthPx * heightPx) { i ->
                if (matrix[i % widthPx, i / widthPx]) Color.BLACK else Color.WHITE
            }
            Bitmap.createBitmap(pixels, widthPx, heightPx, Bitmap.Config.RGB_565)
        } catch (e: WriterException) {
            null
        }
    }
}
