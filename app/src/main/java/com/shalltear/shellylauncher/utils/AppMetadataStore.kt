package com.shalltear.shellylauncher.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

/**
 * Persists the sorted installed-app list to SharedPreferences using a plain
 * tab/newline-delimited format — no JSON library needed.
 *
 * Each line: packageName\tactivityName\tappLabel
 *
 * On cold start this lets [loadFromMetadataCache] skip
 * PackageManager.queryIntentActivities (~100–300 ms on weak devices) and jump
 * straight to icon decoding, so the honeycomb grid appears much sooner.
 * A background full-refresh then detects installs / removals and updates the store.
 */
object AppMetadataStore {

    private const val PREFS_NAME = "shelly_app_metadata_v1"
    private const val KEY_LIST   = "app_list"

    data class Entry(val packageName: String, val activityName: String, val label: String)

    private fun prefs(context: Context): SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns an empty list on first launch (nothing persisted yet). */
    fun load(context: Context): List<Entry> {
        val raw = prefs(context).getString(KEY_LIST, null) ?: return emptyList()
        return raw.split('\n').mapNotNull { line ->
            val parts = line.split('\t')
            if (parts.size == 3) Entry(parts[0], parts[1], parts[2]) else null
        }
    }

    /** Persists asynchronously (apply) — safe to call from a background coroutine. */
    fun save(context: Context, entries: List<Entry>) {
        val raw = entries.joinToString("\n") {
            "${it.packageName}\t${it.activityName}\t${it.label}"
        }
        prefs(context).edit { putString(KEY_LIST, raw) }
    }
}
