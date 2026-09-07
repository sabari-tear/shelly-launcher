package com.shalltear.shellylauncher.utils

import android.content.Context
import androidx.core.content.edit

/**
 * Manages pinned/favorite apps for quick access.
 * Stores up to 5 favorite app package names in order.
 */
object AppFavoritesPrefs {
    private const val PREF_KEY = "app_favorites"
    private const val MAX_FAVORITES = 5

    data class FavoritesData(
        val favorites: List<String> = emptyList(),
    )

    fun load(context: Context): FavoritesData {
        val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        val favoritesStr = prefs.getString(PREF_KEY, "") ?: ""
        val favorites = if (favoritesStr.isBlank()) emptyList() else favoritesStr.split(",")
        return FavoritesData(favorites)
    }

    fun save(context: Context, favorites: List<String>) {
        val validFavorites = favorites.take(MAX_FAVORITES)
        val prefs = context.getSharedPreferences("launcher_prefs", Context.MODE_PRIVATE)
        prefs.edit {
            putString(PREF_KEY, validFavorites.joinToString(","))
        }
    }

    fun addFavorite(context: Context, packageName: String) {
        val current = load(context).favorites.toMutableList()
        if (!current.contains(packageName) && current.size < MAX_FAVORITES) {
            current.add(0, packageName)  // Insert at top for recency
            save(context, current)
        }
    }

    fun removeFavorite(context: Context, packageName: String) {
        val current = load(context).favorites.toMutableList()
        current.remove(packageName)
        save(context, current)
    }

    fun toggleFavorite(context: Context, packageName: String) {
        val current = load(context).favorites
        if (current.contains(packageName)) {
            removeFavorite(context, packageName)
        } else {
            addFavorite(context, packageName)
        }
    }

    fun isFavorite(context: Context, packageName: String): Boolean {
        return load(context).favorites.contains(packageName)
    }
}
