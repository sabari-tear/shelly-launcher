package com.shalltear.shellylauncher.utils

import android.content.Context
import androidx.core.content.edit
import java.io.File

object CustomTextWidgetPrefs {
    private const val PREFS_NAME = "custom_text_widget_prefs"
    private const val KEY_TEXT = "text"
    private const val KEY_FONT_FILENAME = "font_filename"
    private const val KEY_LETTER_SPACING = "letter_spacing"
    private const val KEY_ALIGNMENT = "alignment"
    private const val KEY_OFFSET_X_FRACTION = "offset_x_fraction"
    private const val KEY_OFFSET_Y_FRACTION = "offset_y_fraction"
    private const val KEY_WIDTH_FRACTION = "width_fraction"
    private const val KEY_FONT_SIZE_SP = "font_size_sp"
    private const val KEY_VISIBLE = "visible"
    private const val KEY_TEXT_COLOR_ARGB = "text_color_argb"
    private const val KEY_OPACITY_PCT = "opacity_pct"

    data class Settings(
        val text: String = "",
        val fontFilename: String? = null,
        val letterSpacing: Float = 0f,
        val alignment: String = "center",
        val offsetXFraction: Float = 0.05f,
        val offsetYFraction: Float = 0.35f,
        val widthFraction: Float = 0.9f,
        val fontSizeSp: Float = 22f,
        val visible: Boolean = true,
        val textColorArgb: Int = 0xFFFFFFFF.toInt(),
        val opacityPct: Int = 100,
    )

    fun load(context: Context): Settings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Settings(
            text = prefs.getString(KEY_TEXT, "") ?: "",
            fontFilename = prefs.getString(KEY_FONT_FILENAME, null),
            letterSpacing = prefs.getFloat(KEY_LETTER_SPACING, 0f),
            alignment = prefs.getString(KEY_ALIGNMENT, "center") ?: "center",
            offsetXFraction = prefs.getFloat(KEY_OFFSET_X_FRACTION, 0.05f),
            offsetYFraction = prefs.getFloat(KEY_OFFSET_Y_FRACTION, 0.35f),
            widthFraction = prefs.getFloat(KEY_WIDTH_FRACTION, 0.9f),
            fontSizeSp = prefs.getFloat(KEY_FONT_SIZE_SP, 22f),
            visible = prefs.getBoolean(KEY_VISIBLE, true),
            textColorArgb = prefs.getInt(KEY_TEXT_COLOR_ARGB, 0xFFFFFFFF.toInt()),
            opacityPct = prefs.getInt(KEY_OPACITY_PCT, 100),
        )
    }

    fun save(context: Context, settings: Settings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_TEXT, settings.text)
            putString(KEY_FONT_FILENAME, settings.fontFilename)
            putFloat(KEY_LETTER_SPACING, settings.letterSpacing)
            putString(KEY_ALIGNMENT, settings.alignment)
            putFloat(KEY_OFFSET_X_FRACTION, settings.offsetXFraction)
            putFloat(KEY_OFFSET_Y_FRACTION, settings.offsetYFraction)
            putFloat(KEY_WIDTH_FRACTION, settings.widthFraction)
            putFloat(KEY_FONT_SIZE_SP, settings.fontSizeSp)
            putBoolean(KEY_VISIBLE, settings.visible)
            putInt(KEY_TEXT_COLOR_ARGB, settings.textColorArgb)
            putInt(KEY_OPACITY_PCT, settings.opacityPct)
        }
    }

    /** Returns the directory where custom font files are stored, creating it if needed. */
    fun fontsDir(context: Context): File =
        File(context.filesDir, "fonts").also { it.mkdirs() }
}
