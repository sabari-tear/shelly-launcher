package com.shalltear.shellylauncher.data

import android.content.Intent
import android.graphics.Bitmap

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Bitmap,
    val launchIntent: Intent?
)
