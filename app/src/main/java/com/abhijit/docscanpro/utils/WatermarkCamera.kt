package com.abhijit.docscanpro.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.location.Location
import android.location.LocationManager
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Stamps a scanned bitmap with timestamp + optional GPS coordinates.
 * Fully on-device, no API needed. Used for "Watermark Camera" feature.
 */
object WatermarkCamera {

    data class WatermarkOptions(
        val showTimestamp: Boolean = true,
        val showGps: Boolean = false,
        val showDeviceName: Boolean = false,
        val position: WatermarkPosition = WatermarkPosition.BOTTOM_RIGHT,
        val fontSize: Float = 32f,
        val opacity: Int = 200  // 0–255
    )

    enum class WatermarkPosition { TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT }

    fun stamp(
        bitmap: Bitmap,
        context: Context,
        options: WatermarkOptions = WatermarkOptions()
    ): Bitmap {
        val output = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(output)

        val lines = buildLines(context, options)
        if (lines.isEmpty()) return output

        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = options.fontSize
            typeface = Typeface.MONOSPACE
            alpha = options.opacity
        }
        val bgPaint = Paint().apply {
            color = Color.BLACK
            alpha = (options.opacity * 0.6f).toInt()
        }

        val lineHeight = options.fontSize * 1.4f
        val padding = options.fontSize * 0.5f
        val blockWidth = lines.maxOf { textPaint.measureText(it) } + padding * 2
        val blockHeight = lines.size * lineHeight + padding * 2

        val (x, y) = when (options.position) {
            WatermarkPosition.TOP_LEFT -> Pair(padding, padding)
            WatermarkPosition.TOP_RIGHT -> Pair(output.width - blockWidth - padding, padding)
            WatermarkPosition.BOTTOM_LEFT -> Pair(padding, output.height - blockHeight - padding)
            WatermarkPosition.BOTTOM_RIGHT -> Pair(output.width - blockWidth - padding, output.height - blockHeight - padding)
        }

        canvas.drawRect(x, y, x + blockWidth, y + blockHeight, bgPaint)

        lines.forEachIndexed { index, line ->
            canvas.drawText(line, x + padding, y + padding + (index + 1) * lineHeight - textPaint.descent(), textPaint)
        }

        return output
    }

    private fun buildLines(context: Context, options: WatermarkOptions): List<String> {
        val lines = mutableListOf<String>()
        if (options.showTimestamp) {
            val fmt = SimpleDateFormat("dd MMM yyyy  HH:mm:ss", Locale.ENGLISH)
            lines.add(fmt.format(Date()))
        }
        if (options.showGps) {
            val location = getLastKnownLocation(context)
            if (location != null) {
                val lat = String.format("%.5f", location.latitude)
                val lon = String.format("%.5f", location.longitude)
                lines.add("$lat, $lon")
            }
        }
        if (options.showDeviceName) {
            lines.add(android.os.Build.MODEL)
        }
        return lines
    }

    private fun getLastKnownLocation(context: Context): Location? {
        return try {
            val lm = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = lm.getProviders(true)
            var best: Location? = null
            for (provider in providers) {
                @Suppress("MissingPermission")
                val loc = lm.getLastKnownLocation(provider) ?: continue
                if (best == null || loc.accuracy < best.accuracy) best = loc
            }
            best
        } catch (e: Exception) {
            null
        }
    }
}
