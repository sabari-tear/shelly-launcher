package com.shalltear.shellylauncher.utils

import android.app.ActivityManager
import android.content.Context

/**
 * Queries hardware capability flags that the launcher uses to decide whether to
 * enable expensive visual effects (glow, spring animations, etc.).
 */
object DeviceCapabilities {

    /**
     * Returns true if the system has declared itself a low-RAM device.
     * On Android 8+ this maps to the `ro.config.low_ram` system property and is
     * reliable across OEMs (set to true on Go-edition and entry-level devices).
     */
    fun isLowRamDevice(context: Context): Boolean {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return am.isLowRamDevice
    }
}
