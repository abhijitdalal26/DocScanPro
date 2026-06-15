package com.abhijit.docscanpro.camera

import android.graphics.Bitmap
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc

/**
 * Detects the four corner points of a document in a photo using OpenCV.
 * Pipeline: Grayscale → Gaussian Blur → Canny Edge → Dilate → findContours → largest quad
 */
object DocumentBorderDetector {

    data class DocumentBorder(
        val topLeft: android.graphics.PointF,
        val topRight: android.graphics.PointF,
        val bottomRight: android.graphics.PointF,
        val bottomLeft: android.graphics.PointF
    ) {
        fun toPointFList() = listOf(topLeft, topRight, bottomRight, bottomLeft)
    }

    fun detect(bitmap: Bitmap): DocumentBorder? {
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_BGR2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)

        val edges = Mat()
        Imgproc.Canny(gray, edges, 75.0, 200.0)

        // Dilate edges to close small gaps in document border
        val kernel = Imgproc.getStructuringElement(Imgproc.MORPH_RECT, Size(3.0, 3.0))
        Imgproc.dilate(edges, edges, kernel, Point(-1.0, -1.0), 2)

        val contours = ArrayList<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(edges, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        // Sort contours by area descending — the document is usually the largest
        contours.sortByDescending { Imgproc.contourArea(it) }

        for (contour in contours.take(5)) {
            val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, 0.02 * peri, true)

            if (approx.rows() == 4) {
                val pts = approx.toList()
                val ordered = orderCorners(pts)
                return DocumentBorder(
                    topLeft = android.graphics.PointF(ordered[0].x.toFloat(), ordered[0].y.toFloat()),
                    topRight = android.graphics.PointF(ordered[1].x.toFloat(), ordered[1].y.toFloat()),
                    bottomRight = android.graphics.PointF(ordered[2].x.toFloat(), ordered[2].y.toFloat()),
                    bottomLeft = android.graphics.PointF(ordered[3].x.toFloat(), ordered[3].y.toFloat())
                )
            }
        }

        // No quad found — return full-image corners as fallback
        return fullImageBorder(bitmap)
    }

    fun fullImageBorder(bitmap: Bitmap) = DocumentBorder(
        topLeft = android.graphics.PointF(0f, 0f),
        topRight = android.graphics.PointF(bitmap.width.toFloat(), 0f),
        bottomRight = android.graphics.PointF(bitmap.width.toFloat(), bitmap.height.toFloat()),
        bottomLeft = android.graphics.PointF(0f, bitmap.height.toFloat())
    )

    /**
     * Orders 4 points as: top-left, top-right, bottom-right, bottom-left.
     * Based on sum (TL = min sum, BR = max sum) and diff (TR = min diff, BL = max diff).
     */
    private fun orderCorners(pts: List<Point>): List<Point> {
        val sorted = pts.sortedBy { it.x + it.y }
        val tl = sorted[0]
        val br = sorted[3]
        val remaining = listOf(sorted[1], sorted[2])
        val tr = remaining.minByOrNull { it.y - it.x }!!
        val bl = remaining.maxByOrNull { it.y - it.x }!!
        return listOf(tl, tr, br, bl)
    }
}
