package com.shalltear.shellylauncher.ui.composable

import android.content.Context
import android.content.BroadcastReceiver
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Network
import android.net.TrafficStats
import android.os.BatteryManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NetworkCell
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shellylauncher.utils.WeatherWidget
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val iconTint = Color(0xAAFFFFFF)

@Composable
fun AodClock(
    isAppsVisible: Boolean,
    modifier: Modifier = Modifier,
    showBattery: Boolean = true,
    showWifi: Boolean = true,
    showCellular: Boolean = true,
    showSpeed: Boolean = true,
    itemSpacingDp: Int = 6,
    clockStatusSpacingDp: Int = 4,
    timeFontSizeSp: Int = 16,
    iconSizeDp: Int = 13,
    itemOrder: List<String> = listOf("battery", "wifi", "cellular", "speed"),
    previewMode: Boolean = false,
    showTime: Boolean = true,
    timeFormat: String = "HH:mm",
    showDate: Boolean = true,
    dateFormat: String = "EEE d MMM",
    twoLines: Boolean = false,
    alignment: String = "center",
    marginStartDp: Int = 0,
    marginEndDp: Int = 0,
    textColorArgb: Int = 0xFFFFFFFF.toInt(),
    opacityPct: Int = 100,
    showWeather: Boolean = false,  // Optional weather widget
) {
    val context = LocalContext.current
    var timeLabel by remember { mutableStateOf("") }
    var dateLabel by remember { mutableStateOf("") }
    var battLabel by remember { mutableStateOf("") }
    var hasWifi by remember { mutableStateOf(false) }
    var hasCell by remember { mutableStateOf(false) }
    var speedLabel by remember { mutableStateOf("") }

    val timeFmt = remember(timeFormat) { SimpleDateFormat(timeFormat, Locale.getDefault()) }
    val dateFmt = remember(dateFormat) { SimpleDateFormat(dateFormat, Locale.getDefault()) }
    var prevRx by remember { mutableLongStateOf(TrafficStats.getTotalRxBytes().coerceAtLeast(0L)) }
    var prevTx by remember { mutableLongStateOf(TrafficStats.getTotalTxBytes().coerceAtLeast(0L)) }

    fun updateConnectivity(cm: ConnectivityManager) {
        var wifiFound = false
        var cellFound = false
        for (net in cm.allNetworks) {
            val caps = cm.getNetworkCapabilities(net) ?: continue
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) wifiFound = true
            if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) cellFound = true
        }
        hasWifi = wifiFound
        hasCell = cellFound
    }

    LaunchedEffect(timeFormat, dateFormat) {
        while (isActive) {
            timeLabel = timeFmt.format(Date())
            dateLabel = dateFmt.format(Date())
            val tickMs = if (timeFormat.contains('s') || dateFormat.contains('s')) 1_000L else 60_000L
            delay(tickMs)
        }
    }

    // Battery updates are event-driven, avoiding repeated sticky-intent polling every 2s.
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
                val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 0
                val battStatus = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
                val isCharging = battStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                    battStatus == BatteryManager.BATTERY_STATUS_FULL
                battLabel = "$pct%${if (isCharging) "⚡" else ""}"
            }
        }
        val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
        context.registerReceiver(receiver, filter)
        context.registerReceiver(null, filter)?.let { receiver.onReceive(context, it) }
        onDispose { runCatching { context.unregisterReceiver(receiver) } }
    }

    // Connectivity updates only when network changes, not on a periodic polling loop.
    DisposableEffect(showWifi, showCellular) {
        if (!showWifi && !showCellular) {
            hasWifi = false
            hasCell = false
            onDispose { }
        } else {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) = updateConnectivity(cm)
                override fun onLost(network: Network) = updateConnectivity(cm)
                override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                    updateConnectivity(cm)
                }
            }
            updateConnectivity(cm)
            cm.registerDefaultNetworkCallback(callback)
            onDispose { runCatching { cm.unregisterNetworkCallback(callback) } }
        }
    }

    // Keep speed updates isolated and off when disabled to save battery.
    LaunchedEffect(showSpeed) {
        if (!showSpeed) {
            speedLabel = ""
            return@LaunchedEffect
        }
        while (isActive) {
            val curRx = TrafficStats.getTotalRxBytes().coerceAtLeast(0L)
            val curTx = TrafficStats.getTotalTxBytes().coerceAtLeast(0L)
            val rxBps = ((curRx - prevRx) / 2L).coerceAtLeast(0L)
            val txBps = ((curTx - prevTx) / 2L).coerceAtLeast(0L)
            prevRx = curRx
            prevTx = curTx
            speedLabel = "↓${fmtSpeed(rxBps)} ↑${fmtSpeed(txBps)}"
            delay(2_000L)
        }
    }

    val alpha by animateFloatAsState(
        targetValue = if (isAppsVisible && !previewMode) 0f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "clockAlpha"
    )

    val colHAlignment = when (alignment) {
        "left"  -> Alignment.Start
        "right" -> Alignment.End
        else    -> Alignment.CenterHorizontally
    }
    val textHAlignment = when (alignment) {
        "left"  -> TextAlign.Start
        "right" -> TextAlign.End
        else    -> TextAlign.Center
    }

    val textColor  = Color(textColorArgb)
    val dimColor   = textColor.copy(alpha = 0.67f)

    Column(
        horizontalAlignment = colHAlignment,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = marginStartDp.dp, end = marginEndDp.dp, bottom = if (previewMode) 0.dp else 48.dp)
            .graphicsLayer { this.alpha = alpha * (opacityPct / 100f) }
    ) {
        // Time & Date
        if (twoLines) {
            if (showTime) {
                Text(
                    text = timeLabel,
                    color = textColor,
                    fontSize = timeFontSizeSp.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = textHAlignment,
                )
            }
            if (showDate) {
                Text(
                    text = dateLabel,
                    color = dimColor,
                    fontSize = (timeFontSizeSp * 0.65f).sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = textHAlignment,
                )
            }
        } else {
            val parts = buildList {
                if (showTime) add(timeLabel)
                if (showDate) add(dateLabel)
            }
            if (parts.isNotEmpty()) {
                Text(
                    text = parts.joinToString("  "),
                    color = textColor,
                    fontSize = timeFontSizeSp.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = textHAlignment,
                )
            }
        }

        Spacer(modifier = Modifier.height(clockStatusSpacingDp.dp))

        // Optional weather widget
        if (showWeather) {
            val weather = remember { WeatherWidget.getCurrentWeather() }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = "${WeatherWidget.getWeatherEmoji(weather.condition)} ${weather.temperature}°",
                    color = dimColor,
                    fontSize = (timeFontSizeSp * 0.7f).sp,
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
            Spacer(modifier = Modifier.height(clockStatusSpacingDp.dp))
        }

        // Status row: items rendered in user-defined order
        val visibleItems = itemOrder.filter { key ->
            when (key) {
                "wifi"     -> showWifi && hasWifi
                "cellular" -> showCellular && hasCell
                "speed"    -> showSpeed && speedLabel.isNotEmpty()
                "battery"  -> showBattery && battLabel.isNotEmpty()
                else       -> false
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = when (alignment) {
                "left"  -> Arrangement.Start
                "right" -> Arrangement.End
                else    -> Arrangement.Center
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            visibleItems.forEachIndexed { i, key ->
                when (key) {
                    "wifi" -> Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = "WiFi",
                        tint = dimColor,
                        modifier = Modifier.size(iconSizeDp.dp),
                    )
                    "cellular" -> Icon(
                        imageVector = Icons.Filled.NetworkCell,
                        contentDescription = "Cellular",
                        tint = dimColor,
                        modifier = Modifier.size(iconSizeDp.dp),
                    )
                    "speed" -> Text(
                        text = speedLabel,
                        color = dimColor,
                        fontSize = iconSizeDp.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    "battery" -> Text(
                        text = battLabel,
                        color = dimColor,
                        fontSize = iconSizeDp.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                if (i < visibleItems.lastIndex) {
                    Spacer(modifier = Modifier.width(itemSpacingDp.dp))
                }
            }
        }
    }
}

private fun fmtSpeed(bps: Long): String = when {
    bps >= 1_000_000L -> "${"%.1f".format(bps / 1_000_000.0)}M"
    bps >= 1_000L     -> "${"%.0f".format(bps / 1_000.0)}K"
    else              -> "${bps}B"
}
