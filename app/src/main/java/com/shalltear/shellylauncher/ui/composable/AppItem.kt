package com.shalltear.shellylauncher.ui.composable

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shellylauncher.data.AppInfo
import com.shalltear.shellylauncher.utils.IconCache
import com.shalltear.shellylauncher.utils.NotificationBadgeUtils
import com.shalltear.shellylauncher.utils.lerp

@Composable
fun AppItem(
    app: AppInfo,
    index: Int,
    basePos: Offset,
    fingerPosition: State<Offset>,
    isAppsVisible: Boolean,
    selectedIndex: State<Int>,
    isLightUpMode: Boolean,
    hexRadiusPx: Float,
    isLowEndDevice: Boolean = false,
    badgeCount: Int = 0,        // Notification badge count
    isSearchMode: Boolean = false,  // When true: hide this item, SearchResultsGrid shows results
) {
    val isSelected = selectedIndex.value == index

    // Use the process-level IconCache ImageBitmap first (avoids asImageBitmap() being called at all
    // after first load). Falls back to remember(app.icon) so correctness is guaranteed.
    val iconBitmap: ImageBitmap = remember(app.icon) {
        IconCache.getImageBitmap(app.packageName) ?: app.icon.asImageBitmap()
    }

    // Animate scale. Target values resolve during composition when `isSelected` changes.
    // On low-end devices use snap() to eliminate animation CPU/GPU cost entirely.
    val scaleAnim by animateFloatAsState(
        targetValue = if (isSelected) 1.8f else 1f,
        animationSpec = if (isLowEndDevice) snap()
                        else spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "iconScale"
    )

    // Reveal animation: icons emerge from dark on enter, sink to dark on exit.
    // revealProgress 0 = fully hidden (dark), 1 = fully revealed.
    val revealProgress by animateFloatAsState(
        targetValue = if (isAppsVisible) 1f else 0f,
        animationSpec = if (isLowEndDevice) snap()
                        else tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "revealProgress"
    )

    // Smooth transition for Light Up mode
    val lightUpProgress by animateFloatAsState(
        targetValue = if (isLightUpMode) 1f else 0f,
        animationSpec = if (isLowEndDevice) snap()
                        else spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "lightUpTransition"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                val progress = lightUpProgress
                val reveal = revealProgress

                // Skip rendering only when all animations have fully settled to hidden
                if (reveal == 0f && progress == 0f) {
                    alpha = 0f
                    return@graphicsLayer
                }

                val fingerPos = fingerPosition.value
                val dx = basePos.x - fingerPos.x
                val dy = basePos.y - fingerPos.y
                val distSq = dx * dx + dy * dy

                // 1. Calculate Spotlight Alpha
                val spotlightRadius = hexRadiusPx * 4f
                val spotlightRadiusSq = spotlightRadius * spotlightRadius
                val normalAlpha = if (distSq > spotlightRadiusSq) 0f else {
                    val dist = kotlin.math.sqrt(distSq)
                    val fade = 1f - (dist / spotlightRadius)
                    fade * fade * (3f - 2f * fade) // Smooth curve
                }

                // 2. Calculate Donut Repulsion Physics
                val repulsionRadius = hexRadiusPx * 3.5f
                val repulsionRadiusSq = repulsionRadius * repulsionRadius
                val maxRepulsionForce = hexRadiusPx * 1.5f

                var normalOffsetX = basePos.x
                var normalOffsetY = basePos.y

                // Instead of snapping, use a parabolic curve that is 0 at center, max at half radius, 0 at edge
                if (distSq < repulsionRadiusSq && distSq > 1f) {
                    val dist = kotlin.math.sqrt(distSq)
                    val normalizedDist = dist / repulsionRadius
                    // 4 * x * (1 - x) is a perfect parabola peaking at 1 when x = 0.5
                    val force = maxRepulsionForce * 4f * normalizedDist * (1f - normalizedDist)

                    val dirX = dx / dist
                    val dirY = dy / dist
                    normalOffsetX += dirX * force
                    normalOffsetY += dirY * force
                }

                // 3. Interpolate everything based on mode + progress
                val revealScale = lerp(0.75f, 1f, reveal)

                if (isSearchMode) {
                    // Search mode: hide all hex-grid apps; SearchResultsGrid shows results instead.
                    alpha = 0f
                    translationX = basePos.x - hexRadiusPx
                    translationY = basePos.y - hexRadiusPx
                    scaleX = revealScale
                    scaleY = revealScale
                } else {
                    // Normal drag mode: spotlight alpha + repulsion physics.
                    alpha = lerp(normalAlpha, 1f, progress) * reveal
                    val finalOffsetX = lerp(normalOffsetX, basePos.x, progress)
                    val finalOffsetY = lerp(normalOffsetY, basePos.y, progress)
                    translationX = finalOffsetX - hexRadiusPx
                    translationY = finalOffsetY - hexRadiusPx
                    val finalScale = lerp(scaleAnim, 1f, progress) * revealScale
                    scaleX = finalScale
                    scaleY = finalScale
                }
            }
            .size(with(LocalDensity.current) { (hexRadiusPx * 2).toDp() }),
        contentAlignment = Alignment.Center
    ) {
        // Draw glow behind icon — skipped on low-end devices to reduce GPU overdraw.
        if (!isLowEndDevice && scaleAnim > 1.05f) {
            val glowAlpha = ((scaleAnim - 1f) / 0.8f).coerceIn(0f, 1f)
            if (glowAlpha > 0f) {
                Canvas(modifier = Modifier.fillMaxSize(0.9f)) {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.5f * glowAlpha),
                                Color.Transparent
                            )
                        )
                    )
                }
            }
        }

        Image(
            bitmap = iconBitmap,
            contentDescription = app.name,
            modifier = Modifier.fillMaxSize(0.7f)
        )

        // Notification badge in top-right corner
        if (badgeCount > 0) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(20.dp)
                    .background(Color(0xFFFF4444), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = NotificationBadgeUtils.formatBadgeText(badgeCount),
                    color = Color.White,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
