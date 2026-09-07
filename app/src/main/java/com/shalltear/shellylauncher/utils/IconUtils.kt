package com.shalltear.shellylauncher.utils

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.DisplayMetrics
import androidx.core.graphics.createBitmap
import androidx.core.graphics.toColorInt

/**
 * Returns the icon pixel size to use for a given screen density, keeping
 * memory proportional to the actual display size of icons.
 *
 * The icons are drawn at most ~77 dp (55 dp hex radius × 2 × 0.7 fill factor).
 * There is no need to store a full 192 px bitmap on every density.
 *
 *   mdpi  (160 dpi) →  48 px  (~8 KB ARGB_8888)
 *   hdpi  (240 dpi) →  72 px  (~20 KB)
 *   xhdpi (320 dpi) →  96 px  (~36 KB)
 *   xxhdpi+         → 128 px  (~64 KB)
 *
 * On low-end devices the sizes are halved to cut icon memory by 75%
 * (4× fewer pixels) and reduce GPU texture upload time.
 */
fun iconSizeForDensity(densityDpi: Int, isLowEnd: Boolean = false): Int {
    val base = when {
        densityDpi <= DisplayMetrics.DENSITY_MEDIUM -> 48
        densityDpi <= DisplayMetrics.DENSITY_HIGH   -> 72
        densityDpi <= DisplayMetrics.DENSITY_XHIGH  -> 96
        else                                         -> 128
    }
    return if (isLowEnd) (base / 2).coerceAtLeast(24) else base
}

fun drawableToBitmap(drawable: Drawable, size: Int = 96): Bitmap {
    val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    return bitmap
}

fun createLightUpIcon(): Bitmap {
    val size = 128
    val bitmap = createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint().apply {
        color = "#FFD700".toColorInt() // Gold
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2.1f, paint)

    val iconPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 8f
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 4f, iconPaint)
    canvas.drawLine(size / 2f, size / 4f, size / 2f, size / 8f, iconPaint)
    canvas.drawLine(size / 2f, size * 3/4f, size / 2f, size * 7/8f, iconPaint)
    canvas.drawLine(size / 4f, size / 2f, size / 8f, size / 2f, iconPaint)
    canvas.drawLine(size * 3/4f, size / 2f, size * 7/8f, size / 2f, iconPaint)
    return bitmap
}
