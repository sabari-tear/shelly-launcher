package com.shalltear.shellylauncher.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.shalltear.shellylauncher.data.AppInfo
import com.shalltear.shellylauncher.utils.AppMetadataStore
import com.shalltear.shellylauncher.utils.IconCache
import com.shalltear.shellylauncher.utils.drawableToBitmap
import com.shalltear.shellylauncher.utils.DeviceCapabilities
import com.shalltear.shellylauncher.utils.iconSizeForDensity

/**
 * Phase 1 — fast path (~1–5 ms vs ~200–500 ms for a full PM query).
 *
 * Reads the persisted metadata from [AppMetadataStore] and resolves icons via
 * PackageManager.getActivityIcon(), which is cheaper than a full resolveInfo
 * query because the PM lookup is skipped. Returns an empty list on the very
 * first launch (no cache yet) so the caller falls through to [loadInstalledApps].
 *
 * Apps that were uninstalled since the last save are silently dropped; the full
 * refresh in Phase 2 corrects the list immediately after.
 */
fun loadFromMetadataCache(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val isLowEnd = DeviceCapabilities.isLowRamDevice(context)
    val iconSize = iconSizeForDensity(context.resources.displayMetrics.densityDpi, isLowEnd)
    val entries = AppMetadataStore.load(context)

    return entries.mapNotNull { entry ->
        try {
            val component = ComponentName(entry.packageName, entry.activityName)
            val icon = IconCache.get(entry.packageName) ?: run {
                val bmp = drawableToBitmap(pm.getActivityIcon(component), iconSize)
                IconCache.put(entry.packageName, bmp)
                bmp
            }
            AppInfo(
                name = entry.label,
                packageName = entry.packageName,
                icon = icon,
                launchIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setClassName(entry.packageName, entry.activityName)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        } catch (_: Exception) {
            null // app was uninstalled since last save — Phase 2 cleans up the store
        }
    }
}

/**
 * Phase 2 — authoritative full refresh.
 *
 * Queries the PackageManager for all launcher-intent activities, decodes/caches
 * icons (cache hit if Phase 1 already populated them), and persists the resulting
 * sorted list to [AppMetadataStore] so the next cold start can use Phase 1.
 */
fun loadInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val isLowEnd = DeviceCapabilities.isLowRamDevice(context)
    val iconSize = iconSizeForDensity(context.resources.displayMetrics.densityDpi, isLowEnd)

    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolvedActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(
            mainIntent,
            PackageManager.ResolveInfoFlags.of(0),
        )
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(mainIntent, 0)
    }

    val apps = resolvedActivities
        .map { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            val packageName = activityInfo.packageName
            val icon = IconCache.get(packageName) ?: run {
                val bmp = drawableToBitmap(resolveInfo.loadIcon(pm), iconSize)
                IconCache.put(packageName, bmp)
                bmp
            }
            AppInfo(
                name = resolveInfo.loadLabel(pm).toString(),
                packageName = packageName,
                icon = icon,
                launchIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setClassName(activityInfo.packageName, activityInfo.name)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                },
            )
        }
        .distinctBy { it.launchIntent?.component?.flattenToShortString() }
        .sortedBy { it.name.lowercase() }

    // Persist sorted metadata so the next cold start can skip this PM query.
    AppMetadataStore.save(
        context,
        apps.map { app ->
            AppMetadataStore.Entry(
                packageName = app.packageName,
                activityName = app.launchIntent?.component?.className ?: "",
                label = app.name,
            )
        },
    )

    return apps
}
