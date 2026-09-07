package com.shalltear.shellylauncher.ui.overlay

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shellylauncher.ui.components.SettingsOverlayScaffold
import com.shalltear.shellylauncher.ui.components.SettingsSliderRow

@Composable
fun AppDrawerSettingsOverlay(
    visible: Boolean,
    wallpaperBitmap: Bitmap?,
    blurRadiusDp: Float,
    appDrawerOpacityPct: Float,
    onBlurRadiusChange: (Float) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)),
    ) {
        val wallpaperImageBitmap = remember(wallpaperBitmap) { wallpaperBitmap?.asImageBitmap() }

        SettingsOverlayScaffold(
            title = "App Drawer",
            onBack = onDismiss,
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Preview — shows wallpaper at the configured blur + opacity as it looks when drawer opens
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(190.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF141420), Color(0xFF0C0C16))),
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                wallpaperImageBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .blur(blurRadiusDp.coerceAtLeast(0f).dp)
                            .graphicsLayer { alpha = appDrawerOpacityPct / 100f }
                    )
                } ?: Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { alpha = appDrawerOpacityPct / 100f },
                    contentAlignment = Alignment.Center
                ) {
                    Text("No wallpaper set", color = Color(0x66FFFFFF), fontSize = 14.sp)
                }

                // Overlay label to indicate this is the app drawer look
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .background(Color(0x66000000), RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp, bottomStart = 16.dp, bottomEnd = 16.dp))
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Preview: App Drawer appearance",
                        color = Color(0xCCFFFFFF),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Blur radius slider
            SettingsSliderRow(
                label = "Background Blur",
                valueLabel = "${blurRadiusDp.toInt()}dp",
                value = blurRadiusDp,
                onValueChange = { onBlurRadiusChange(it); onSave() },
                valueRange = 0f..20f,
                steps = 19,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // App drawer opacity slider
            SettingsSliderRow(
                label = "Background Opacity",
                valueLabel = "${appDrawerOpacityPct.toInt()}%",
                value = appDrawerOpacityPct,
                onValueChange = { onOpacityChange(it); onSave() },
                valueRange = 0f..100f,
                steps = 99,
            )

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
