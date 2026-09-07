package com.shalltear.shellylauncher.utils

/**
 * Utilities for rendering notification badge counts on app icons.
 * In production, this would connect to NotificationCompat.getNotifications()
 * but currently provides mock badge rendering support.
 */
object NotificationBadgeUtils {
    
    /**
     * Stores unread notification count per package.
     * In a real implementation, this would be populated by a NotificationListener service.
     */
    private val badgeCountCache = mutableMapOf<String, Int>()

    fun getBadgeCount(packageName: String): Int {
        return badgeCountCache[packageName] ?: 0
    }

    fun setBadgeCount(packageName: String, count: Int) {
        if (count <= 0) {
            badgeCountCache.remove(packageName)
        } else {
            badgeCountCache[packageName] = count.coerceAtMost(99)  // Cap at 99
        }
    }

    fun hasBadge(packageName: String): Boolean {
        return badgeCountCache.containsKey(packageName)
    }

    fun clearBadge(packageName: String) {
        badgeCountCache.remove(packageName)
    }

    fun clearAllBadges() {
        badgeCountCache.clear()
    }

    /**
     * Format badge count for display.
     * Shows "1" through "99", and "99+" for counts >= 100.
     */
    fun formatBadgeText(count: Int): String {
        return when {
            count <= 0 -> ""
            count <= 99 -> count.toString()
            else -> "99+"
        }
    }
}
