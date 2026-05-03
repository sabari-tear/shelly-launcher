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
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val coroutineScope = rememberCoroutineScope()
    
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var isAppsVisible by remember { mutableStateOf(false) }
    var fingerPosition by remember { mutableStateOf(Offset.Zero) }
    var glowCenter by remember { mutableStateOf(Offset.Zero) }
    var itemBounds by remember { mutableStateOf(mapOf<Int, Rect>()) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    
    val gridState = rememberLazyGridState()
    val density = LocalDensity.current

    // Load apps on startup
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            apps = loadInstalledApps(context)
        }
    }

    // Determine selected app based on finger position
    LaunchedEffect(fingerPosition, isAppsVisible, itemBounds) {
        if (!isAppsVisible) {
            selectedIndex = null
        } else {
            // Find the item whose bounds contain the finger position
            val matchedIndex = itemBounds.entries.firstOrNull { it.value.contains(fingerPosition) }?.key
            if (matchedIndex != selectedIndex) {
                selectedIndex = matchedIndex
            }
        }
    }

    // Auto-scroll logic
    LaunchedEffect(isAppsVisible) {
        while (isAppsVisible) {
            val y = fingerPosition.y
            val screenHeight = with(density) { context.resources.displayMetrics.heightPixels.toFloat() }
            val edgeThreshold = screenHeight * 0.15f // Top/Bottom 15% of screen
            
            val scrollSpeed = 25f
            if (y > 0 && y < edgeThreshold) {
                gridState.scrollBy(-scrollSpeed)
            } else if (y > screenHeight - edgeThreshold) {
                gridState.scrollBy(scrollSpeed)
            }
            delay(16) // ~60fps
        }
    }

    // Animation for glow
    val glowAlpha by animateFloatAsState(
        targetValue = if (isAppsVisible) 0.6f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "glowAlpha"
    )
    val glowRadius by animateFloatAsState(
        targetValue = if (isAppsVisible) 1000f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessLow),
        label = "glowRadius"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectDragGesturesAfterLongPress(
                    onDragStart = { offset ->
                        glowCenter = offset
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
        // Radial Glow Background
        if (glowAlpha > 0f) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(Color(0xFFE0E0E0).copy(alpha = glowAlpha), Color.Transparent),
                        center = glowCenter,
                        radius = maxOf(glowRadius, 1f)
                    ),
                    radius = glowRadius,
                    center = glowCenter
                )
            }
        }

        // App Grid
        AnimatedVisibility(
            visible = isAppsVisible,
            enter = fadeIn(tween(300)),
            exit = fadeOut(tween(300))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 80.dp),
                    state = gridState,
                    contentPadding = PaddingValues(top = 160.dp, bottom = 40.dp, start = 16.dp, end = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                        AppItem(
                            app = app,
                            isSelected = index == selectedIndex,
                            onPositioned = { rect ->
                                // Update bounds for intersection testing
                                val currentBounds = itemBounds.toMutableMap()
                                currentBounds[index] = rect
                                itemBounds = currentBounds
                            }
                        )
                    }
                }
                
                // Top App Name Display
                val selectedApp = selectedIndex?.let { apps.getOrNull(it) }
                if (selectedApp != null) {
                    Text(
                        text = selectedApp.name,
                        color = Color.White,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Light,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 64.dp, start = 24.dp, end = 24.dp)
                            .align(Alignment.TopCenter)
                    )
                }
            }
        }
    }
}

@Composable
fun AppItem(
    app: AppInfo,
    isSelected: Boolean,
    onPositioned: (Rect) -> Unit
) {
    // Animate scale. 1.5f as requested.
    val scale by animateFloatAsState(
        targetValue = if (isSelected) 1.5f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coordinates ->
                onPositioned(coordinates.boundsInRoot())
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                // Slight translation upward when selected for better visibility under finger
                translationY = if (isSelected) -20f else 0f
            }
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .background(Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = app.name,
                modifier = Modifier.fillMaxSize()
            )
        }
        // Minimalist label under icon, hidden when scaled up since big header shows it
        AnimatedVisibility(
            visible = !isSelected,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Text(
                text = app.name,
                color = Color(0x99FFFFFF),
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}