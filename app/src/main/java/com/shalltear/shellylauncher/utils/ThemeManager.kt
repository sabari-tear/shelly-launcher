package com.shalltear.shellylauncher.utils

import android.content.Context
import androidx.compose.ui.graphics.Color

/**
 * Theme colors and system for ShellyLauncher.
 * Manages primary accent, dark mode, and color palettes.
 */
object ThemeManager {
    // Predefined color palettes
    data class Palette(
        val name: String,
        val primary: Color,
        val secondary: Color,
        val accent: Color,
    )

    val palettes = listOf(
        Palette("Purple", Color(0xFF7B6FEF), Color(0xFF5A4FD4), Color(0xFF9B94FF)),
        Palette("Blue", Color(0xFF4A90FF), Color(0xFF2E5BFF), Color(0xFF7BB7FF)),
        Palette("Green", Color(0xFF00C896), Color(0xFF00A876), Color(0xFF4DD9A8)),
        Palette("Pink", Color(0xFFFF6B9D), Color(0xFFFF4981), Color(0xFFFF99BB)),
        Palette("Orange", Color(0xFFFF9500), Color(0xFFFF7500), Color(0xFFFFB84D)),
        Palette("Cyan", Color(0xFF00BCD4), Color(0xFF0097A7), Color(0xFF4DD0E1)),
    )

    const val DEFAULT_PALETTE = "Purple"

    fun getPalette(name: String): Palette {
        return palettes.find { it.name == name } ?: palettes[0]
    }

    fun savePalette(context: Context, paletteName: String) {
        val prefs = context.getSharedPreferences("launcher_theme", Context.MODE_PRIVATE)
        prefs.edit().putString("color_palette", paletteName).apply()
    }

    fun loadPalette(context: Context): Palette {
        val prefs = context.getSharedPreferences("launcher_theme", Context.MODE_PRIVATE)
        val paletteName = prefs.getString("color_palette", DEFAULT_PALETTE) ?: DEFAULT_PALETTE
        return getPalette(paletteName)
    }

    fun saveDarkMode(context: Context, enabled: Boolean) {
        val prefs = context.getSharedPreferences("launcher_theme", Context.MODE_PRIVATE)
        prefs.edit().putBoolean("dark_mode", enabled).apply()
    }

    fun loadDarkMode(context: Context): Boolean {
        val prefs = context.getSharedPreferences("launcher_theme", Context.MODE_PRIVATE)
        return prefs.getBoolean("dark_mode", true)
    }
}
