package com.abhijit.docscanpro.camera

import android.graphics.Bitmap
import android.graphics.PointF
import com.abhijit.docscanpro.data.model.ColorMode
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

/**
 * All on-device image enhancement using OpenCV.
 * No API calls, no models — runs fully offline.
 */
object ImageEnhancer {

    // ─── Perspective Correction ───────────────────────────────────────────────

    /**
     * Corrects perspective distortion given 4 corner points (TL, TR, BR, BL).
     * This is the core operation that makes a tilted photo look like a flat scan.
     */
    fun correctPerspective(bitmap: Bitmap, border: DocumentBorderDetector.DocumentBorder): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val tl = border.topLeft
        val tr = border.topRight
        val br = border.bottomRight
        val bl = border.bottomLeft

        val widthA = distance(br, bl)
        val widthB = distance(tr, tl)
        val outputWidth = maxOf(widthA, widthB).toInt()

        val heightA = distance(tr, br)
        val heightB = distance(tl, bl)
        val outputHeight = maxOf(heightA, heightB).toInt()

        val srcPoints = MatOfPoint2f(
            Point(tl.x.toDouble(), tl.y.toDouble()),
            Point(tr.x.toDouble(), tr.y.toDouble()),
            Point(br.x.toDouble(), br.y.toDouble()),
            Point(bl.x.toDouble(), bl.y.toDouble())
        )
        val dstPoints = MatOfPoint2f(
            Point(0.0, 0.0),
            Point(outputWidth.toDouble(), 0.0),
            Point(outputWidth.toDouble(), outputHeight.toDouble()),
            Point(0.0, outputHeight.toDouble())
        )

        val transform = Imgproc.getPerspectiveTransform(srcPoints, dstPoints)
        val result = Mat()
        Imgproc.warpPerspective(src, result, transform, Size(outputWidth.toDouble(), outputHeight.toDouble()))

        val output = Bitmap.createBitmap(outputWidth, outputHeight, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(result, output)
        return output
    }

    // ─── Color Modes ─────────────────────────────────────────────────────────

    fun applyColorMode(bitmap: Bitmap, mode: ColorMode): Bitmap = when (mode) {
        ColorMode.ORIGINAL -> bitmap
        ColorMode.GRAYSCALE -> toGrayscale(bitmap)
        ColorMode.BLACK_WHITE -> toBinaryBlackWhite(bitmap)
        ColorMode.MAGIC_COLOR -> toMagicColor(bitmap)
        ColorMode.ENHANCED -> toEnhanced(bitmap)
    }

    /** Converts to grayscale */
    fun toGrayscale(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.cvtColor(gray, src, Imgproc.COLOR_GRAY2BGRA)
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(src, output)
        return output
    }

    /**
     * Binary B&W mode using adaptive thresholding (Sauvola-like).
     * Produces clean black text on white background — best for OCR preprocessing.
     */
    fun toBinaryBlackWhite(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        // Apply bilateral filter to reduce noise while preserving text edges
        val smoothed = Mat()
        Imgproc.bilateralFilter(gray, smoothed, 9, 75.0, 75.0)

        // Adaptive threshold — works well for documents with uneven lighting
        val binary = Mat()
        Imgproc.adaptiveThreshold(
            smoothed, binary, 255.0,
            Imgproc.ADAPTIVE_THRESH_GAUSSIAN_C,
            Imgproc.THRESH_BINARY,
            11, 2.0
        )

        Imgproc.cvtColor(binary, src, Imgproc.COLOR_GRAY2BGRA)
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(src, output)
        return output
    }

    /**
     * Magic Color mode — brightens background, enhances text contrast.
     * Looks like a "clean scan" — background becomes very white, text stays dark.
     */
    fun toMagicColor(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        // Convert to grayscale for background estimation
        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)

        // Estimate background via large morphological dilation (removes text)
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(15.0, 15.0))
        val background = Mat()
        Imgproc.dilate(gray, background, kernel)

        // Divide original by background to normalize illumination
        val normalized = Mat()
        Core.divide(gray, background, normalized, 255.0)

        // Enhance contrast
        val result = Mat()
        Core.convertScaleAbs(normalized, result, 1.5, -20.0)

        Imgproc.cvtColor(result, src, Imgproc.COLOR_GRAY2BGRA)
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(src, output)
        return output
    }

    /**
     * Enhanced mode — CLAHE (Contrast Limited Adaptive Histogram Equalization).
     * Best for faded/low-contrast documents. Keeps color, just improves contrast.
     */
    fun toEnhanced(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        // Convert to LAB color space — apply CLAHE only to L (luminance) channel
        val lab = Mat()
        Imgproc.cvtColor(src, lab, Imgproc.COLOR_BGR2Lab)
        val channels = ArrayList<Mat>()
        Core.split(lab, channels)

        val clahe = Imgproc.createCLAHE(3.0, Size(8.0, 8.0))
        clahe.apply(channels[0], channels[0])

        Core.merge(channels, lab)
        Imgproc.cvtColor(lab, src, Imgproc.COLOR_Lab2BGR)

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(src, output)
        return output
    }

    // ─── Shadow Removal ───────────────────────────────────────────────────────

    /**
     * Removes uneven lighting and shadows from a scanned document.
     * Uses morphological dilation to estimate background, then divides.
     */
    fun removeShadow(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val channels = ArrayList<Mat>()
        Core.split(src, channels)

        val result = ArrayList<Mat>()
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(7.0, 7.0))

        for (ch in channels) {
            val dilated = Mat()
            Imgproc.dilate(ch, dilated, kernel)
            val blurred = Mat()
            Imgproc.medianBlur(dilated, blurred, 21)
            val diff = Mat()
            Core.absdiff(ch, blurred, diff)
            Core.bitwise_not(diff, diff)
            Core.normalize(diff, diff, 0.0, 255.0, Core.NORM_MINMAX, CvType.CV_8UC1)
            result.add(diff)
        }

        Core.merge(result, src)
        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(src, output)
        return output
    }

    // ─── Sharpening ──────────────────────────────────────────────────────────

    /**
     * Sharpens a document scan using bilateral filter (noise removal) + Laplacian sharpening.
     * Reduces motion blur from hand-held capture.
     */
    fun sharpen(bitmap: Bitmap, strength: Float = 1.5f): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        // Bilateral filter — edge-preserving smoothing
        val smoothed = Mat()
        Imgproc.bilateralFilter(src, smoothed, 9, 75.0, 75.0)

        // Unsharp masking: sharpen = original + strength * (original - blurred)
        val blurred = Mat()
        Imgproc.GaussianBlur(src, blurred, Size(0.0, 0.0), 3.0)
        val sharpened = Mat()
        Core.addWeighted(src, 1.0 + strength, blurred, -strength, 0.0, sharpened)

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(sharpened, output)
        return output
    }

    // ─── Deskew ──────────────────────────────────────────────────────────────

    /**
     * Corrects slight rotation (deskew) of a document.
     * Detects dominant text angle via Hough line transform and rotates to correct.
     */
    fun deskew(bitmap: Bitmap): Bitmap {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        val edges = Mat()
        Imgproc.Canny(gray, edges, 50.0, 150.0)

        val lines = Mat()
        Imgproc.HoughLinesP(edges, lines, 1.0, Math.PI / 180, 100, 100.0, 10.0)

        var angle = 0.0
        var count = 0
        for (i in 0 until lines.rows()) {
            val line = lines[i, 0]
            val dx = line[2] - line[0]
            val dy = line[3] - line[1]
            if (Math.abs(dx) > Math.abs(dy)) { // near-horizontal lines only
                angle += Math.toDegrees(Math.atan2(dy, dx))
                count++
            }
        }

        if (count == 0) return bitmap
        val medianAngle = angle / count
        if (Math.abs(medianAngle) < 0.5) return bitmap // already straight enough

        val center = Point(src.width() / 2.0, src.height() / 2.0)
        val rotMat = Imgproc.getRotationMatrix2D(center, medianAngle, 1.0)
        val rotated = Mat()
        Imgproc.warpAffine(src, rotated, rotMat, src.size(), Imgproc.INTER_LINEAR, Core.BORDER_REPLICATE)

        val output = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(rotated, output)
        return output
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private fun distance(a: PointF, b: PointF): Float {
        val dx = (a.x - b.x).toDouble()
        val dy = (a.y - b.y).toDouble()
        return Math.sqrt(dx * dx + dy * dy).toFloat()
    }
}
