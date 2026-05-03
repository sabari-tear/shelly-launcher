package com.shalltear.shellylauncher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.math.sqrt

data class AppInfo(
    val name: String,
    val packageName: String,
    val icon: Bitmap,
    val launchIntent: Intent?
)

fun loadInstalledApps(context: Context): List<AppInfo> {
    val pm = context.packageManager
    val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
    }
    val resolvedActivities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(
            mainIntent,
            PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
        )
    } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(mainIntent, PackageManager.MATCH_ALL)
    }

    return resolvedActivities
        .map { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo
            AppInfo(
                name = resolveInfo.loadLabel(pm).toString(),
                packageName = activityInfo.packageName,
                icon = drawableToBitmap(resolveInfo.loadIcon(pm)),
                launchIntent = Intent(Intent.ACTION_MAIN).apply {
                    addCategory(Intent.CATEGORY_LAUNCHER)
                    setClassName(activityInfo.packageName, activityInfo.name)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }
        .distinctBy { it.launchIntent?.component?.flattenToShortString() }
        .sortedBy { it.name.lowercase() }
}

fun drawableToBitmap(drawable: Drawable, size: Int = 96): Bitmap {
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, size, size)
    drawable.draw(canvas)
    return bitmap
}

fun lerp(start: Float, stop: Float, fraction: Float): Float {
    return start + (stop - start) * fraction
}

fun createLightUpIcon(): Bitmap {
    val size = 128
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = android.graphics.Paint().apply {
        color = android.graphics.Color.parseColor("#FFD700") // Gold
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2.1f, paint)
    
    val iconPaint = android.graphics.Paint().apply {
        color = android.graphics.Color.WHITE
        isAntiAlias = true
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 8f
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 4f, iconPaint)
    canvas.drawLine(size / 2f, size / 4f, size / 2f, size / 8f, iconPaint)
    canvas.drawLine(size / 2f, size * 3/4f, size / 2f, size * 7/8f, iconPaint)
    canvas.drawLine(size / 4f, size / 2f, size / 8f, size / 2f, iconPaint)
    canvas.drawLine(size * 3/4f, size / 2f, size * 7/8f, size / 2f, iconPaint)
    return bitmap
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            LauncherScreen()
        }
    }
}

@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val density = LocalDensity.current

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var appPositions by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var hexRadiusPx by remember { mutableStateOf(100f) }

    var isAppsVisible by remember { mutableStateOf(false) }
    var isLightUpMode by remember { mutableStateOf(false) }
    
    // Use MutableState to avoid recompositions. Read only inside graphicsLayer and onDrag
    val fingerPosition = remember { mutableStateOf(Offset(-1000f, -1000f)) }
    val selectedIndex = remember { mutableIntStateOf(-1) }

    // Load apps on startup
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val loadedApps = loadInstalledApps(context).toMutableList()
            val lightUpApp = AppInfo(
                name = "Light Up",
                packageName = "com.shelly.lightup",
                icon = createLightUpIcon(),
                launchIntent = null
            )
            val mid = loadedApps.size / 2
            loadedApps.add(mid, lightUpApp)
            apps = loadedApps
        }
    }

    // Calculate Honeycomb positions to fit perfectly on screen
    LaunchedEffect(apps) {
        if (apps.isEmpty()) return@LaunchedEffect
        
        val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
        
        // Reserve space for margins
        val availableWidth = screenWidth * 0.9f
        val availableHeight = screenHeight * 0.75f
        val n = apps.size

        // Calculate optimal hexagon radius to fit N items in available area
        // Area of hex = ~2.6 * R^2. Total area = availableWidth * availableHeight.
        val optimalR = sqrt((availableWidth * availableHeight) / (n * 2.598f))
        
        // Constrain R so it doesn't get ridiculously large or small
        val maxRPx = with(density) { 55.dp.toPx() }
        val minRPx = with(density) { 20.dp.toPx() }
        val r = optimalR.coerceIn(minRPx, maxRPx)
        hexRadiusPx = r

        val hexWidth = sqrt(3f) * r
        val colSpacing = hexWidth
        val rowSpacing = 1.5f * r

        val cols = max(1, (availableWidth / colSpacing).toInt())
        val rows = (n + cols - 1) / cols

        val totalGridWidth = cols * colSpacing + colSpacing / 2f
        val totalGridHeight = (rows - 1) * rowSpacing + 2 * r

        // Center perfectly on screen
        val startX = (screenWidth - totalGridWidth) / 2f + colSpacing / 2f
        val startY = (screenHeight - totalGridHeight) / 2f + r

        val positions = mutableListOf<Offset>()
        apps.forEachIndexed { index, _ ->
            val row = index / cols
            val col = index % cols

            val xOffset = if (row % 2 == 1) colSpacing / 2f else 0f
            val x = startX + (col * colSpacing) + xOffset
            val y = startY + (row * rowSpacing)
            positions.add(Offset(x, y))
        }
        appPositions = positions
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(appPositions, hexRadiusPx) { // Re-bind if positions change
                awaitEachGesture {
                    val down = awaitFirstDown()

                    if (isAppsVisible && isLightUpMode) {
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            var closestIdx = -1
                            var minDist = Float.MAX_VALUE
                            appPositions.forEachIndexed { i, pos ->
                                val dist = (pos - up.position).getDistance()
                                if (dist < minDist) {
                                    minDist = dist
                                    closestIdx = i
                                }
                            }
                            if (minDist < hexRadiusPx * 0.75f) {
                                val app = apps.getOrNull(closestIdx)
                                if (app?.packageName == "com.shelly.lightup") {
                                    isAppsVisible = false
                                    isLightUpMode = false
                                } else {
                                    app?.launchIntent?.let { intent ->
                                        context.startActivity(intent)
                                    }
                                    isAppsVisible = false
                                    isLightUpMode = false
                                }
                            }
                        }
                        return@awaitEachGesture
                    }

                    // Not in light up mode, detect long press manually
                    var longPressDetected = false
                    try {
                        withTimeout(400L) {
                            var isDown = true
                            while (isDown) {
                                val event = awaitPointerEvent()
                                if (event.changes.any { !it.pressed }) {
                                    isDown = false
                                }
                            }
                        }
                    } catch (e: androidx.compose.ui.input.pointer.PointerEventTimeoutCancellationException) {
                        longPressDetected = true
                    }

                    if (longPressDetected) {
                        fingerPosition.value = down.position
                        isAppsVisible = true
                        isLightUpMode = false

                        var isDragging = true
                        while (isDragging) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                val pos = change.position
                                fingerPosition.value = pos
                                change.consume()

                                var closestIdx = -1
                                var minDist = Float.MAX_VALUE
                                appPositions.forEachIndexed { i, p ->
                                    val dist = (p - pos).getDistance()
                                    if (dist < minDist) {
                                        minDist = dist
                                        closestIdx = i
                                    }
                                }

                                val newSelection = if (minDist < hexRadiusPx * 0.75f) closestIdx else -1
                                if (selectedIndex.intValue != newSelection) {
                                    selectedIndex.intValue = newSelection
                                }

                                if (!change.pressed) {
                                    isDragging = false
                                }
                            } else {
                                isDragging = false
                            }
                        }

                        // Drag ended
                        val finalIdx = selectedIndex.intValue
                        if (finalIdx != -1) {
                            val app = apps.getOrNull(finalIdx)
                            if (app?.packageName == "com.shelly.lightup") {
                                isLightUpMode = true
                                selectedIndex.intValue = -1
                            } else {
                                app?.launchIntent?.let { intent ->
                                    context.startActivity(intent)
                                }
                                isAppsVisible = false
                            }
                        } else {
                            isAppsVisible = false
                        }
                    }
                }
            }
    ) {
        // App Grid - Honeycomb
        if (appPositions.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                apps.forEachIndexed { index, app ->
                    AppItem(
                        app = app,
                        index = index,
                        basePos = appPositions[index],
                        fingerPosition = fingerPosition,
                        isAppsVisible = isAppsVisible,
                        selectedIndex = selectedIndex,
                        isLightUpMode = isLightUpMode,
                        hexRadiusPx = hexRadiusPx
                    )
                }
            }
        }

        // Top App Name Display
        AppNameHeader(apps = apps, selectedIndex = selectedIndex, isVisible = isAppsVisible && !isLightUpMode)
    }
}

@Composable
fun AppNameHeader(apps: List<AppInfo>, selectedIndex: State<Int>, isVisible: Boolean) {
    val idx = selectedIndex.value
    val selectedApp = if (idx != -1) apps.getOrNull(idx) else null

    AnimatedVisibility(
        visible = isVisible && selectedApp != null,
        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
        exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
        modifier = Modifier.fillMaxWidth()
    ) {
        selectedApp?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Color(0x26FFFFFF),
                            RoundedCornerShape(50)
                        )
                        .border(
                            1.dp,
                            Color(0x33FFFFFF),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        bitmap = it.icon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = it.name.uppercase(),
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 2.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
fun AppItem(
    app: AppInfo,
    index: Int,
    basePos: Offset,
    fingerPosition: State<Offset>,
    isAppsVisible: Boolean,
    selectedIndex: State<Int>,
    isLightUpMode: Boolean,
    hexRadiusPx: Float
) {
    val isSelected = selectedIndex.value == index

    // Animate scale. Target values resolve during composition when `isSelected` changes.
    val scaleAnim by animateFloatAsState(
        targetValue = if (isSelected) 1.8f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "iconScale"
    )

    // Smooth transition for Light Up mode
    val lightUpProgress by animateFloatAsState(
        targetValue = if (isLightUpMode) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "lightUpTransition"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                val progress = lightUpProgress
                
                // If completely hidden and not lighting up, skip rendering
                if (!isAppsVisible && progress == 0f) {
                    alpha = 0f
                    scaleX = 0.5f
                    scaleY = 0.5f
                    return@graphicsLayer
                }

                val fingerPos = fingerPosition.value
                val dist = (basePos - fingerPos).getDistance()

                // 1. Calculate Spotlight Alpha
                val spotlightRadius = hexRadiusPx * 4f
                val normalAlpha = if (dist > spotlightRadius) 0f else {
                    val fade = 1f - (dist / spotlightRadius)
                    fade * fade * (3f - 2f * fade) // Smooth curve
                }

                // 2. Calculate Donut Repulsion Physics
                val repulsionRadius = hexRadiusPx * 3.5f
                val maxRepulsionForce = hexRadiusPx * 1.5f
                
                var normalOffsetX = basePos.x
                var normalOffsetY = basePos.y

                // Instead of snapping, use a parabolic curve that is 0 at center, max at half radius, 0 at edge
                if (dist < repulsionRadius && dist > 1f) {
                    val normalizedDist = dist / repulsionRadius
                    // 4 * x * (1 - x) is a perfect parabola peaking at 1 when x = 0.5
                    val force = maxRepulsionForce * 4f * normalizedDist * (1f - normalizedDist)
                    
                    val dirX = (basePos.x - fingerPos.x) / dist
                    val dirY = (basePos.y - fingerPos.y) / dist
                    normalOffsetX += dirX * force
                    normalOffsetY += dirY * force
                }

                // 3. Interpolate everything based on lightUpProgress
                alpha = lerp(normalAlpha, 1f, progress)
                
                val finalOffsetX = lerp(normalOffsetX, basePos.x, progress)
                val finalOffsetY = lerp(normalOffsetY, basePos.y, progress)
                translationX = finalOffsetX - hexRadiusPx
                translationY = finalOffsetY - hexRadiusPx
                
                val finalScale = lerp(scaleAnim, 1f, progress)
                scaleX = finalScale
                scaleY = finalScale
            }
            .size(with(LocalDensity.current) { (hexRadiusPx * 2).toDp() }),
        contentAlignment = Alignment.Center
    ) {
        // Draw glow behind icon
        if (scaleAnim > 1.05f) {
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
            bitmap = app.icon.asImageBitmap(),
            contentDescription = app.name,
            modifier = Modifier.fillMaxSize(0.7f)
        )
    }
}