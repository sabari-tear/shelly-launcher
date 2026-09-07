package com.shalltear.shellylauncher.ui.overlay

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shellylauncher.ui.components.SettingsOverlayScaffold
import com.shalltear.shellylauncher.ui.components.SettingsSliderRow

@Composable
fun WallpaperSettingsOverlay(
    visible: Boolean,
    wallpaperBitmap: Bitmap?,
    hasWallpaper: Boolean,
    blurRadiusDp: Float,
    onBlurRadiusChange: (Float) -> Unit,
    opacityPct: Float,
    onOpacityChange: (Float) -> Unit,
    onPickWallpaper: () -> Unit,
    onRemoveWallpaper: () -> Unit,
    onDismiss: () -> Unit,
    saveWallpaper: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)),
    ) {
        val wallpaperImageBitmap = remember(wallpaperBitmap) { wallpaperBitmap?.asImageBitmap() }

        SettingsOverlayScaffold(
            title = "Wallpaper",
            onBack = onDismiss,
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Preview
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
                            .graphicsLayer {
                                alpha = opacityPct / 100f
                                val blurPx = blurRadiusDp.dp.toPx()
                                renderEffect = if (blurPx > 0f) {
                                    BlurEffect(blurPx, blurPx, TileMode.Decal)
                                } else null
                            }
                    )
                } ?: Text("No wallpaper set", color = Color(0x66FFFFFF), fontSize = 14.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Blur slider (static homescreen wallpaper blur)
            SettingsSliderRow(
                label = "Wallpaper Blur",
                valueLabel = "${blurRadiusDp.toInt()}dp",
                value = blurRadiusDp,
                onValueChange = { onBlurRadiusChange(it); saveWallpaper() },
                valueRange = 0f..20f,
                steps = 19,
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Opacity slider (homescreen wallpaper opacity)
            SettingsSliderRow(
                label = "Wallpaper Opacity",
                valueLabel = "${opacityPct.toInt()}%",
                value = opacityPct,
                onValueChange = { onOpacityChange(it); saveWallpaper() },
                valueRange = 0f..100f,
                steps = 99,
            )

            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF181825), Color(0xFF111119))),
                        RoundedCornerShape(14.dp)
                    )
                    .border(1.dp, Color(0x337B6FEF), RoundedCornerShape(14.dp))
                    .clickable(onClick = onPickWallpaper),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Image, contentDescription = null, tint = Color(0xFF7B6FEF), modifier = Modifier.size(20.dp))
                    Text("Pick from Gallery", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }

            if (hasWallpaper) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF2A1010), Color(0xFF1A0808))),
                            RoundedCornerShape(14.dp)
                        )
                        .border(1.dp, Color(0x33FF4444), RoundedCornerShape(14.dp))
                        .clickable(onClick = onRemoveWallpaper),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Remove Wallpaper", color = Color(0xFFFF6B6B), fontSize = 15.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
