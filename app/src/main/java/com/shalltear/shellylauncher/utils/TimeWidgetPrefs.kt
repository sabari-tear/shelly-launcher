package com.shalltear.shellylauncher.utils

import android.content.Context

object TimeWidgetPrefs {
    private const val PREFS_NAME = "time_widget_prefs"
    private const val KEY_SHOW_BATTERY = "show_battery"
    private const val KEY_SHOW_WIFI = "show_wifi"
    private const val KEY_SHOW_CELLULAR = "show_cellular"
    private const val KEY_SHOW_SPEED = "show_speed"
    private const val KEY_SPACING_DP = "spacing_dp"
    private const val KEY_CLOCK_STATUS_SPACING_DP = "clock_status_spacing_dp"
    private const val KEY_TIME_FONT_SIZE_SP = "time_font_size_sp"
    private const val KEY_ICON_SIZE_DP = "icon_size_dp"
    private const val KEY_ITEM_ORDER = "item_order"
    private const val KEY_SHOW_TIME = "show_time"
    private const val KEY_TIME_FORMAT = "time_format"
    private const val KEY_SHOW_DATE = "show_date"
    private const val KEY_DATE_FORMAT = "date_format"
    private const val KEY_TWO_LINES = "two_lines"
    private const val KEY_ALIGNMENT = "alignment"
    private const val KEY_MARGIN_START_DP = "margin_start_dp"
    private const val KEY_MARGIN_END_DP = "margin_end_dp"
    private const val KEY_TEXT_COLOR_ARGB = "text_color_argb"
    private const val KEY_OPACITY_PCT = "opacity_pct"
    private val DEFAULT_ORDER = listOf("battery", "wifi", "cellular", "speed")

    data class Settings(
        val showBattery: Boolean = true,
        val showWifi: Boolean = true,
        val showCellular: Boolean = true,
        val showSpeed: Boolean = true,
        val spacingDp: Int = 6,
        val clockStatusSpacingDp: Int = 4,
        val timeFontSizeSp: Int = 16,
        val iconSizeDp: Int = 13,
        val itemOrder: List<String> = listOf("battery", "wifi", "cellular", "speed"),
        val showTime: Boolean = true,
        val timeFormat: String = "HH:mm",
        val showDate: Boolean = true,
        val dateFormat: String = "EEE d MMM",
        val twoLines: Boolean = false,
        val alignment: String = "center",
        val marginStartDp: Int = 0,
        val marginEndDp: Int = 0,
        val textColorArgb: Int = 0xFFFFFFFF.toInt(),
        val opacityPct: Int = 100,
    )

    fun load(context: Context): Settings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return Settings(
            showBattery = prefs.getBoolean(KEY_SHOW_BATTERY, true),
            showWifi = prefs.getBoolean(KEY_SHOW_WIFI, true),
            showCellular = prefs.getBoolean(KEY_SHOW_CELLULAR, true),
            showSpeed = prefs.getBoolean(KEY_SHOW_SPEED, true),
            spacingDp = prefs.getInt(KEY_SPACING_DP, 6),
            clockStatusSpacingDp = prefs.getInt(KEY_CLOCK_STATUS_SPACING_DP, 4),
            timeFontSizeSp = prefs.getInt(KEY_TIME_FONT_SIZE_SP, 16),
            iconSizeDp = prefs.getInt(KEY_ICON_SIZE_DP, 13),
            itemOrder = prefs.getString(KEY_ITEM_ORDER, null)
                ?.split(",")?.filter { it.isNotBlank() }
                ?.takeIf { it.size == DEFAULT_ORDER.size } ?: DEFAULT_ORDER,
            showTime = prefs.getBoolean(KEY_SHOW_TIME, true),
            timeFormat = prefs.getString(KEY_TIME_FORMAT, "HH:mm") ?: "HH:mm",
            showDate = prefs.getBoolean(KEY_SHOW_DATE, true),
            dateFormat = prefs.getString(KEY_DATE_FORMAT, "EEE d MMM") ?: "EEE d MMM",
            twoLines = prefs.getBoolean(KEY_TWO_LINES, false),
            alignment = prefs.getString(KEY_ALIGNMENT, "center") ?: "center",
            marginStartDp = prefs.getInt(KEY_MARGIN_START_DP, 0),
            marginEndDp = prefs.getInt(KEY_MARGIN_END_DP, 0),
            textColorArgb = prefs.getInt(KEY_TEXT_COLOR_ARGB, 0xFFFFFFFF.toInt()),
            opacityPct = prefs.getInt(KEY_OPACITY_PCT, 100),
        )
    }

    fun save(context: Context, settings: Settings) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().apply {
            putBoolean(KEY_SHOW_BATTERY, settings.showBattery)
            putBoolean(KEY_SHOW_WIFI, settings.showWifi)
            putBoolean(KEY_SHOW_CELLULAR, settings.showCellular)
            putBoolean(KEY_SHOW_SPEED, settings.showSpeed)
            putInt(KEY_SPACING_DP, settings.spacingDp)
            putInt(KEY_CLOCK_STATUS_SPACING_DP, settings.clockStatusSpacingDp)
            putInt(KEY_TIME_FONT_SIZE_SP, settings.timeFontSizeSp)
            putInt(KEY_ICON_SIZE_DP, settings.iconSizeDp)
            putString(KEY_ITEM_ORDER, settings.itemOrder.joinToString(","))
            putBoolean(KEY_SHOW_TIME, settings.showTime)
            putString(KEY_TIME_FORMAT, settings.timeFormat)
            putBoolean(KEY_SHOW_DATE, settings.showDate)
            putString(KEY_DATE_FORMAT, settings.dateFormat)
            putBoolean(KEY_TWO_LINES, settings.twoLines)
            putString(KEY_ALIGNMENT, settings.alignment)
            putInt(KEY_MARGIN_START_DP, settings.marginStartDp)
            putInt(KEY_MARGIN_END_DP, settings.marginEndDp)
            putInt(KEY_TEXT_COLOR_ARGB, settings.textColorArgb)
            putInt(KEY_OPACITY_PCT, settings.opacityPct)
            apply()
        }
    }
}
