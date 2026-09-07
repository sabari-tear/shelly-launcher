package com.shalltear.shellylauncher.ui.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val BgGradient = Brush.verticalGradient(listOf(Color(0xFF0C0C14), Color(0xFF07070D)))
private val AccentColor = Color(0xFF7B6FEF)
private val CardGradient = Brush.verticalGradient(listOf(Color(0xFF181825), Color(0xFF111119)))
private val GlowColor = Color(0x507B6FEF)

/**
 * Full-screen loading splash shown on first launch until apps and wallpaper are ready.
 * Uses the same premium dark aesthetic as the settings screens.
 * No heavy animations — just a gentle pulsing accent dot so low-end devices
 * don't burn CPU before caches are ready.
 */
@Composable
fun LoadingScreen() {
    // Single infinite pulse on the dot — very cheap, one scalar lerp per frame
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue  = 1f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulse",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BgGradient),
        contentAlignment = Alignment.Center,
    ) {
        // Card — matches SettingsNavRow / tile style exactly
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(24.dp))
                .background(CardGradient)
                .drawBehind {
                    // 1dp border drawn manually so we can use GlowColor
                    drawRoundRect(
                        color       = Color(0x18FFFFFF),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(24.dp.toPx()),
                        style        = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.dp.toPx()),
                    )
                }
                .padding(horizontal = 48.dp, vertical = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Pulsing accent circle — no Lottie / no Canvas animation, just alpha
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(AccentColor.copy(alpha = 0.9f), AccentColor.copy(alpha = 0.3f))
                        )
                    )
                    .graphicsLayer { alpha = pulse },
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text       = "ShellyLauncher",
                color      = Color.White,
                fontSize   = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text       = "Loading…",
                color      = AccentColor,
                fontSize   = 13.sp,
                fontWeight = FontWeight.Medium,
            )
        }

        // Subtle glow line at bottom — same DividerGlow pattern as SettingsOverlayScaffold
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
                .size(width = 200.dp, height = 1.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, GlowColor, Color.Transparent)
                    )
                )
        )
    }
}
