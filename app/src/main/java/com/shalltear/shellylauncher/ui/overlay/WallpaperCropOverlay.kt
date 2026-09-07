package com.shalltear.shellylauncher.ui.overlay

import android.graphics.Bitmap
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs

/**
 * Full-screen wallpaper positioning overlay.
 *
 * Shows the wallpaper as a pannable/zoomable layer with real homescreen widgets
 * overlaid on top via [homescreenContent] so the user can see exactly how it looks.
 *
 * [onConfirm] returns (offsetX, offsetY, zoom) when the user taps "Done".
 */
@Composable
fun WallpaperCropOverlay(
    visible: Boolean,
    wallpaperBitmap: Bitmap?,
    initialOffsetX: Float,
    initialOffsetY: Float,
    initialZoom: Float = 1f,
    onConfirm: (offsetX: Float, offsetY: Float, zoom: Float) -> Unit,
    onDismiss: () -> Unit,
    homescreenContent: @Composable BoxScope.() -> Unit = {},
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit  = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)),
    ) {
        val haptic = LocalHapticFeedback.current

        var offsetX by remember(initialOffsetX) { mutableFloatStateOf(initialOffsetX) }
        var offsetY by remember(initialOffsetY) { mutableFloatStateOf(initialOffsetY) }
        var zoom    by remember(initialZoom)    { mutableFloatStateOf(initialZoom) }
        var containerSize by remember { mutableStateOf(IntSize.Zero) }
        var isDragging by remember { mutableStateOf(false) }

        // ── Alignment guide configuration ────────────────────────────────────
        // Show center guides when offset fraction is within ±snapShow of 0.5.
        // Hard-snap when within ±snapThreshold of 0.5.
        val snapThreshold  = 0.025f   // ~2.5% of screen
        val snapShowRadius = 0.06f    // guide appears within 6%
        val guideColor     = Color(0xFFFFD60A)   // same yellow as widget guides
        var snapFiredX by remember { mutableStateOf(false) }
        var snapFiredY by remember { mutableStateOf(false) }

        val isNearCenterX = isDragging && abs(offsetX - 0.5f) <= snapShowRadius
        val isNearCenterY = isDragging && abs(offsetY - 0.5f) <= snapShowRadius

        val vCenterAlpha by animateFloatAsState(
            targetValue   = if (isNearCenterX) 1f else 0f,
            animationSpec = tween(80),
            label         = "vCenterAlpha",
        )
        val hCenterAlpha by animateFloatAsState(
            targetValue   = if (isNearCenterY) 1f else 0f,
            animationSpec = tween(80),
            label         = "hCenterAlpha",
        )

        val imageBitmap = remember(wallpaperBitmap) { wallpaperBitmap?.asImageBitmap() }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .onSizeChanged { containerSize = it }
                // Block all touches from falling through to the launcher below
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
        ) {
            // ── Wallpaper layer ──────────────────────────────────────────────
            if (imageBitmap != null && containerSize != IntSize.Zero) {
                val bitmapW = wallpaperBitmap!!.width.toFloat()
                val bitmapH = wallpaperBitmap.height.toFloat()
                val containerW = containerSize.width.toFloat()
                val containerH = containerSize.height.toFloat()

                // Base scale that fills the screen at zoom = 1
                val baseScaleX = containerW / bitmapW
                val baseScaleY = containerH / bitmapH
                val baseScale  = maxOf(baseScaleX, baseScaleY)

                // Effective scale with user zoom applied
                val effectiveScale = baseScale * zoom

                val scaledW = bitmapW * effectiveScale
                val scaledH = bitmapH * effectiveScale

                val maxDragX = ((scaledW - containerW) / 2f).coerceAtLeast(0f)
                val maxDragY = ((scaledH - containerH) / 2f).coerceAtLeast(0f)

                val translX = (offsetX - 0.5f) * 2f * maxDragX
                val translY = (offsetY - 0.5f) * 2f * maxDragY

                Image(
                    bitmap = imageBitmap,
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            scaleX = effectiveScale / baseScaleX
                            scaleY = effectiveScale / baseScaleX
                            translationX = -translX
                            translationY = -translY
                        }
                        // Combined pan + pinch gesture on the wallpaper image
                        .pointerInput(containerSize) {
                            awaitEachGesture {
                                awaitFirstDown(requireUnconsumed = false)
                                isDragging = true
                                do {
                                    val event = awaitPointerEvent()
                                    val panDelta  = event.calculatePan()
                                    val zoomDelta = event.calculateZoom()

                                    // Apply zoom (clamp: 0.5x – 4x relative to fill)
                                    if (zoomDelta != 1f) {
                                        zoom = (zoom * zoomDelta).coerceIn(0.5f, 4f)
                                    }

                                    // Re-compute drag limits after zoom change
                                    val newScaled = bitmapW * baseScale * zoom
                                    val newScaledH = bitmapH * baseScale * zoom
                                    val newMaxDragX = ((newScaled  - containerW) / 2f).coerceAtLeast(0f)
                                    val newMaxDragY = ((newScaledH - containerH) / 2f).coerceAtLeast(0f)

                                    if (newMaxDragX > 0f) {
                                        var newX = (offsetX - panDelta.x / (2f * newMaxDragX)).coerceIn(0f, 1f)
                                        // Snap to horizontal center
                                        if (abs(newX - 0.5f) < snapThreshold) {
                                            newX = 0.5f
                                            if (!snapFiredX) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                snapFiredX = true
                                            }
                                        } else {
                                            snapFiredX = false
                                        }
                                        offsetX = newX
                                    }
                                    if (newMaxDragY > 0f) {
                                        var newY = (offsetY - panDelta.y / (2f * newMaxDragY)).coerceIn(0f, 1f)
                                        // Snap to vertical center
                                        if (abs(newY - 0.5f) < snapThreshold) {
                                            newY = 0.5f
                                            if (!snapFiredY) {
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                snapFiredY = true
                                            }
                                        } else {
                                            snapFiredY = false
                                        }
                                        offsetY = newY
                                    }

                                    event.changes.forEach { it.consume() }
                                } while (event.changes.any { it.pressed })
                                isDragging    = false
                                snapFiredX    = false
                                snapFiredY    = false
                            }
                        }
                )
            }

            // ── Homescreen widgets overlay ──────────────────────────────────
            // This slot renders AodClock + custom text exactly as on the homescreen.
            homescreenContent()

            // ── Alignment guide lines ────────────────────────────────────────
            // Yellow center guides appear while dragging, matching widget move mode.
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx = 1.5.dp.toPx()
                // Vertical center guide (full-height line at screen center X)
                if (vCenterAlpha > 0f) {
                    drawLine(
                        color       = guideColor.copy(alpha = vCenterAlpha),
                        start       = Offset(size.width / 2f, 0f),
                        end         = Offset(size.width / 2f, size.height),
                        strokeWidth = strokePx,
                    )
                }
                // Horizontal center guide (full-width line at screen center Y)
                if (hCenterAlpha > 0f) {
                    drawLine(
                        color       = guideColor.copy(alpha = hCenterAlpha),
                        start       = Offset(0f, size.height / 2f),
                        end         = Offset(size.width, size.height / 2f),
                        strokeWidth = strokePx,
                    )
                }
            }

            // ── Done / Cancel row + hint label at top ───────────────────────
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 48.dp)
            ) {
                // Cancel
                Box(
                    modifier = Modifier
                        .size(width = 130.dp, height = 52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0x22FFFFFF))
                        .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(26.dp))
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                val up = awaitPointerEvent()
                                if (up.changes.all { !it.pressed }) onDismiss()
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Cancel", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                }

                // Done
                Box(
                    modifier = Modifier
                        .size(width = 130.dp, height = 52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF5A4FD4), Color(0xFF7B6FEF)))
                        )
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                awaitFirstDown()
                                val up = awaitPointerEvent()
                                if (up.changes.all { !it.pressed }) onConfirm(offsetX, offsetY, zoom)
                            }
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                        Text("Done", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            // ── Hint label at bottom ─────────────────────────────────────────
            Text(
                text = "Pinch to zoom  •  Drag to reposition",
                color = Color(0xCCFFFFFF),
                fontSize = 12.sp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 24.dp)
                    .background(Color(0x88000000), RoundedCornerShape(20.dp))
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }
    }
}
