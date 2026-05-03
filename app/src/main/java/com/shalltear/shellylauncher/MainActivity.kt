package com.shalltear.shellylauncher

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Build
import android.content.pm.PackageManager
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInteropFilter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max

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
    val canvas = Canvas(bitmap)
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val drawerGridState = rememberLazyGridState()

    var showApps by remember { mutableStateOf(false) }
    var currentPos by remember { mutableStateOf(Offset.Zero) }
    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var selectedIndex by remember { mutableStateOf(-1) }
    var autoScrollDirection by remember { mutableStateOf(0) }
    // Use an array as a mutable ref to avoid spurious recompositions
    val longPressJobRef = remember { arrayOfNulls<Job>(1) }

    val minCellWidthDp = 86.dp
    val cellHeightDp = 104.dp
    val edgeZonePx = with(density) { 84.dp.toPx() }
    val autoScrollStepPx = with(density) { 20.dp.toPx() }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val containerHeightPx = with(density) { maxHeight.toPx() }
        val containerWidthPx = with(density) { maxWidth.toPx() }
        val minCellWidthPx = with(density) { minCellWidthDp.toPx() }
        val cellHeightPx = with(density) { cellHeightDp.toPx() }
        val columns = max(1, (containerWidthPx / minCellWidthPx).toInt())
        val cellWidthPx = containerWidthPx / columns

        fun updateSelectionFromFinger() {
            if (!showApps || apps.isEmpty()) {
                selectedIndex = -1
                return
            }
            val rowOffset = drawerGridState.firstVisibleItemIndex / columns
            val absoluteY = currentPos.y +
                rowOffset * cellHeightPx +
                drawerGridState.firstVisibleItemScrollOffset
            val row = (absoluteY / cellHeightPx).toInt().coerceAtLeast(0)
            val col = (currentPos.x / cellWidthPx).toInt().coerceIn(0, columns - 1)
            val idx = row * columns + col
            selectedIndex = idx.coerceIn(0, apps.lastIndex)
        }

        LaunchedEffect(showApps, autoScrollDirection, columns) {
            if (!showApps || autoScrollDirection == 0) return@LaunchedEffect
            // Continue scrolling while finger stays near the top/bottom edge.
            while (showApps && autoScrollDirection != 0) {
                drawerGridState.scrollBy(autoScrollDirection * autoScrollStepPx)
                updateSelectionFromFinger()
                delay(16L)
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInteropFilter { event ->
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            longPressJobRef[0]?.cancel()
                            autoScrollDirection = 0
                            selectedIndex = -1
                            currentPos = Offset(event.x, event.y)
                            longPressJobRef[0] = coroutineScope.launch {
                                delay(500L)
                                val loadedApps = if (apps.isEmpty()) {
                                    withContext(Dispatchers.IO) { loadInstalledApps(context) }
                                } else {
                                    apps
                                }
                                apps = loadedApps
                                showApps = true
                                updateSelectionFromFinger()
                            }
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            currentPos = Offset(event.x, event.y)
                            if (showApps) {
                                autoScrollDirection = when {
                                    currentPos.y < edgeZonePx -> -1
                                    currentPos.y > (containerHeightPx - edgeZonePx) -> 1
                                    else -> 0
                                }
                                updateSelectionFromFinger()
                            }
                            true
                        }
                        MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                            longPressJobRef[0]?.cancel()
                            autoScrollDirection = 0
                            if (showApps) {
                                val idx = selectedIndex
                                if (idx >= 0 && idx < apps.size) {
                                    apps[idx].launchIntent?.let { intent ->
                                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        context.startActivity(intent)
                                    }
                                }
                                showApps = false
                                selectedIndex = -1
                            }
                            true
                        }
                        else -> false
                    }
                }
        ) {
            if (showApps) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(columns),
                    state = drawerGridState,
                    modifier = Modifier
                        .fillMaxWidth(0.94f)
                        .fillMaxHeight(0.78f)
                        .align(Alignment.Center)
                        .background(Color(0xCC000000), RoundedCornerShape(24.dp))
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    userScrollEnabled = false,
                    contentPadding = PaddingValues(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(apps, key = { _, app -> app.packageName }) { index, app ->
                        AppDrawerItem(
                            app = app,
                            isSelected = index == selectedIndex,
                            height = cellHeightDp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AppDrawerItem(
    app: AppInfo,
    isSelected: Boolean,
    height: androidx.compose.ui.unit.Dp
) {
    val iconSize by animateDpAsState(
        targetValue = if (isSelected) 62.dp else 54.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "iconSize"
    )
    val cardScale by animateFloatAsState(
        targetValue = if (isSelected) 1.08f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardScale"
    )
    val tileColor by animateColorAsState(
        targetValue = if (isSelected) Color(0x33FFFFFF) else Color.Transparent,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "tileColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .graphicsLayer {
                scaleX = cardScale
                scaleY = cardScale
            }
            .background(tileColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 8.dp, vertical = 10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(iconSize)
                .background(Color(0x30FFFFFF), RoundedCornerShape(10.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                bitmap = app.icon.asImageBitmap(),
                contentDescription = app.name,
                modifier = Modifier.fillMaxSize()
            )
        }
        AnimatedVisibility(
            visible = isSelected,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Text(
                text = app.name,
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }
    }
}