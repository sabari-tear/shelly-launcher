package com.shalltear.shellylauncher.utils

import android.content.Context
import androidx.core.content.edit

object AppGridPrefs {
    private const val PREFS_NAME = "app_grid_prefs"
    private const val KEY_LABEL_THEME = "label_theme"
    private const val KEY_HAS_OPENED_SETTINGS = "has_opened_settings"

    data class Settings(
        val labelTheme: String = "auto", // "auto", "light", "dark"
        val hasOpenedSettings: Boolean = false,
    )

    fun load(context: Context): Settings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Settings(
            labelTheme = prefs.getString(KEY_LABEL_THEME, "auto") ?: "auto",
            hasOpenedSettings = prefs.getBoolean(KEY_HAS_OPENED_SETTINGS, false),
        )
    }

    fun save(context: Context, settings: Settings) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit {
            putString(KEY_LABEL_THEME, settings.labelTheme)
            putBoolean(KEY_HAS_OPENED_SETTINGS, settings.hasOpenedSettings)
        }
    }
}
