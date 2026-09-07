package com.shalltear.shellylauncher.utils

import android.content.Context
import androidx.core.content.edit

object WallpaperPrefs {
    private const val PREFS_NAME = "wallpaper_prefs"
    private const val KEY_URI = "wallpaper_uri"
    private const val KEY_BLUR_RADIUS = "blur_radius"
    private const val KEY_HOMESCREEN_BLUR = "homescreen_blur_radius"
    private const val KEY_OPACITY_PCT = "opacity_pct"
    private const val KEY_APP_DRAWER_OPACITY = "app_drawer_opacity_pct"
    private const val KEY_CROP_OFFSET_X = "crop_offset_x"
    private const val KEY_CROP_OFFSET_Y = "crop_offset_y"
    private const val KEY_ZOOM_SCALE    = "zoom_scale"

    data class Settings(
        val uri: String? = null,
        val blurRadiusDp: Int = 12,
        val homescreenBlurRadiusDp: Int = 0,
        val opacityPct: Int = 100,
        val appDrawerOpacityPct: Int = 80,
        val cropOffsetXFraction: Float = 0.5f,
        val cropOffsetYFraction: Float = 0.5f,
        val zoomScale: Float = 1.0f,
    )

    fun load(context: Context): Settings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Settings(
            uri = prefs.getString(KEY_URI, null),
            blurRadiusDp = prefs.getInt(KEY_BLUR_RADIUS, 12),
            homescreenBlurRadiusDp = prefs.getInt(KEY_HOMESCREEN_BLUR, 0),
            opacityPct = prefs.getInt(KEY_OPACITY_PCT, 100),
            appDrawerOpacityPct = prefs.getInt(KEY_APP_DRAWER_OPACITY, 80),
            cropOffsetXFraction = prefs.getFloat(KEY_CROP_OFFSET_X, 0.5f),
            cropOffsetYFraction = prefs.getFloat(KEY_CROP_OFFSET_Y, 0.5f),
            zoomScale = prefs.getFloat(KEY_ZOOM_SCALE, 1.0f),
        )
    }

    fun save(context: Context, settings: Settings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_URI, settings.uri)
            putInt(KEY_BLUR_RADIUS, settings.blurRadiusDp)
            putInt(KEY_HOMESCREEN_BLUR, settings.homescreenBlurRadiusDp)
            putInt(KEY_OPACITY_PCT, settings.opacityPct)
            putInt(KEY_APP_DRAWER_OPACITY, settings.appDrawerOpacityPct)
            putFloat(KEY_CROP_OFFSET_X, settings.cropOffsetXFraction)
            putFloat(KEY_CROP_OFFSET_Y, settings.cropOffsetYFraction)
            putFloat(KEY_ZOOM_SCALE, settings.zoomScale)
        }
    }
}
