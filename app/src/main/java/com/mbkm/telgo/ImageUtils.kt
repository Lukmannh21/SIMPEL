package com.mbkm.telgo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import kotlin.math.min

/**
 * Utility class for high-quality image processing
 */
object ImageUtils {
    // Maximum image dimensions for display in UI
    private const val MAX_UI_WIDTH = 1000
    private const val MAX_UI_HEIGHT = 1000

    // Maximum image dimensions for PDF rendering (higher quality)
    private const val MAX_PDF_WIDTH = 2000
    private const val MAX_PDF_HEIGHT = 2000

    /**
     * Loads a high-quality bitmap from URI with proper sampling to avoid memory issues
     */
    fun loadHighQualityBitmapFromUri(context: Context, uri: Uri, maxWidth: Int, maxHeight: Int): Bitmap? {
        try {
            // First get image dimensions
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                BitmapFactory.decodeStream(input, null, options)
            }

            if (options.outWidth <= 0 || options.outHeight <= 0) {
                Log.e("ImageUtils", "Invalid image dimensions: ${options.outWidth}x${options.outHeight}")
                return null
            }

            // Calculate sampling factor
            val widthSample = options.outWidth.toFloat() / maxWidth
            val heightSample = options.outHeight.toFloat() / maxHeight
            val sampleFactor = maxOf(widthSample, heightSample)

            // Load with sampling if needed
            val loadOptions = BitmapFactory.Options().apply {
                inPreferredConfig = Bitmap.Config.ARGB_8888
                inSampleSize = if (sampleFactor > 1) sampleFactor.toInt() else 1
            }

            context.contentResolver.openInputStream(uri)?.use { input ->
                return BitmapFactory.decodeStream(input, null, loadOptions)
            }
        } catch (e: Exception) {
            Log.e("ImageUtils", "Error loading bitmap: ${e.message}")
        }
        return null
    }

    /**
     * Scales a bitmap to target dimensions while maintaining aspect ratio
     */
    fun scaleBitmapForDisplay(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        // Calculate scaling factors
        val scaleWidth = maxWidth.toFloat() / width
        val scaleHeight = maxHeight.toFloat() / height
        val scaleFactor = min(scaleWidth, scaleHeight)

        // Only scale down, never up
        if (scaleFactor >= 1.0f) return bitmap

        val newWidth = (width * scaleFactor).toInt()
        val newHeight = (height * scaleFactor).toInt()

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Loads a bitmap from URI for display in the UI
     */
    fun loadBitmapForUI(context: Context, uri: Uri): Bitmap? {
        return loadHighQualityBitmapFromUri(context, uri, MAX_UI_WIDTH, MAX_UI_HEIGHT)
    }

    /**
     * Loads a bitmap from URI optimized for PDF rendering
     */
    fun loadBitmapForPDF(context: Context, uri: Uri): Bitmap? {
        return loadHighQualityBitmapFromUri(context, uri, MAX_PDF_WIDTH, MAX_PDF_HEIGHT)
    }
}