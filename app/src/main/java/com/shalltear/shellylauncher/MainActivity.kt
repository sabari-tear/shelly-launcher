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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
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
import kotlinx.coroutines.delay
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
    var maxScroll by remember { mutableFloatStateOf(0f) }

    var isAppsVisible by remember { mutableStateOf(false) }
    var fingerPosition by remember { mutableStateOf(Offset(-1000f, -1000f)) }
    var scrollY by remember { mutableFloatStateOf(0f) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    val hexRadiusPx = with(density) { 38.dp.toPx() }
    val hexWidth = sqrt(3f) * hexRadiusPx
    val rowSpacing = 1.5f * hexRadiusPx
    val colSpacing = hexWidth

    // Load apps on startup
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val loadedApps = loadInstalledApps(context)
            apps = loadedApps
        }
    }

    // Calculate Honeycomb positions when apps or screen size changes
    LaunchedEffect(apps) {
        if (apps.isEmpty()) return@LaunchedEffect
        val screenWidth = context.resources.displayMetrics.widthPixels.toFloat()
        val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
        val cols = max(1, (screenWidth / colSpacing).toInt())
        val horizontalPadding = (screenWidth - (cols * colSpacing)) / 2f + colSpacing / 2f
        val topPadding = 400f

        val positions = mutableListOf<Offset>()
        apps.forEachIndexed { index, _ ->
            val row = index / cols
            val col = index % cols

            val xOffset = if (row % 2 == 1) colSpacing / 2f else 0f
            val x = (col * colSpacing) + xOffset + horizontalPadding
            val y = row * rowSpacing + topPadding
            positions.add(Offset(x, y))
        }
        appPositions = positions

        val maxY = positions.lastOrNull()?.y ?: 0f
        maxScroll = max(0f, maxY - screenHeight + topPadding)
    }

    // Selection & Auto-Scroll logic
    LaunchedEffect(isAppsVisible, fingerPosition) {
        if (!isAppsVisible || appPositions.isEmpty()) {
            selectedIndex = null
            return@LaunchedEffect
        }

        // Auto Scroll Loop
        val screenHeight = context.resources.displayMetrics.heightPixels.toFloat()
        val edgeThreshold = screenHeight * 0.15f
        val scrollSpeed = 20f

        if (fingerPosition.y > 0 && fingerPosition.y < edgeThreshold) {
            scrollY = (scrollY - scrollSpeed).coerceAtLeast(0f)
        } else if (fingerPosition.y > screenHeight - edgeThreshold) {
            scrollY = (scrollY + scrollSpeed).coerceAtMost(maxScroll)
        }

        // Selection Logic
        var closestIdx = -1
        var minDist = Float.MAX_VALUE
        appPositions.forEachIndexed { i, pos ->
            val screenPos = pos - Offset(0f, scrollY)
            val dist = (screenPos - fingerPosition).getDistance()
            if (dist < minDist) {
                minDist = dist
                closestIdx = i
            }
        }
        
        // Select if within a reasonable distance (e.g. hexRadius)
        if (minDist < hexRadiusPx * 1.5f) {
            if (selectedIndex != closestIdx) selectedIndex = closestIdx
        } else {
            if (selectedIndex != null) selectedIndex = null
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        fingerPosition = offset
                        isAppsVisible = true
                    },
                    onDrag = { change, _ ->
                        fingerPosition = change.position
                        change.consume()
                    },
                    onDragEnd = {
                        isAppsVisible = false
                        selectedIndex?.let { index ->
                            apps.getOrNull(index)?.launchIntent?.let { intent ->
                                context.startActivity(intent)
                            }
                        }
                    },
                    onDragCancel = {
                        isAppsVisible = false
                    }
                )
            }
    ) {
        // App Grid - Honeycomb
        if (appPositions.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                apps.forEachIndexed { index, app ->
                    val pos = appPositions[index]
                    val isSelected = index == selectedIndex

                    AppItem(
                        app = app,
                        basePos = pos,
                        fingerPositionProvider = { fingerPosition },
                        scrollYProvider = { scrollY },
                        isAppsVisibleProvider = { isAppsVisible },
                        isSelectedProvider = { isSelected },
                        hexRadiusPx = hexRadiusPx
                    )
                }
            }
        }

        // Top App Name Display
        val selectedApp = selectedIndex?.let { apps.getOrNull(it) }
        AnimatedVisibility(
            visible = isAppsVisible && selectedApp != null,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            selectedApp?.let {
                Text(
                    text = it.name,
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Light,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 80.dp, start = 24.dp, end = 24.dp)
                )
            }
        }
    }
}

@Composable
fun AppItem(
    app: AppInfo,
    basePos: Offset,
    fingerPositionProvider: () -> Offset,
    scrollYProvider: () -> Float,
    isAppsVisibleProvider: () -> Boolean,
    isSelectedProvider: () -> Boolean,
    hexRadiusPx: Float
) {
    val density = LocalDensity.current

    val scaleAnim by animateFloatAsState(
        targetValue = if (isSelectedProvider()) 1.6f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "iconScale"
    )

    Box(
        modifier = Modifier
            .graphicsLayer {
                val fingerPos = fingerPositionProvider()
                val scrollY = scrollYProvider()
                val isVisible = isAppsVisibleProvider()

                if (!isVisible) {
                    alpha = 0f
                    scaleX = 0.5f
                    scaleY = 0.5f
                    return@graphicsLayer
                }

                val screenPos = basePos - Offset(0f, scrollY)
                val dist = (screenPos - fingerPos).getDistance()

                // Spotlight Alpha mask
                val spotlightRadius = with(density) { 160.dp.toPx() }
                alpha = if (dist > spotlightRadius) 0f else {
                    val fade = 1f - (dist / spotlightRadius)
                    // Apply a curve so the drop-off isn't linear but stays bright longer near center
                    fade * fade * (3f - 2f * fade) 
                }

                // Repulsion Physics
                val repulsionRadius = with(density) { 120.dp.toPx() }
                val maxRepulsionForce = with(density) { 36.dp.toPx() }
                
                var offsetX = screenPos.x
                var offsetY = screenPos.y

                if (dist < repulsionRadius && dist > 1f && !isSelectedProvider()) {
                    val force = maxRepulsionForce * (1f - dist / repulsionRadius)
                    val dirX = (screenPos.x - fingerPos.x) / dist
                    val dirY = (screenPos.y - fingerPos.y) / dist
                    offsetX += dirX * force
                    offsetY += dirY * force
                }

                // Center the box over the point
                translationX = offsetX - hexRadiusPx
                translationY = offsetY - hexRadiusPx
                
                scaleX = scaleAnim
                scaleY = scaleAnim
            }
            .size(with(density) { (hexRadiusPx * 2).toDp() }),
        contentAlignment = Alignment.Center
    ) {
        Image(
            bitmap = app.icon.asImageBitmap(),
            contentDescription = app.name,
            modifier = Modifier
                .fillMaxSize(0.75f) // Makes the icon slightly smaller than the full hex radius
                .background(Color(0x1AFFFFFF), RoundedCornerShape(14.dp))
                .padding(6.dp)
        )
    }
}