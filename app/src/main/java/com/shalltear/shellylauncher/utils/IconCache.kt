package com.shalltear.shellylauncher.utils

import android.graphics.Bitmap
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap

/**
 * Process-wide LRU bitmap cache for app icons.
 *
 * Max size = 1/8 of the available heap, hard-capped at 4 MB.
 * This keeps memory footprint tiny on low-RAM devices (e.g. 512 MB RAM phones
 * where the app heap limit can be as low as 48 MB, giving ~6 MB for the cache).
 *
 * Keyed by package name so icons survive calls to [loadInstalledApps] on the
 * same process lifecycle (e.g. triggered by PACKAGE_ADDED broadcasts later).
 *
 * A second [ImageBitmap] tier wraps each [Bitmap] exactly once so Compose
 * always uses the same GPU texture handle — re-calling [asImageBitmap] on a new
 * object forces a GPU re-upload on every recomposition.
 */
object IconCache {

    private val maxBytes: Int =
        (Runtime.getRuntime().maxMemory() / 8).coerceAtMost(4L * 1024 * 1024).toInt()

    private val cache = object : LruCache<String, Bitmap>(maxBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.byteCount
        
        override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
            // Explicitly recycle old bitmap when evicted or replaced to prevent memory leaks
            if (evicted || newValue == null) {
                oldValue.recycle()
            }
        }
    }

    // Mirrors the Bitmap cache; never evicted independently — cleared together in evictAll().
    private val imageBitmapCache = HashMap<String, ImageBitmap>()

    fun get(key: String): Bitmap? = cache[key]

    fun put(key: String, bitmap: Bitmap) {
        cache.put(key, bitmap)
        // Pre-wrap so every caller gets the same ImageBitmap instance
        imageBitmapCache[key] = bitmap.asImageBitmap()
    }

    /** Returns the cached [ImageBitmap] for [key], or null if not yet loaded. */
    fun getImageBitmap(key: String): ImageBitmap? = imageBitmapCache[key]

    /** Evict a specific icon and recycle its bitmap. */
    fun evict(key: String) {
        cache.remove(key)?.recycle()
        imageBitmapCache.remove(key)
    }

    /** Call when the app list is fully refreshed so stale icons are evicted. */
    fun evictAll() {
        // Explicitly recycle all bitmaps before clearing
        cache.snapshot().forEach { (_, bitmap) -> bitmap.recycle() }
        cache.evictAll()
        imageBitmapCache.clear()
    }
}
