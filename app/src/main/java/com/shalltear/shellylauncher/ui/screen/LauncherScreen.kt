package com.shalltear.shellylauncher.ui.screen

// Android
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.net.Uri

// Activity / Compose
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts

// Compose Animation
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut

// Compose Foundation
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width

// Compose Material
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text

// Compose Runtime
import androidx.compose.runtime.key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue

// Compose UI
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// AndroidX Core
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.net.toUri

// Project — data / repository / utils
import com.shalltear.shellylauncher.MainActivity
import com.shalltear.shellylauncher.data.AppInfo
import com.shalltear.shellylauncher.repository.loadFromMetadataCache
import com.shalltear.shellylauncher.repository.loadInstalledApps
import com.shalltear.shellylauncher.ui.composable.AppItem
import com.shalltear.shellylauncher.ui.composable.AppNameHeader
import com.shalltear.shellylauncher.ui.composable.AodClock
import com.shalltear.shellylauncher.ui.composable.AppSearchBar
import com.shalltear.shellylauncher.ui.composable.SearchResultsGrid
import com.shalltear.shellylauncher.ui.composable.FavoritesShelf
import com.shalltear.shellylauncher.ui.overlay.CustomTextMoveMode
import com.shalltear.shellylauncher.ui.overlay.CustomTextSettingsOverlay
import com.shalltear.shellylauncher.ui.overlay.LauncherSettingsOverlay
import com.shalltear.shellylauncher.ui.overlay.TimeWidgetSettingsOverlay
import com.shalltear.shellylauncher.ui.overlay.WallpaperSettingsOverlay
import com.shalltear.shellylauncher.ui.overlay.AppDrawerSettingsOverlay
import com.shalltear.shellylauncher.ui.overlay.WallpaperCropOverlay
import com.shalltear.shellylauncher.utils.CustomTextWidgetPrefs
import com.shalltear.shellylauncher.utils.DeviceCapabilities
import com.shalltear.shellylauncher.utils.SpatialGrid
import com.shalltear.shellylauncher.utils.TimeWidgetPrefs
import com.shalltear.shellylauncher.utils.WallpaperPrefs
import com.shalltear.shellylauncher.utils.AppGridPrefs
import com.shalltear.shellylauncher.utils.createLightUpIcon
import com.shalltear.shellylauncher.utils.ThemeManager
import com.shalltear.shellylauncher.utils.AppFavoritesPrefs
import com.shalltear.shellylauncher.utils.NotificationBadgeUtils
import com.shalltear.shellylauncher.utils.WeatherWidget

// Kotlin stdlib / Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sqrt

@Composable
fun LauncherScreen() {
    val context = LocalContext.current
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current
    val windowSize = LocalWindowInfo.current.containerSize
    val screenWidthPx = windowSize.width.toFloat()
    val screenHeightPx = windowSize.height.toFloat()

    var apps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var appPositions by remember { mutableStateOf<List<Offset>>(emptyList()) }
    var hexRadiusPx by remember { mutableFloatStateOf(100f) }

    var isAppsVisible by remember { mutableStateOf(false) }
    var isLightUpMode by remember { mutableStateOf(false) }
    var isSettingsVisible by remember { mutableStateOf(false) }
    var settingsPos by remember { mutableStateOf(Offset.Zero) }
    var settingsBoundsSize by remember { mutableStateOf(Offset.Zero) }

    // Use MutableState to avoid recompositions. Read only inside graphicsLayer and onDrag
    val fingerPosition = remember { mutableStateOf(Offset(-1000f, -1000f)) }
    val selectedIndex = remember { mutableIntStateOf(-1) }
    
    // Spatial grid rebuilt whenever appPositions changes; avoids O(n) scan on every move event.
    val spatialGrid = remember { mutableStateOf<SpatialGrid?>(null) }
    
    // Auto-detected from ActivityManager.isLowRamDevice(); user can override in Settings.
    var isLowEndDevice by remember { mutableStateOf(DeviceCapabilities.isLowRamDevice(context)) }

    // App Grid Settings
    val initialAppGridSettings = remember { AppGridPrefs.load(context) }
    var appLabelTheme by remember { mutableStateOf(initialAppGridSettings.labelTheme) }
    var hasOpenedSettings by remember { mutableStateOf(initialAppGridSettings.hasOpenedSettings) }

    // Time Widget Settings
    val initialTimeWidgetSettings = remember { TimeWidgetPrefs.load(context) }
    var showBatteryWidget by remember { mutableStateOf(initialTimeWidgetSettings.showBattery) }
    var showWifiWidget by remember { mutableStateOf(initialTimeWidgetSettings.showWifi) }
    var showCellularWidget by remember { mutableStateOf(initialTimeWidgetSettings.showCellular) }
    var showSpeedWidget by remember { mutableStateOf(initialTimeWidgetSettings.showSpeed) }
    var spacingWidget by remember { mutableFloatStateOf(initialTimeWidgetSettings.spacingDp.toFloat()) }
    var clockStatusSpacingWidget by remember { mutableFloatStateOf(initialTimeWidgetSettings.clockStatusSpacingDp.toFloat()) }
    var timeFontSizeWidget by remember { mutableFloatStateOf(initialTimeWidgetSettings.timeFontSizeSp.toFloat()) }
    var iconSizeWidget by remember { mutableFloatStateOf(initialTimeWidgetSettings.iconSizeDp.toFloat()) }
    var statusItemOrder by remember { mutableStateOf(initialTimeWidgetSettings.itemOrder) }
    var showTimeWidget by remember { mutableStateOf(initialTimeWidgetSettings.showTime) }
    var timeFormatWidget by remember { mutableStateOf(initialTimeWidgetSettings.timeFormat) }
    var showDateWidget by remember { mutableStateOf(initialTimeWidgetSettings.showDate) }
    var dateFormatWidget by remember { mutableStateOf(initialTimeWidgetSettings.dateFormat) }
    var twoLinesWidget by remember { mutableStateOf(initialTimeWidgetSettings.twoLines) }
    var alignmentWidget by remember { mutableStateOf(initialTimeWidgetSettings.alignment) }
    var marginStartWidget by remember { mutableFloatStateOf(initialTimeWidgetSettings.marginStartDp.toFloat()) }
    var marginEndWidget by remember { mutableFloatStateOf(initialTimeWidgetSettings.marginEndDp.toFloat()) }
    var timeWidgetColorArgb by remember { mutableIntStateOf(initialTimeWidgetSettings.textColorArgb) }
    var timeWidgetOpacityPct by remember { mutableFloatStateOf(initialTimeWidgetSettings.opacityPct.toFloat()) }
    var isTimeWidgetSettingsVisible by remember { mutableStateOf(false) }
    
    // sub-screens: null = Time Widget root, "time_date" / "status_icons" / "spacings"
    var timeWidgetSubScreen by remember { mutableStateOf<String?>(null) }

    // Wallpaper
    val initialWallpaperSettings = remember { WallpaperPrefs.load(context) }
    var wallpaperUriString by remember { mutableStateOf(initialWallpaperSettings.uri) }
    var wallpaperBlurRadius by remember { mutableFloatStateOf(initialWallpaperSettings.blurRadiusDp.toFloat()) }
    var wallpaperHomescreenBlurRadius by remember { mutableFloatStateOf(initialWallpaperSettings.homescreenBlurRadiusDp.toFloat()) }
    var wallpaperOpacityPct by remember { mutableFloatStateOf(initialWallpaperSettings.opacityPct.toFloat()) }
    var appDrawerOpacityPct by remember { mutableFloatStateOf(initialWallpaperSettings.appDrawerOpacityPct.toFloat()) }
    var cropOffsetXFraction by remember { mutableFloatStateOf(initialWallpaperSettings.cropOffsetXFraction) }
    var cropOffsetYFraction by remember { mutableFloatStateOf(initialWallpaperSettings.cropOffsetYFraction) }
    var wallpaperZoomScale by remember { mutableFloatStateOf(initialWallpaperSettings.zoomScale) }
    var wallpaperBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isWallpaperSettingsVisible by remember { mutableStateOf(false) }
    var isAppDrawerSettingsVisible by remember { mutableStateOf(false) }
    var isCropModeVisible by remember { mutableStateOf(false) }

    // Custom Text Widget
    val initialCustomTextSettings = remember { CustomTextWidgetPrefs.load(context) }
    var customText by remember { mutableStateOf(initialCustomTextSettings.text) }
    var customFontFilename by remember { mutableStateOf(initialCustomTextSettings.fontFilename) }
    var customLetterSpacing by remember { mutableFloatStateOf(initialCustomTextSettings.letterSpacing) }
    var customAlignment by remember { mutableStateOf(initialCustomTextSettings.alignment) }
    var customOffsetXFraction by remember { mutableFloatStateOf(initialCustomTextSettings.offsetXFraction) }
    var customOffsetYFraction by remember { mutableFloatStateOf(initialCustomTextSettings.offsetYFraction) }
    var customWidthFraction by remember { mutableFloatStateOf(initialCustomTextSettings.widthFraction) }
    var customFontSizeSp by remember { mutableFloatStateOf(initialCustomTextSettings.fontSizeSp) }
    var customTextVisible by remember { mutableStateOf(initialCustomTextSettings.visible) }
    var customTextColorArgb by remember { mutableIntStateOf(initialCustomTextSettings.textColorArgb) }
    var customTextOpacityPct by remember { mutableFloatStateOf(initialCustomTextSettings.opacityPct.toFloat()) }
    var customFontFamily by remember { mutableStateOf<FontFamily?>(null) }
    var isCustomTextSettingsVisible by remember { mutableStateOf(false) }
    var isCustomTextMoveMode by remember { mutableStateOf(false) }
    var aodClockTopPx by remember { mutableFloatStateOf(Float.MAX_VALUE) }

    // Search and filtering
    var searchQuery by remember { mutableStateOf("") }
    var isSearchMode by remember { mutableStateOf(false) }
    val searchFocusRequester = remember { FocusRequester() }
    var searchBarPos by remember { mutableStateOf(Offset.Zero) }
    var searchBarBoundsSize by remember { mutableStateOf(Offset.Zero) }
    var searchBarRect by remember { mutableStateOf(Rect.Zero) }
    val keyboardController = LocalSoftwareKeyboardController.current

    // Show keyboard when entering search mode; hide when leaving
    LaunchedEffect(isSearchMode) {
        if (isSearchMode) {
            delay(80)
            try { searchFocusRequester.requestFocus() } catch (_: Exception) {}
            keyboardController?.show()
        } else {
            keyboardController?.hide()
        }
    }

    // Clear search when the app drawer fully closes
    LaunchedEffect(isAppsVisible) {
        if (!isAppsVisible) {
            searchQuery = ""
            isSearchMode = false
        }
    }

    // Favourites/pinning system
    var favoritePackages by remember { mutableStateOf(AppFavoritesPrefs.load(context).favorites) }
    val favoriteApps = apps.filter { favoritePackages.contains(it.packageName) }

    // Derived state: is the currently hovered app already a favourite?
    // Uses derivedStateOf so recomposition only triggers when the answer changes (not on every drag tick).
    val selectedAppIsFavorite by remember(favoritePackages) {
        androidx.compose.runtime.derivedStateOf {
            val idx = selectedIndex.intValue
            idx != -1 && favoritePackages.contains(apps.getOrNull(idx)?.packageName)
        }
    }

    // Position/size of the ☆ favourites-pin button (bottom-centre, mirrors the ⚙ gear at top)
    var favButtonPos by remember { mutableStateOf(Offset.Zero) }
    var favButtonBoundsSize by remember { mutableStateOf(Offset.Zero) }

    // Theme system
    val currentTheme = remember { ThemeManager.loadPalette(context) }

    // Loading gate: show loading screen until apps AND wallpaper (if any) are fully ready.
    // This prevents running layout / animations before caches are warm, which especially
    // helps low-end devices that would stutter on the honeycomb grid animation.
    var appsReady     by remember { mutableStateOf(false) }
    var wallpaperReady by remember { mutableStateOf(wallpaperUriString == null) } // no wallpaper → immediately ready
    val isReady = appsReady && wallpaperReady

    // Two-phase app loading:
    //   Phase 1 — persisted metadata (~1-5 ms, shows grid immediately on subsequent launches)
    //   Phase 2 — full PM query (~200-500 ms, authoritative, saves updated metadata)
    LaunchedEffect(Unit) {
        val lightUpApp = withContext(Dispatchers.Default) {
            AppInfo(
                name = "Light Up",
                packageName = "com.shelly.lightup",
                icon = createLightUpIcon(),
                launchIntent = null,
            )
        }

        // Phase 1: fast path
        val cached = withContext(Dispatchers.IO) { loadFromMetadataCache(context) }
        if (cached.isNotEmpty()) {
            apps = cached.toMutableList().also { it.add(it.size / 2, lightUpApp) }
        }

        // Phase 2: full refresh + persist updated metadata
        val fresh = withContext(Dispatchers.IO) { loadInstalledApps(context) }.toMutableList()
        fresh.add(fresh.size / 2, lightUpApp)
        apps = fresh
        appsReady = true
    }

    // Calculate Honeycomb positions to fit perfectly on screen
    LaunchedEffect(apps, screenWidthPx, screenHeightPx) {
        if (apps.isEmpty()) return@LaunchedEffect

        val screenWidth = screenWidthPx
        val screenHeight = screenHeightPx

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

        val cols = max(1, (availableWidth / colSpacing).roundToInt())
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
        // Rebuild spatial grid so drag events use O(1) bucket lookup instead of O(n) scan.
        val grid = SpatialGrid(screenWidth, screenHeight, r * 2f)
        grid.build(positions)
        spatialGrid.value = grid
        appPositions = positions
    }

    // Load wallpaper bitmap from persisted path/URI whenever it changes.
    // Stored value is either an absolute file path (new installs, copied to internal storage)
    // or a legacy content:// URI (older saves). Absolute paths never expire.
    LaunchedEffect(wallpaperUriString) {
        if (wallpaperUriString == null) {
            wallpaperReady = true
            wallpaperBitmap = null
            return@LaunchedEffect
        }
        wallpaperBitmap = wallpaperUriString?.let { uriStr ->
            withContext(Dispatchers.IO) {
                try {
                    val screenW = screenWidthPx.roundToInt()
                    val screenH = screenHeightPx.roundToInt()

                    // Open a stream: absolute path → FileInputStream, content URI → ContentResolver
                    fun openStream(): java.io.InputStream? = if (uriStr.startsWith("/")) {
                        java.io.FileInputStream(java.io.File(uriStr))
                    } else {
                        context.contentResolver.openInputStream(uriStr.toUri())
                    }

                    // First pass: read dimensions without decoding pixels
                    val opts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    openStream()?.use { BitmapFactory.decodeStream(it, null, opts) }

                    // Calculate inSampleSize to fit within screen resolution
                    var sampleSize = 1
                    var w = opts.outWidth
                    var h = opts.outHeight
                    while (w > screenW * 2 || h > screenH * 2) {
                        sampleSize *= 2
                        w /= 2
                        h /= 2
                    }

                    // Second pass: decode at reduced resolution
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = android.graphics.Bitmap.Config.RGB_565 // no alpha needed, saves 50% RAM
                    }
                    openStream()?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
                } catch (_: Exception) { null }
            }
        }
        // Wallpaper decode is done (success or failure) — unblock the loading screen
        wallpaperReady = true
    }

    // Cache the ImageBitmap wrapper so Compose reuses the same GPU texture across recompositions
    val wallpaperImageBitmap = remember(wallpaperBitmap) { wallpaperBitmap?.asImageBitmap() }

    // Load custom font from internal storage whenever the filename changes
    LaunchedEffect(customFontFilename) {
        customFontFamily = customFontFilename?.let { filename ->
            withContext(Dispatchers.IO) {
                try {
                    val file = File(CustomTextWidgetPrefs.fontsDir(context), filename)
                    if (file.exists() && (filename.endsWith(".ttf", ignoreCase = true) || filename.endsWith(".otf", ignoreCase = true))) {
                        try {
                            FontFamily(Typeface.createFromFile(file))
                        } catch (e: Exception) {
                            // Font file is invalid/corrupted — delete it and reset
                            file.delete()
                            null
                        }
                    } else {
                        null
                    }
                } catch (_: Exception) { null }
            }
        }
    }

    val coroutineScope = rememberCoroutineScope()

    val fontPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    // Validate file is TTF/OTF before copying
                    val mimeType = context.contentResolver.getType(uri) ?: ""
                    val filename = uri.lastPathSegment ?: "custom_font.ttf"
                    
                    if (filename.endsWith(".ttf", ignoreCase = true) || 
                        filename.endsWith(".otf", ignoreCase = true) ||
                        mimeType.contains("font", ignoreCase = true)) {
                        
                        val dest = File(CustomTextWidgetPrefs.fontsDir(context), "custom_font.ttf")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            dest.outputStream().use { output -> input.copyTo(output) }
                        }
                        
                        // Validate the copied font by trying to load it
                        try {
                            Typeface.createFromFile(dest)
                            kotlinx.coroutines.withContext(Dispatchers.Main) {
                                customFontFilename = "custom_font.ttf"
                            }
                        } catch (e: Exception) {
                            // Font is invalid — delete it
                            dest.delete()
                        }
                    }
                } catch (_: Exception) { }
            }
        }
    }

    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    // Copy the image into internal storage so we own the file and are never
                    // affected by Android revoking the content URI permission (which happens
                    // after process death, app updates, or gallery app changes).
                    val dest = java.io.File(context.filesDir, "wallpaper.jpg")
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        dest.outputStream().use { output -> input.copyTo(output) }
                    }
                    val localPath = dest.absolutePath
                    kotlinx.coroutines.withContext(Dispatchers.Main) {
                        WallpaperPrefs.save(context, WallpaperPrefs.Settings(
                            uri = localPath,
                            blurRadiusDp = wallpaperBlurRadius.toInt(),
                            homescreenBlurRadiusDp = wallpaperHomescreenBlurRadius.toInt(),
                            opacityPct = wallpaperOpacityPct.toInt(),
                            appDrawerOpacityPct = appDrawerOpacityPct.toInt(),
                            cropOffsetXFraction = 0.5f,
                            cropOffsetYFraction = 0.5f,
                            zoomScale = 1f,
                        ))
                        cropOffsetXFraction = 0.5f
                        cropOffsetYFraction = 0.5f
                        wallpaperZoomScale = 1f
                        wallpaperUriString = localPath
                        isCropModeVisible = true
                    }
                } catch (_: Exception) { }
            }
        }
    }

    // Hide the status bar on the homescreen; restore it when a settings overlay is open.
    // Move mode is deliberately excluded: it is a transparent overlay over the homescreen,
    // so the status bar must stay hidden to keep the layout coordinate space identical to
    // the homescreen. If the status bar were shown here the root Box would shift/resize,
    // causing the widget to appear at a different position than it does on the homescreen.
    val view = LocalView.current
    val anyOverlayOpen = isSettingsVisible || isTimeWidgetSettingsVisible || isWallpaperSettingsVisible ||
        isCustomTextSettingsVisible || isAppDrawerSettingsVisible || isCropModeVisible
    SideEffect {
        val window = (view.context as android.app.Activity).window
        val controller = WindowCompat.getInsetsController(window, view)
        if (anyOverlayOpen) {
            controller.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars())
            // Allow a swipe-down gesture to peek the status bar transiently
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    // Re-apply the correct status-bar visibility whenever the window regains focus
    // (e.g. after lock-screen unlock). SideEffect only fires on recomposition; if no Compose
    // state changes when the screen is unlocked, the status bar would stay visible without this.
    val currentAnyOverlayOpen = rememberUpdatedState(anyOverlayOpen)
    DisposableEffect(Unit) {
        val activity = view.context as? MainActivity
        activity?.let { act ->
            MainActivity.reapplyImmersive = {
                val controller = WindowCompat.getInsetsController(act.window, view)
                if (currentAnyOverlayOpen.value) {
                    controller.show(WindowInsetsCompat.Type.statusBars())
                } else {
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }
        onDispose { MainActivity.reapplyImmersive = null }
    }

    // ── Back-button navigation ────────────────────────────────────────────────
    // Compose BackHandler uses a LIFO stack — the LAST registered enabled handler wins.
    // Register from least to most specific so the correct screen is dismissed on back press.

    // Launcher-level fallback: catches back presses when the homescreen is fully visible.
    // Prevents the default ComponentActivity behaviour of calling finish() — a launcher
    // activity must NEVER close, even when its back stack is empty.
    BackHandler(enabled = true) { /* intentional no-op */ }

    // App grid / light-up mode — lowest priority overlay handlers
    BackHandler(enabled = isSearchMode) {
        isSearchMode = false
        searchQuery = ""
        isAppsVisible = false
    }
    BackHandler(enabled = isAppsVisible && !isSearchMode) {
        isAppsVisible = false
        isLightUpMode = false
    }
    BackHandler(enabled = isSettingsVisible) { isSettingsVisible = false }
    BackHandler(enabled = isWallpaperSettingsVisible) { isWallpaperSettingsVisible = false }
    BackHandler(enabled = isAppDrawerSettingsVisible) { isAppDrawerSettingsVisible = false }
    BackHandler(enabled = isCropModeVisible) { isCropModeVisible = false }
    BackHandler(enabled = isCustomTextSettingsVisible) { isCustomTextSettingsVisible = false }
    BackHandler(enabled = isTimeWidgetSettingsVisible && timeWidgetSubScreen == null) {
        isTimeWidgetSettingsVisible = false
    }
    BackHandler(enabled = isTimeWidgetSettingsVisible && timeWidgetSubScreen != null) {
        timeWidgetSubScreen = null
    }

    // ── Home-button: close all open overlays ─────────────────────────────────
    // When the user presses Home on the launcher itself, Android calls onNewIntent.
    // We respond by collapsing everything back to the clean homescreen state.
    LaunchedEffect(Unit) {
        MainActivity.homeEvents.collect {
            isAppsVisible = false
            isLightUpMode = false
            searchQuery = ""
            isSearchMode = false
            isSettingsVisible = false
            isTimeWidgetSettingsVisible = false
            timeWidgetSubScreen = null
            isWallpaperSettingsVisible = false
            isAppDrawerSettingsVisible = false
            isCropModeVisible = false
            isCustomTextSettingsVisible = false
            isCustomTextMoveMode = false
        }
    }

    // Blur wallpaper when the app grid is open; homescreen uses its own static blur
    val contentBlurRadius by animateDpAsState(
        targetValue = if (isAppsVisible || isLightUpMode) wallpaperBlurRadius.roundToInt().dp else wallpaperHomescreenBlurRadius.roundToInt().dp,
        animationSpec = tween(300),
        label = "contentBlur",
    )
    // Wallpaper alpha: on homescreen use wallpaperOpacityPct; in app drawer use appDrawerOpacityPct
    val wallpaperAlpha by animateFloatAsState(
        targetValue = if (isAppsVisible || isLightUpMode) appDrawerOpacityPct / 100f else wallpaperOpacityPct / 100f,
        animationSpec = tween(300),
        label = "wallpaperAlpha",
    )
    // Home page widgets (text, clock) fade to fully invisible when the app-selecting page opens
    // so they can never overlap or bleed through the app grid UI.
    val homepageAlpha by animateFloatAsState(
        targetValue = if (isAppsVisible || isLightUpMode) 0f else 1f,
        animationSpec = tween(300),
        label = "homepageAlpha",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(appPositions, hexRadiusPx) { // Re-bind if positions change
                awaitEachGesture {
                    val down = awaitFirstDown()

                    // Do not activate app grid while the custom text widget is being repositioned
                    if (isCustomTextMoveMode) {
                        waitForUpOrCancellation()
                        return@awaitEachGesture
                    }

                    // ── SEARCH MODE: SearchResultsGrid items handle their own taps via clickable.
                    // Any tap that reaches here hit the background → exit search mode.
                    if (isAppsVisible && isSearchMode) {
                        if (!searchBarRect.contains(down.position)) {
                            // Background tap: wait for finger-up then exit search mode
                            waitForUpOrCancellation()
                            isSearchMode = false
                            searchQuery = ""
                            isAppsVisible = false
                        }
                        // Touch on search bar falls through — BasicTextField handles it
                        return@awaitEachGesture
                    }

                    if (isAppsVisible && isLightUpMode) {
                        val up = waitForUpOrCancellation()
                        if (up != null) {
                            val thresholdSq = (hexRadiusPx * 0.75f) * (hexRadiusPx * 0.75f)
                            val closestIdx = spatialGrid.value
                                ?.findNearest(up.position, appPositions, thresholdSq) ?: -1
                            if (closestIdx != -1) {
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
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        fingerPosition.value = down.position
                        isAppsVisible = true
                        isLightUpMode = false

                        var isFingerOverSettings = false
                        var isFingerOverFav = false
                        var isFingerOverSearch = false
                        var isDragging = true
                        while (isDragging) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull()
                            if (change != null) {
                                val pos = change.position
                                fingerPosition.value = pos
                                change.consume()

                                // Track whether finger is hovering over settings icon
                                val sDx = settingsPos.x - pos.x
                                val sDy = settingsPos.y - pos.y
                                val hitRadius = maxOf(settingsBoundsSize.x, settingsBoundsSize.y) * 1.2f
                                isFingerOverSettings = (sDx * sDx + sDy * sDy) <= hitRadius * hitRadius

                                // Track whether finger is hovering over the favourites (★) button
                                val fDx = favButtonPos.x - pos.x
                                val fDy = favButtonPos.y - pos.y
                                val favHitRadius = maxOf(favButtonBoundsSize.x, favButtonBoundsSize.y) * 1.5f
                                isFingerOverFav = (fDx * fDx + fDy * fDy) <= favHitRadius * favHitRadius

                                // Track whether finger is hovering over the search bar pill
                                val srDx = searchBarPos.x - pos.x
                                val srDy = searchBarPos.y - pos.y
                                val searchHitW = searchBarBoundsSize.x * 0.55f
                                val searchHitH = searchBarBoundsSize.y * 2.5f
                                isFingerOverSearch = kotlin.math.abs(srDx) <= searchHitW &&
                                    kotlin.math.abs(srDy) <= searchHitH

                                val thresholdSq = (hexRadiusPx * 0.75f) * (hexRadiusPx * 0.75f)
                                val newSelection = spatialGrid.value
                                    ?.findNearest(pos, appPositions, thresholdSq) ?: -1
                                if (selectedIndex.intValue != newSelection) {
                                    selectedIndex.intValue = newSelection
                                }

                                if (!change.pressed) {
                                    isDragging = false
                                    // Open settings if finger was released over the settings icon
                                    if (isFingerOverSettings) {
                                        isSettingsVisible = true
                                        isAppsVisible = false
                                        selectedIndex.intValue = -1
                                        fingerPosition.value = Offset(-1000f, -1000f)
                                        return@awaitEachGesture
                                    }
                                    // Activate search mode if released over the search bar
                                    if (isFingerOverSearch) {
                                        isSearchMode = true
                                        selectedIndex.intValue = -1
                                        fingerPosition.value = Offset(-1000f, -1000f)
                                        return@awaitEachGesture
                                    }

                                    // Pin/unpin if finger released over the favourites button
                                    if (isFingerOverFav && selectedIndex.intValue != -1) {
                                        val app = apps.getOrNull(selectedIndex.intValue)
                                        if (app != null) {
                                            AppFavoritesPrefs.toggleFavorite(context, app.packageName)
                                            favoritePackages = AppFavoritesPrefs.load(context).favorites
                                            hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                        }
                                        isAppsVisible = false
                                        selectedIndex.intValue = -1
                                        fingerPosition.value = Offset(-1000f, -1000f)
                                        return@awaitEachGesture
                                    }
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
        // Wallpaper background — rendered first so it sits behind everything
        wallpaperImageBitmap?.let { bmp ->
            val screenW = screenWidthPx
            val screenH = screenHeightPx
            val bitmapW = bmp.width.toFloat()
            val bitmapH = bmp.height.toFloat()
            val baseScaleX = screenW / bitmapW
            val baseScaleY = screenH / bitmapH
            // effectiveScale = the scale that fills the screen at zoom=1, then multiplied by user zoom
            val effectiveScale = maxOf(baseScaleX, baseScaleY) * wallpaperZoomScale
            val scaledW = bitmapW * effectiveScale
            val scaledH = bitmapH * effectiveScale
            val maxDragX = ((scaledW - screenW) / 2f).coerceAtLeast(0f)
            val maxDragY = ((scaledH - screenH) / 2f).coerceAtLeast(0f)
            val translX = -(cropOffsetXFraction - 0.5f) * 2f * maxDragX
            val translY = -(cropOffsetYFraction - 0.5f) * 2f * maxDragY
            // graphicsLayer scale mirrors WallpaperCropOverlay: both axes use baseScaleX
            // so the aspect-correct image renders identically to the crop preview.
            val layerScale = effectiveScale / baseScaleX
            Image(
                bitmap = bmp,
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = wallpaperAlpha
                        scaleX = layerScale
                        scaleY = layerScale
                        translationX = translX
                        translationY = translY
                        // Merge blur into the same layer to avoid stacked-layer rendering
                        // issues on OEM GPU drivers. renderEffect is silently ignored on
                        // API < 31 (no blur on those devices, but alpha/scale still work).
                        val blurPx = contentBlurRadius.toPx()
                        renderEffect = if (blurPx > 0f) {
                            BlurEffect(blurPx, blurPx, TileMode.Decal)
                        } else null
                    }
            )
        }

        // ── HOME PAGE LAYER ───────────────────────────────────────────────────────
        // Custom text widget + clock live here. The entire layer fades to alpha=0
        // when the app-selecting page opens, so homescreen widgets can never overlap
        // or bleed through the app grid UI.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = homepageAlpha }
        ) {
            // Custom Text Widget — with bounds clipping to prevent overflow
            if (!isCustomTextMoveMode && customTextVisible && customText.isNotBlank()) {
                Text(
                    text = customText,
                    fontFamily = customFontFamily ?: FontFamily.Default,
                    fontSize = customFontSizeSp.sp,
                    letterSpacing = customLetterSpacing.em,
                    textAlign = when (customAlignment) {
                        "left"  -> TextAlign.Start
                        "right" -> TextAlign.End
                        else    -> TextAlign.Center
                    },
                    color = Color(customTextColorArgb),
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    maxLines = 5,  // Prevent unbounded growth
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (customOffsetXFraction * screenWidthPx).roundToInt(),
                                (customOffsetYFraction * screenHeightPx).roundToInt()
                            )
                        }
                        .width(with(density) { (customWidthFraction * screenWidthPx).toDp() })
                        .graphicsLayer { alpha = customTextOpacityPct / 100f }
                )
            }

            // AOD-style clock — the outer layer handles fading when app grid opens
            AodClock(
                isAppsVisible = false,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onGloballyPositioned { aodClockTopPx = it.boundsInRoot().top },
                showBattery = showBatteryWidget,
                showWifi = showWifiWidget,
                showCellular = showCellularWidget,
                showSpeed = showSpeedWidget,
                itemSpacingDp = spacingWidget.toInt(),
                clockStatusSpacingDp = clockStatusSpacingWidget.toInt(),
                timeFontSizeSp = timeFontSizeWidget.toInt(),
                iconSizeDp = iconSizeWidget.toInt(),
                itemOrder = statusItemOrder,
                showTime = showTimeWidget,
                timeFormat = timeFormatWidget,
                showDate = showDateWidget,
                dateFormat = dateFormatWidget,
                twoLines = twoLinesWidget,
                alignment = alignmentWidget,
                marginStartDp = marginStartWidget.toInt(),
                marginEndDp = marginEndWidget.toInt(),
                textColorArgb = timeWidgetColorArgb,
                opacityPct = timeWidgetOpacityPct.toInt(),
            )
        }

        // ── APP SELECTING PAGE LAYER ──────────────────────────────────────────────
        // App grid, name header, settings button. Drawn on top of the home layer
        // so nothing from the homescreen can ever bleed through.

        // ── SEARCH BAR ──────────────────────────────────────────────────────────
        // Always in the tree (no AnimatedVisibility) so onGloballyPositioned fires
        // even before the grid opens.  graphicsLayer drives all visibility:
        //   • homescreen → alpha=0 (invisible but tracking position)
        //   • grid open  → spotlight-fades as held finger approaches the top
        //   • search mode→ alpha=1 (fully opaque, keyboard up)
        // Drag your finger to the search pill and release to enter search mode.
        AppSearchBar(
            searchQuery = searchQuery,
            onQueryChange = { searchQuery = it },
            isSearchMode = isSearchMode,
            focusRequester = searchFocusRequester,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 20.dp, start = 16.dp, end = 56.dp)
                .onGloballyPositioned { coords ->
                    val bounds = coords.boundsInRoot()
                    searchBarPos = bounds.center
                    searchBarBoundsSize = Offset(bounds.width, bounds.height)
                    searchBarRect = bounds
                }
                .graphicsLayer {
                    when {
                        isSearchMode -> alpha = 1f
                        !isAppsVisible || isLightUpMode -> alpha = 0f
                        else -> {
                            val fingerPos = fingerPosition.value
                            val dx = searchBarPos.x - fingerPos.x
                            val dy = searchBarPos.y - fingerPos.y
                            val distSq = dx * dx + dy * dy
                            val spotlightRadius = hexRadiusPx * 5f
                            val fraction = if (distSq > spotlightRadius * spotlightRadius) 0f else {
                                val fade = 1f - (kotlin.math.sqrt(distSq) / spotlightRadius)
                                fade * fade * (3f - 2f * fade)
                            }
                            alpha = fraction
                        }
                    }
                },
        )

        // ── SEARCH RESULTS GRID ────────────────────────────────────────────────────────
        // Appears below the search bar when search mode is active.
        // All hex-grid AppItems are hidden (alpha=0) so only this grid is visible.
        // Each result item handles its own tap → launch (clickable inside SearchResultsGrid).
        SearchResultsGrid(
            filteredApps = remember(apps, searchQuery) {
                if (searchQuery.isBlank()) emptyList()
                else apps.filter {
                    it.name.contains(searchQuery, ignoreCase = true) ||
                        it.packageName.contains(searchQuery, ignoreCase = true)
                }
            },
            isVisible = isSearchMode,
            hexRadiusPx = hexRadiusPx,
            onAppClick = { app ->
                app.launchIntent?.let { context.startActivity(it) }
                isAppsVisible = false
                isSearchMode = false
                searchQuery = ""
            },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 68.dp, start = 12.dp, end = 12.dp)
                .fillMaxWidth(),
        )

        // App Grid - Honeycomb
        if (appPositions.isNotEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                apps.forEachIndexed { index, app ->
                    key(app.packageName) {
                        AppItem(
                            app = app,
                            index = index,
                            basePos = appPositions[index],
                            fingerPosition = fingerPosition,
                            isAppsVisible = isAppsVisible,
                            selectedIndex = selectedIndex,
                            isLightUpMode = isLightUpMode,
                            hexRadiusPx = hexRadiusPx,
                            isLowEndDevice = isLowEndDevice,
                            badgeCount = 0,  // TODO: wire to NotificationListenerService
                            isSearchMode = isSearchMode,
                        )
                    }
                }
            }
        }

        // Favourites Shelf (bottom of homescreen — slides up when favourites exist)
        FavoritesShelf(
            visible = !isAppsVisible && favoriteApps.isNotEmpty(),
            favorites = favoriteApps,
            onFavoriteClick = { app ->
                context.startActivity(app.launchIntent)
            },
            onRemoveFavorite = { app ->
                AppFavoritesPrefs.removeFavorite(context, app.packageName)
                favoritePackages = AppFavoritesPrefs.load(context).favorites
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
        )

        // ── FAVOURITES PIN BUTTON (bottom-centre) ────────────────────────────────
        // Mirrors the ⚙ settings button at the top: invisible until finger is nearby,
        // then fades + pops. Releasing the dragged app here pins/unpins it.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 20.dp)
                .onGloballyPositioned { coords ->
                    val bounds = coords.boundsInRoot()
                    favButtonPos = bounds.center
                    favButtonBoundsSize = Offset(bounds.width, bounds.height)
                }
                .graphicsLayer {
                    if (!isAppsVisible || isLightUpMode) { alpha = 0f; return@graphicsLayer }
                    val fingerPos = fingerPosition.value
                    val dx = favButtonPos.x - fingerPos.x
                    val dy = favButtonPos.y - fingerPos.y
                    val distSq = dx * dx + dy * dy
                    val spotlightRadius = hexRadiusPx * 4f
                    val fraction = if (distSq > spotlightRadius * spotlightRadius) 0f else {
                        val fade = 1f - (kotlin.math.sqrt(distSq) / spotlightRadius)
                        fade * fade * (3f - 2f * fade)
                    }
                    alpha = fraction
                    val hitRadius = maxOf(favButtonBoundsSize.x, favButtonBoundsSize.y) * 1.5f
                    val popFraction = if (distSq > hitRadius * hitRadius) 0f else {
                        val t = 1f - (kotlin.math.sqrt(distSq) / hitRadius)
                        t * t * (3f - 2f * t)
                    }
                    scaleX = 1f + 0.4f * popFraction
                    scaleY = 1f + 0.4f * popFraction
                },
        ) {
            Text(
                text = if (selectedAppIsFavorite) "★" else "☆",
                color = Color(0x99FFFFFF),
                fontSize = 28.sp,
            )
        }

        // Top App Name Display
        AppNameHeader(
            apps = apps,
            selectedIndex = selectedIndex,
            isVisible = isAppsVisible && !isLightUpMode,
            labelTheme = appLabelTheme,
        )

        // Settings Button
        val pulseTransition = rememberInfiniteTransition(label = "settingsPulse")
        val pulseScale by pulseTransition.animateFloat(
            initialValue = 1f,
            targetValue = if (!hasOpenedSettings) 1.25f else 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(800),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "pulseScale",
        )
        IconButton(
            onClick = {
                isSettingsVisible = true
                if (!hasOpenedSettings) {
                    hasOpenedSettings = true
                    AppGridPrefs.save(context, AppGridPrefs.Settings(
                        labelTheme = appLabelTheme,
                        hasOpenedSettings = true,
                    ))
                }
            },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 20.dp, end = 16.dp)
                .onGloballyPositioned { coordinates ->
                    val bounds = coordinates.boundsInRoot()
                    settingsPos = bounds.center
                    settingsBoundsSize = Offset(bounds.width, bounds.height)
                }
                .graphicsLayer {
                    if (!isAppsVisible && !isLightUpMode) {
                        alpha = 0f
                        return@graphicsLayer
                    }
                    if (isLightUpMode) {
                        alpha = 1f
                        return@graphicsLayer
                    }
                    val fingerPos = fingerPosition.value
                    val dx = settingsPos.x - fingerPos.x
                    val dy = settingsPos.y - fingerPos.y
                    val distSq = dx * dx + dy * dy
                    val spotlightRadius = hexRadiusPx * 4f
                    val spotlightRadiusSq = spotlightRadius * spotlightRadius
                    val fraction = if (distSq > spotlightRadiusSq) 0f else {
                        val fade = 1f - (kotlin.math.sqrt(distSq) / spotlightRadius)
                        fade * fade * (3f - 2f * fade)
                    }
                    alpha = fraction
                    // Pop scale as finger approaches
                    val hitRadius = maxOf(settingsBoundsSize.x, settingsBoundsSize.y) * 1.5f
                    val popFraction = if (distSq > hitRadius * hitRadius) 0f else {
                        val t = 1f - (kotlin.math.sqrt(distSq) / hitRadius)
                        t * t * (3f - 2f * t)
                    }
                    val baseScale = if (!hasOpenedSettings) pulseScale else 1f
                    scaleX = baseScale + 0.4f * popFraction
                    scaleY = baseScale + 0.4f * popFraction
                }
        ) {
            Text(
                text = "⚙",
                color = Color(0x99FFFFFF),
                fontSize = 28.sp
            )
        }

        LauncherSettingsOverlay(
            visible = isSettingsVisible,
            isLowEndDevice = isLowEndDevice,
            appLabelTheme = appLabelTheme,
            onLowEndDeviceChange = { isLowEndDevice = it },
            onAppLabelThemeChange = {
                appLabelTheme = it
                AppGridPrefs.save(context, AppGridPrefs.Settings(labelTheme = it))
            },
            onTimeWidget = { isTimeWidgetSettingsVisible = true },
            onWallpaper = { isWallpaperSettingsVisible = true },
            onCustomText = { isCustomTextSettingsVisible = true },
            onAppDrawer = { isAppDrawerSettingsVisible = true },
            onDismiss = { isSettingsVisible = false },
        )

        // Time Widget Sub-screen Overlay
        TimeWidgetSettingsOverlay(
            visible = isTimeWidgetSettingsVisible,
            subScreen = timeWidgetSubScreen,
            showBattery = showBatteryWidget,
            showWifi = showWifiWidget,
            showCellular = showCellularWidget,
            showSpeed = showSpeedWidget,
            spacingDp = spacingWidget,
            clockStatusSpacingDp = clockStatusSpacingWidget,
            timeFontSizeSp = timeFontSizeWidget,
            iconSizeDp = iconSizeWidget,
            itemOrder = statusItemOrder,
            showTime = showTimeWidget,
            timeFormat = timeFormatWidget,
            showDate = showDateWidget,
            dateFormat = dateFormatWidget,
            twoLines = twoLinesWidget,
            alignment = alignmentWidget,
            marginStartDp = marginStartWidget,
            marginEndDp = marginEndWidget,
            textColorArgb = timeWidgetColorArgb,
            opacityPct = timeWidgetOpacityPct,
            onSubScreenChange = { timeWidgetSubScreen = it },
            onShowBatteryChange = { showBatteryWidget = it },
            onShowWifiChange = { showWifiWidget = it },
            onShowCellularChange = { showCellularWidget = it },
            onShowSpeedChange = { showSpeedWidget = it },
            onSpacingChange = { spacingWidget = it },
            onClockStatusSpacingChange = { clockStatusSpacingWidget = it },
            onTimeFontSizeChange = { timeFontSizeWidget = it },
            onIconSizeChange = { iconSizeWidget = it },
            onItemOrderChange = { statusItemOrder = it },
            onShowTimeChange = { showTimeWidget = it },
            onTimeFormatChange = { timeFormatWidget = it },
            onShowDateChange = { showDateWidget = it },
            onDateFormatChange = { dateFormatWidget = it },
            onTwoLinesChange = { twoLinesWidget = it },
            onAlignmentChange = { alignmentWidget = it },
            onMarginStartChange = { marginStartWidget = it },
            onMarginEndChange = { marginEndWidget = it },
            onTextColorChange = { timeWidgetColorArgb = it },
            onOpacityChange = { timeWidgetOpacityPct = it },
            onDismiss = { isTimeWidgetSettingsVisible = false },
            saveAll = {
                TimeWidgetPrefs.save(context, TimeWidgetPrefs.Settings(
                    showBattery = showBatteryWidget,
                    showWifi = showWifiWidget,
                    showCellular = showCellularWidget,
                    showSpeed = showSpeedWidget,
                    spacingDp = spacingWidget.toInt(),
                    clockStatusSpacingDp = clockStatusSpacingWidget.toInt(),
                    timeFontSizeSp = timeFontSizeWidget.toInt(),
                    iconSizeDp = iconSizeWidget.toInt(),
                    itemOrder = statusItemOrder,
                    showTime = showTimeWidget,
                    timeFormat = timeFormatWidget,
                    showDate = showDateWidget,
                    dateFormat = dateFormatWidget,
                    twoLines = twoLinesWidget,
                    alignment = alignmentWidget,
                    marginStartDp = marginStartWidget.toInt(),
                    marginEndDp = marginEndWidget.toInt(),
                    textColorArgb = timeWidgetColorArgb,
                    opacityPct = timeWidgetOpacityPct.toInt(),
                ))
            },
        )


        WallpaperSettingsOverlay(
            visible = isWallpaperSettingsVisible,
            wallpaperBitmap = wallpaperBitmap,
            hasWallpaper = wallpaperUriString != null,
            blurRadiusDp = wallpaperHomescreenBlurRadius,
            onBlurRadiusChange = { wallpaperHomescreenBlurRadius = it },
            opacityPct = wallpaperOpacityPct,
            onOpacityChange = { wallpaperOpacityPct = it },
            onPickWallpaper = { wallpaperPickerLauncher.launch("image/*") },
            onRemoveWallpaper = {
                WallpaperPrefs.save(context, WallpaperPrefs.Settings(
                    uri = null,
                    blurRadiusDp = wallpaperBlurRadius.toInt(),
                    homescreenBlurRadiusDp = wallpaperHomescreenBlurRadius.toInt(),
                    opacityPct = wallpaperOpacityPct.toInt(),
                    appDrawerOpacityPct = appDrawerOpacityPct.toInt(),
                    cropOffsetXFraction = cropOffsetXFraction,
                    cropOffsetYFraction = cropOffsetYFraction,
                    zoomScale = wallpaperZoomScale,
                ))
                wallpaperUriString = null
            },
            onDismiss = { isWallpaperSettingsVisible = false },
            saveWallpaper = {
                WallpaperPrefs.save(context, WallpaperPrefs.Settings(
                    uri = wallpaperUriString,
                    blurRadiusDp = wallpaperBlurRadius.toInt(),
                    homescreenBlurRadiusDp = wallpaperHomescreenBlurRadius.toInt(),
                    opacityPct = wallpaperOpacityPct.toInt(),
                    appDrawerOpacityPct = appDrawerOpacityPct.toInt(),
                    cropOffsetXFraction = cropOffsetXFraction,
                    cropOffsetYFraction = cropOffsetYFraction,
                    zoomScale = wallpaperZoomScale,
                ))
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
        )

        AppDrawerSettingsOverlay(
            visible = isAppDrawerSettingsVisible,
            wallpaperBitmap = wallpaperBitmap,
            blurRadiusDp = wallpaperBlurRadius,
            appDrawerOpacityPct = appDrawerOpacityPct,
            onBlurRadiusChange = { wallpaperBlurRadius = it },
            onOpacityChange = { appDrawerOpacityPct = it },
            onDismiss = { isAppDrawerSettingsVisible = false },
            onSave = {
                WallpaperPrefs.save(context, WallpaperPrefs.Settings(
                    uri = wallpaperUriString,
                    blurRadiusDp = wallpaperBlurRadius.toInt(),
                    homescreenBlurRadiusDp = wallpaperHomescreenBlurRadius.toInt(),
                    opacityPct = wallpaperOpacityPct.toInt(),
                    appDrawerOpacityPct = appDrawerOpacityPct.toInt(),
                    cropOffsetXFraction = cropOffsetXFraction,
                    cropOffsetYFraction = cropOffsetYFraction,
                    zoomScale = wallpaperZoomScale,
                ))
                hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            },
        )

        WallpaperCropOverlay(
            visible = isCropModeVisible,
            wallpaperBitmap = wallpaperBitmap,
            initialOffsetX = cropOffsetXFraction,
            initialOffsetY = cropOffsetYFraction,
            initialZoom = wallpaperZoomScale,
            onConfirm = { x, y, z ->
                cropOffsetXFraction = x
                cropOffsetYFraction = y
                wallpaperZoomScale = z
                WallpaperPrefs.save(context, WallpaperPrefs.Settings(
                    uri = wallpaperUriString,
                    blurRadiusDp = wallpaperBlurRadius.toInt(),
                    homescreenBlurRadiusDp = wallpaperHomescreenBlurRadius.toInt(),
                    opacityPct = wallpaperOpacityPct.toInt(),
                    appDrawerOpacityPct = appDrawerOpacityPct.toInt(),
                    cropOffsetXFraction = x,
                    cropOffsetYFraction = y,
                    zoomScale = z,
                ))
                isCropModeVisible = false
            },
            onDismiss = { isCropModeVisible = false },
            homescreenContent = {
                // AodClock — exactly as shown on the homescreen
                AodClock(
                    isAppsVisible = false,
                    modifier = Modifier.align(Alignment.BottomCenter),
                    showBattery = showBatteryWidget,
                    showWifi = showWifiWidget,
                    showCellular = showCellularWidget,
                    showSpeed = showSpeedWidget,
                    itemSpacingDp = spacingWidget.toInt(),
                    clockStatusSpacingDp = clockStatusSpacingWidget.toInt(),
                    timeFontSizeSp = timeFontSizeWidget.toInt(),
                    iconSizeDp = iconSizeWidget.toInt(),
                    itemOrder = statusItemOrder,
                    showTime = showTimeWidget,
                    timeFormat = timeFormatWidget,
                    showDate = showDateWidget,
                    dateFormat = dateFormatWidget,
                    twoLines = twoLinesWidget,
                    alignment = alignmentWidget,
                    marginStartDp = marginStartWidget.toInt(),
                    marginEndDp = marginEndWidget.toInt(),
                    textColorArgb = timeWidgetColorArgb,
                    opacityPct = timeWidgetOpacityPct.toInt(),
                )
                // Custom text widget — exactly as shown on the homescreen with bounds clipping
                if (customTextVisible && customText.isNotBlank()) {
                    Text(
                        text = customText,
                        fontFamily = customFontFamily ?: FontFamily.Default,
                        fontSize = customFontSizeSp.sp,
                        letterSpacing = customLetterSpacing.em,
                        textAlign = when (customAlignment) {
                            "left"  -> TextAlign.Start
                            "right" -> TextAlign.End
                            else    -> TextAlign.Center
                        },
                        color = Color(customTextColorArgb).copy(alpha = customTextOpacityPct / 100f),
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        maxLines = 5,  // Prevent unbounded growth
                        modifier = Modifier
                            .offset {
                                IntOffset(
                                    (customOffsetXFraction * screenWidthPx).roundToInt(),
                                    (customOffsetYFraction * screenHeightPx).roundToInt()
                                )
                            }
                            .width(with(density) { (customWidthFraction * screenWidthPx).toDp() })
                    )
                }
            },
        )

        // ── Custom Text Widget Settings Overlay ────────────────────────────────
        CustomTextSettingsOverlay(
            visible = isCustomTextSettingsVisible && !isCustomTextMoveMode,
            customText = customText,
            customFontSizeSp = customFontSizeSp,
            customLetterSpacing = customLetterSpacing,
            customAlignment = customAlignment,
            customFontFilename = customFontFilename,
            customFontFamily = customFontFamily,
            customTextVisible = customTextVisible,
            customTextColorArgb = customTextColorArgb,
            customOpacityPct = customTextOpacityPct,
            onTextChange = { customText = it },
            onFontSizeChange = { customFontSizeSp = it },
            onLetterSpacingChange = { customLetterSpacing = it },
            onAlignmentChange = { customAlignment = it },
            onVisibilityChange = { customTextVisible = it },
            onColorChange = { customTextColorArgb = it },
            onOpacityChange = { customTextOpacityPct = it },
            onUploadFont = { fontPickerLauncher.launch("*/*") },
            onResetFont = {
                customFontFilename = null
                customFontFamily = null
                CustomTextWidgetPrefs.save(context, CustomTextWidgetPrefs.Settings(
                    text = customText, fontFilename = null,
                    letterSpacing = customLetterSpacing, alignment = customAlignment,
                    offsetXFraction = customOffsetXFraction, offsetYFraction = customOffsetYFraction,
                    widthFraction = customWidthFraction, fontSizeSp = customFontSizeSp,
                    visible = customTextVisible,
                    textColorArgb = customTextColorArgb,
                    opacityPct = customTextOpacityPct.toInt(),
                ))
            },
            onMoveResize = {
                // Dismiss ALL settings overlays so the homescreen is fully visible,
                // then show the move mode after the fade-out completes.
                isCustomTextSettingsVisible = false
                isSettingsVisible = false
                coroutineScope.launch {
                    delay(220)
                    isCustomTextMoveMode = true
                }
            },
            onDismiss = { isCustomTextSettingsVisible = false },
            saveCustomText = {
                CustomTextWidgetPrefs.save(context, CustomTextWidgetPrefs.Settings(
                    text = customText, fontFilename = customFontFilename,
                    letterSpacing = customLetterSpacing, alignment = customAlignment,
                    offsetXFraction = customOffsetXFraction, offsetYFraction = customOffsetYFraction,
                    widthFraction = customWidthFraction, fontSizeSp = customFontSizeSp,
                    visible = customTextVisible, textColorArgb = customTextColorArgb,
                    opacityPct = customTextOpacityPct.toInt(),
                ))
            },
        )

        // ── Custom Text Move & Resize Mode ─────────────────────────────────────
        if (isCustomTextMoveMode) {
            CustomTextMoveMode(
                customText = customText,
                customFontSizeSp = customFontSizeSp,
                customLetterSpacing = customLetterSpacing,
                customAlignment = customAlignment,
                customFontFamily = customFontFamily,
                customTextVisible = customTextVisible,
                customFontFilename = customFontFilename,
                customTextColorArgb = customTextColorArgb,
                customTextOpacityPct = customTextOpacityPct,
                initialXFraction = customOffsetXFraction,
                initialYFraction = customOffsetYFraction,
                initialWidthFraction = customWidthFraction,
                aodClockTopPx = aodClockTopPx,
                onExit = { x, y, w ->
                    customOffsetXFraction = x
                    customOffsetYFraction = y
                    customWidthFraction = w
                    isCustomTextMoveMode = false
                },
                onHideAndExit = { x, y, w ->
                    customOffsetXFraction = x
                    customOffsetYFraction = y
                    customWidthFraction = w
                    customTextVisible = false
                    isCustomTextMoveMode = false
                },
            )
        }

        // ── Loading screen ─────────────────────────────────────────────────────
        // Rendered last (top Z-order) so it covers everything until ready.
        // Fades out once apps AND wallpaper bitmap are both available.
        // No touches pass through: the loading composable fills the screen.
        AnimatedVisibility(
            visible = !isReady,
            enter   = fadeIn(tween(0)),   // instant appear (screen starts black anyway)
            exit    = fadeOut(tween(400)),
        ) {
            LoadingScreen()
        }
    }
}
