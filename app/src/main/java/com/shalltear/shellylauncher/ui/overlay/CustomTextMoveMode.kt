package com.shalltear.shellylauncher.ui.overlay

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.shalltear.shellylauncher.utils.CustomTextWidgetPrefs
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun CustomTextMoveMode(
    customText: String,
    customFontSizeSp: Float,
    customLetterSpacing: Float,
    customAlignment: String,
    customFontFamily: FontFamily?,
    customTextVisible: Boolean,
    customFontFilename: String?,
    customTextColorArgb: Int,
    customTextOpacityPct: Float,
    initialXFraction: Float,
    initialYFraction: Float,
    initialWidthFraction: Float,
    aodClockTopPx: Float,
    onExit: (xFraction: Float, yFraction: Float, widthFraction: Float) -> Unit,
    onHideAndExit: (xFraction: Float, yFraction: Float, widthFraction: Float) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val haptic  = LocalHapticFeedback.current
    val windowSize = LocalWindowInfo.current.containerSize
    val textMeasurer = rememberTextMeasurer()

    // Measure the intrinsic width of the actual text glyphs (first px → last px).
    // This is narrower than the container whenever text doesn't fill it, and is
    // what we use for all horizontal-center calculations instead of the box width.
    val actualTextWidthPx: Float = remember(customText, customFontSizeSp, customLetterSpacing, customFontFamily) {
        textMeasurer.measure(
            text  = customText.ifBlank { "Custom Text" },
            style = TextStyle(
                fontFamily    = customFontFamily ?: FontFamily.Default,
                fontSize      = customFontSizeSp.sp,
                letterSpacing = customLetterSpacing.em,
            )
        ).size.width.toFloat()
    }

    val screenWidthPx = windowSize.width.toFloat()
    val screenHeightPx = windowSize.height.toFloat()

    val maxYFraction = if (aodClockTopPx < screenHeightPx) {
        ((aodClockTopPx - with(density) { 12.dp.toPx() }) / screenHeightPx).coerceIn(0f, 0.85f)
    } else 0.78f

    var moveXFraction     by remember { mutableFloatStateOf(initialXFraction) }
    var moveYFraction     by remember { mutableFloatStateOf(initialYFraction) }
    var moveWidthFraction by remember { mutableFloatStateOf(initialWidthFraction) }
    var textHeightPx      by remember { mutableFloatStateOf(200f) }
    var isDragging        by remember { mutableStateOf(false) }

    // ── Snap / alignment guide configuration ──────────────────────────────────
    // Show guide line when within snapShowPx; hard-snap when within snapPx.
    val snapPx     = with(density) { 10.dp.toPx() }
    val snapShowPx = with(density) { 24.dp.toPx() }
    val guideColor = Color(0xFFFFD60A) // Instagram / iOS yellow

    // One-shot haptic flags reset on drag end so re-entry fires again
    var snapFiredCenterX by remember { mutableStateOf(false) }
    var snapFiredCenterY by remember { mutableStateOf(false) }
    var snapFiredClock   by remember { mutableStateOf(false) }

    // Derived widget geometry (px).
    // widgetCenterXPx is the visual center of the TEXT GLYPHS, not the container edge.
    //   left  → text starts at containerLeft; glyph center = containerLeft + textW/2
    //   right → text ends at containerRight; glyph center = containerRight − textW/2
    //   center → text centred in container; glyph center = container centre (same result)
    val widgetCenterXPx = when (customAlignment) {
        "left"  -> moveXFraction * screenWidthPx + actualTextWidthPx / 2f
        "right" -> (moveXFraction + moveWidthFraction) * screenWidthPx - actualTextWidthPx / 2f
        else    -> moveXFraction * screenWidthPx + moveWidthFraction * screenWidthPx / 2f
    }
    val widgetCenterYPx = moveYFraction * screenHeightPx + textHeightPx / 2f
    val widgetBottomYPx = moveYFraction * screenHeightPx + textHeightPx

    // Guides visible when actively dragging and within the show radius
    val isNearCenterX = isDragging && abs(widgetCenterXPx - screenWidthPx / 2f) <= snapShowPx
    val isNearCenterY = isDragging && abs(widgetCenterYPx - screenHeightPx / 2f) <= snapShowPx
    val isNearClock   = isDragging && aodClockTopPx < screenHeightPx &&
                        abs(widgetBottomYPx - aodClockTopPx) <= snapShowPx

    // Animated opacity for smooth appear / disappear
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
    val clockGuideAlpha by animateFloatAsState(
        targetValue   = if (isNearClock) 1f else 0f,
        animationSpec = tween(80),
        label         = "clockAlpha",
    )

    // ── iPhone-style jiggle (wobble) ──────────────────────────────────────────
    val infiniteTransition = rememberInfiniteTransition(label = "jiggle")
    val jiggleAngle by infiniteTransition.animateFloat(
        initialValue  = -1.8f,
        targetValue   =  1.8f,
        animationSpec = infiniteRepeatable(
            animation  = tween(durationMillis = 130, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "jiggleAngle",
    )

    // Lift scale when picked up (spring bounce on release)
    val liftScale by animateFloatAsState(
        targetValue   = if (isDragging) 1.02f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label         = "liftScale",
    )

    fun saveAndExit() {
        CustomTextWidgetPrefs.save(
            context,
            CustomTextWidgetPrefs.Settings(
                text            = customText,
                fontFilename    = customFontFilename,
                letterSpacing   = customLetterSpacing,
                alignment       = customAlignment,
                offsetXFraction = moveXFraction,
                offsetYFraction = moveYFraction,
                widthFraction   = moveWidthFraction,
                fontSizeSp      = customFontSizeSp,
                visible         = customTextVisible,
                textColorArgb   = customTextColorArgb,
                opacityPct      = customTextOpacityPct.toInt(),
            )
        )
        onExit(moveXFraction, moveYFraction, moveWidthFraction)
    }

    // Done pill fades in after 300ms so it doesn't distract during initial drag
    var donePillVisible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(300)
        donePillVisible = true
    }
    val donePillAlpha by animateFloatAsState(
        targetValue = if (donePillVisible) 1f else 0f,
        animationSpec = tween(250),
        label = "donePillAlpha",
    )

    // Hardware back button → save current position and exit move mode (same as “Done”).
    BackHandler { saveAndExit() }

    // ── Root: full-screen dim; tap outside widget to save & exit ──────────────
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .pointerInput(Unit) {
                detectTapGestures { tap ->
                    val padPx  = with(density) { 20.dp.toPx() }
                    val wLeft  = moveXFraction * screenWidthPx  - padPx
                    val wTop   = moveYFraction * screenHeightPx - padPx
                    val wRight = wLeft + moveWidthFraction * screenWidthPx + padPx * 2f
                    val wBot   = wTop  + padPx * 2f + textHeightPx
                    if (tap.x < wLeft || tap.x > wRight || tap.y < wTop || tap.y > wBot) {
                        saveAndExit()
                    }
                }
            }
    ) {

        // ── "Done" pill — top-right, iOS blue ─────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 52.dp, end = 20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(Color.White)
                .pointerInput(Unit) { detectTapGestures { saveAndExit() } }
                .padding(horizontal = 18.dp, vertical = 9.dp)
                .graphicsLayer { alpha = donePillAlpha }
        ) {
            Text(
                text       = "Done",
                color      = Color(0xFF007AFF),
                fontSize   = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        // ── Widget card ────────────────────────────────────────────────────────
        // The card is offset outward by `cardInset` so the border/background extend
        // around the text without displacing it. The text inside the card has no
        // padding, so it starts at exactly (moveXFraction*W, moveYFraction*H) —
        // the same pixel as the homescreen Text composable. Perfect sync.
        val cardInsetPx = with(density) { 4.dp.toPx() }
        val cardInsetDp = with(density) { 4.dp }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (moveXFraction * screenWidthPx - cardInsetPx).toInt(),
                        y = (moveYFraction * screenHeightPx - cardInsetPx).toInt(),
                    )
                }
                .width(with(density) { (moveWidthFraction * screenWidthPx).toDp() + cardInsetDp * 2 })
                .graphicsLayer {
                    rotationZ = if (!isDragging) jiggleAngle else 0f
                    scaleX    = liftScale
                    scaleY    = liftScale
                }
                .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                .background(Color.White.copy(alpha = 0.07f), RoundedCornerShape(16.dp))
                .padding(cardInsetDp)
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = {
                            isDragging = true
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        },
                        onDragEnd = {
                            isDragging       = false
                            snapFiredCenterX = false
                            snapFiredCenterY = false
                            snapFiredClock   = false
                        },
                        onDragCancel = {
                            isDragging       = false
                            snapFiredCenterX = false
                            snapFiredCenterY = false
                            snapFiredClock   = false
                        },
                        onDrag = { change, dragAmount ->
                            change.consume()

                            val wWidthPx = moveWidthFraction * screenWidthPx

                            var newX = (moveXFraction + dragAmount.x / screenWidthPx)
                                .coerceIn(0f, (1f - moveWidthFraction).coerceAtLeast(0f))
                            var newY = (moveYFraction + dragAmount.y / screenHeightPx)
                                .coerceIn(0f, maxYFraction)

                            // ── Snap: horizontal center (based on text glyph bounds) ──
                            // Compute where the text content centre currently is, then snap
                            // the container so the glyphs land exactly on screen centre.
                            val newTextCenterXPx = when (customAlignment) {
                                "left"  -> newX * screenWidthPx + actualTextWidthPx / 2f
                                "right" -> (newX + moveWidthFraction) * screenWidthPx - actualTextWidthPx / 2f
                                else    -> newX * screenWidthPx + wWidthPx / 2f
                            }
                            if (abs(newTextCenterXPx - screenWidthPx / 2f) < snapPx) {
                                // Solve for containerLeft so that text glyph centre == screenW/2,
                                // then round to nearest pixel to avoid sub-pixel drift.
                                val snappedXPx = when (customAlignment) {
                                    "left"  -> (screenWidthPx / 2f - actualTextWidthPx / 2f)
                                                    .roundToInt().toFloat()
                                                    .coerceIn(0f, (screenWidthPx - wWidthPx).coerceAtLeast(0f))
                                    "right" -> (screenWidthPx / 2f + actualTextWidthPx / 2f - wWidthPx)
                                                    .roundToInt().toFloat()
                                                    .coerceIn(0f, (screenWidthPx - wWidthPx).coerceAtLeast(0f))
                                    else    -> ((screenWidthPx - wWidthPx) / 2f).roundToInt().toFloat()
                                }
                                newX = snappedXPx / screenWidthPx
                                if (!snapFiredCenterX) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    snapFiredCenterX = true
                                }
                            } else {
                                snapFiredCenterX = false
                            }

                            // ── Snap: vertical center ──────────────────────
                            val newCenterYPx = newY * screenHeightPx + textHeightPx / 2f
                            if (abs(newCenterYPx - screenHeightPx / 2f) < snapPx) {
                                newY = ((screenHeightPx / 2f - textHeightPx / 2f) / screenHeightPx)
                                    .coerceIn(0f, maxYFraction)
                                if (!snapFiredCenterY) {
                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    snapFiredCenterY = true
                                }
                            } else {
                                snapFiredCenterY = false
                            }

                            // ── Snap: widget bottom to AOD clock top ───────
                            if (aodClockTopPx < screenHeightPx) {
                                val newBottomYPx = newY * screenHeightPx + textHeightPx
                                if (abs(newBottomYPx - aodClockTopPx) < snapPx) {
                                    newY = ((aodClockTopPx - textHeightPx) / screenHeightPx)
                                        .coerceIn(0f, maxYFraction)
                                    if (!snapFiredClock) {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        snapFiredClock = true
                                    }
                                } else {
                                    snapFiredClock = false
                                }
                            }

                            moveXFraction = newX
                            moveYFraction = newY
                        }
                    )
                }
        ) {
            Text(
                text          = customText.ifBlank { "Custom Text" },
                fontFamily    = customFontFamily ?: FontFamily.Default,
                fontSize      = customFontSizeSp.sp,
                letterSpacing = customLetterSpacing.em,
                textAlign     = when (customAlignment) {
                    "left"  -> TextAlign.Start
                    "right" -> TextAlign.End
                    else    -> TextAlign.Center
                },
                color    = Color(customTextColorArgb).copy(alpha = customTextOpacityPct / 100f),
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { textHeightPx = it.size.height.toFloat() },
            )
        }

        // ── Resize handle — outside the card at bottom-right corner ───────────
        // Kept as a sibling so it cannot inflate the card's measured height or width.
        // Positioned at (cardRight - half, cardBottom - half) so it straddles the corner.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset {
                    val handleHalfPx = 9.dp.toPx()   // half of 18dp handle
                    IntOffset(
                        x = (moveXFraction * screenWidthPx + moveWidthFraction * screenWidthPx + cardInsetPx - handleHalfPx).toInt(),
                        y = (moveYFraction * screenHeightPx + textHeightPx + cardInsetPx - handleHalfPx).toInt(),
                    )
                }
                .size(18.dp)
                .graphicsLayer {
                    rotationZ = if (!isDragging) jiggleAngle else 0f
                    scaleX    = liftScale
                    scaleY    = liftScale
                }
                .background(Color.White, CircleShape)
                .border(1.dp, Color(0xFFBBBBBB), CircleShape)
                .pointerInput(Unit) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        moveWidthFraction =
                            (moveWidthFraction + dragAmount.x / screenWidthPx)
                                .coerceIn(0.15f, 1f - moveXFraction)
                    }
                }
        ) {
            Text("\u21D4", color = Color(0xFF333333), fontSize = 9.sp)
        }

        // ── Minus (−) badge — iOS red, top-left corner of widget ──────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (moveXFraction * screenWidthPx  - 9.dp.toPx()).toInt(),
                        y = (moveYFraction * screenHeightPx - 9.dp.toPx()).toInt(),
                    )
                }
                .size(18.dp)
                .graphicsLayer {
                    rotationZ = if (!isDragging) jiggleAngle else 0f
                    scaleX    = liftScale
                    scaleY    = liftScale
                }
                .background(Color(0xFFFF3B30), CircleShape)
                .border(1.5.dp, Color.White, CircleShape)
                .pointerInput(Unit) {
                    detectTapGestures {
                        CustomTextWidgetPrefs.save(
                            context,
                            CustomTextWidgetPrefs.Settings(
                                text            = customText,
                                fontFilename    = customFontFilename,
                                letterSpacing   = customLetterSpacing,
                                alignment       = customAlignment,
                                offsetXFraction = moveXFraction,
                                offsetYFraction = moveYFraction,
                                widthFraction   = moveWidthFraction,
                                fontSizeSp      = customFontSizeSp,
                                visible         = false,
                                textColorArgb   = customTextColorArgb,
                                opacityPct      = customTextOpacityPct.toInt(),
                            )
                        )
                        onHideAndExit(moveXFraction, moveYFraction, moveWidthFraction)
                    }
                }
        ) {
            Text("\u2212", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        }

        // ── Alignment guide lines (drawn last = top Z-order) ──────────────────
        // Thin yellow lines appear as the widget approaches an alignment anchor,
        // matching the Instagram Stories / iOS widget edit feel.
        val strokePx = with(density) { 1.5.dp.toPx() }
        Canvas(modifier = Modifier.fillMaxSize()) {

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

            // AOD clock-top guide (full-width line at clock top edge)
            if (clockGuideAlpha > 0f && aodClockTopPx < size.height) {
                drawLine(
                    color       = guideColor.copy(alpha = clockGuideAlpha),
                    start       = Offset(0f, aodClockTopPx),
                    end         = Offset(size.width, aodClockTopPx),
                    strokeWidth = strokePx,
                )
            }
        }
    }
}