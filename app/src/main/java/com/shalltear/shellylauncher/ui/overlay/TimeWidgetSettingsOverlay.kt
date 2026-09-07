package com.shalltear.shellylauncher.ui.overlay

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material.icons.rounded.DragHandle
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shellylauncher.ui.components.SettingsNavRow
import com.shalltear.shellylauncher.ui.components.SettingsOverlayScaffold
import com.shalltear.shellylauncher.ui.components.SettingsSliderRow
import com.shalltear.shellylauncher.ui.components.ColorPickerRow
import com.shalltear.shellylauncher.ui.composable.AodClock
import com.shalltear.shellylauncher.utils.TimeWidgetPrefs
import sh.calvin.reorderable.ReorderableColumn
import sh.calvin.reorderable.ReorderableItem

@Composable
fun TimeWidgetSettingsOverlay(
    visible: Boolean,
    subScreen: String?,
    showBattery: Boolean,
    showWifi: Boolean,
    showCellular: Boolean,
    showSpeed: Boolean,
    spacingDp: Float,
    clockStatusSpacingDp: Float,
    timeFontSizeSp: Float,
    iconSizeDp: Float,
    itemOrder: List<String>,
    showTime: Boolean,
    timeFormat: String,
    showDate: Boolean,
    dateFormat: String,
    twoLines: Boolean,
    alignment: String,
    marginStartDp: Float,
    marginEndDp: Float,
    textColorArgb: Int,
    opacityPct: Float,
    onSubScreenChange: (String?) -> Unit,
    onShowBatteryChange: (Boolean) -> Unit,
    onShowWifiChange: (Boolean) -> Unit,
    onShowCellularChange: (Boolean) -> Unit,
    onShowSpeedChange: (Boolean) -> Unit,
    onSpacingChange: (Float) -> Unit,
    onClockStatusSpacingChange: (Float) -> Unit,
    onTimeFontSizeChange: (Float) -> Unit,
    onIconSizeChange: (Float) -> Unit,
    onItemOrderChange: (List<String>) -> Unit,
    onShowTimeChange: (Boolean) -> Unit,
    onTimeFormatChange: (String) -> Unit,
    onShowDateChange: (Boolean) -> Unit,
    onDateFormatChange: (String) -> Unit,
    onTwoLinesChange: (Boolean) -> Unit,
    onAlignmentChange: (String) -> Unit,
    onMarginStartChange: (Float) -> Unit,
    onMarginEndChange: (Float) -> Unit,
    onTextColorChange: (Int) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onDismiss: () -> Unit,
    saveAll: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow))
    ) {
        SettingsOverlayScaffold(
            title = when (subScreen) {
                "time_date"    -> "Time & Date"
                "status_icons" -> "Status Icons"
                "spacings"     -> "Spacings"
                else           -> "Time Widget"
            },
            onBack = {
                if (subScreen != null) onSubScreenChange(null) else onDismiss()
            },
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            // Live preview card — always visible
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(listOf(Color(0xFF141420), Color(0xFF0C0C16))),
                        RoundedCornerShape(16.dp)
                    )
                    .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                    .padding(vertical = 24.dp),
                contentAlignment = Alignment.Center
            ) {
                AodClock(
                    isAppsVisible = false,
                    showBattery = showBattery,
                    showWifi = showWifi,
                    showCellular = showCellular,
                    showSpeed = showSpeed,
                    itemSpacingDp = spacingDp.toInt(),
                    clockStatusSpacingDp = clockStatusSpacingDp.toInt(),
                    timeFontSizeSp = timeFontSizeSp.toInt(),
                    iconSizeDp = iconSizeDp.toInt(),
                    itemOrder = itemOrder,
                    showTime = showTime,
                    timeFormat = timeFormat,
                    showDate = showDate,
                    dateFormat = dateFormat,
                    twoLines = twoLines,
                    alignment = alignment,
                    marginStartDp = marginStartDp.toInt(),
                    marginEndDp = marginEndDp.toInt(),
                    textColorArgb = textColorArgb,
                    opacityPct = opacityPct.toInt(),
                    previewMode = true,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (subScreen) {

                // ── ROOT — 3 nav rows ─────────────────────────────────
                null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        listOf(
                            "time_date"    to "Time & Date",
                            "status_icons" to "Status Icons",
                            "spacings"     to "Spacings",
                        ).forEach { (key, label) ->
                            SettingsNavRow(label = label, onClick = { onSubScreenChange(key) })
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }

                // ── TIME & DATE ───────────────────────────────────────
                "time_date" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Show Time
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(listOf(Color(0xFF181825), Color(0xFF111119))),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(14.dp))
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Show Time", color = Color.White, fontSize = 16.sp)
                                Switch(checked = showTime, onCheckedChange = { onShowTimeChange(it); saveAll() })
                            }
                            if (showTime) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Format", color = Color(0x99FFFFFF), fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    listOf("HH:mm" to "24h", "hh:mm a" to "12h").forEach { (fmt, lbl) ->
                                        val selected = timeFormat == fmt
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .background(
                                                    if (selected)
                                                        Brush.horizontalGradient(listOf(Color(0xFF5A4FD4), Color(0xFF7B6FEF)))
                                                    else
                                                        Brush.horizontalGradient(listOf(Color(0xFF1C1C2A), Color(0xFF141420))),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    if (selected) Color(0x507B6FEF) else Color(0x18FFFFFF),
                                                    RoundedCornerShape(10.dp)
                                                )
                                                .clickable { onTimeFormatChange(fmt); saveAll() }
                                                .padding(horizontal = 20.dp, vertical = 9.dp)
                                        ) { Text(lbl, color = if (selected) Color.White else Color(0x88FFFFFF), fontSize = 14.sp) }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Show Date
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(listOf(Color(0xFF181825), Color(0xFF111119))),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(14.dp))
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Show Date", color = Color.White, fontSize = 16.sp)
                                Switch(checked = showDate, onCheckedChange = { onShowDateChange(it); saveAll() })
                            }
                            if (showDate) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("Format", color = Color(0x99FFFFFF), fontSize = 13.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                val dateFormats = listOf(
                                    "EEE d MMM"  to "Wed 6 May",
                                    "d MMM yyyy" to "6 May 2026",
                                    "dd/MM/yyyy" to "06/05/2026",
                                    "MM/dd/yyyy" to "05/06/2026",
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    dateFormats.chunked(2).forEach { pair ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            pair.forEach { (fmt, lbl) ->
                                                val selected = dateFormat == fmt
                                                Box(
                                                    contentAlignment = Alignment.Center,
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .background(
                                                            if (selected)
                                                                Brush.horizontalGradient(listOf(Color(0xFF5A4FD4), Color(0xFF7B6FEF)))
                                                            else
                                                                Brush.horizontalGradient(listOf(Color(0xFF1C1C2A), Color(0xFF141420))),
                                                            RoundedCornerShape(10.dp)
                                                        )
                                                        .border(
                                                            1.dp,
                                                            if (selected) Color(0x507B6FEF) else Color(0x18FFFFFF),
                                                            RoundedCornerShape(10.dp)
                                                        )
                                                        .clickable { onDateFormatChange(fmt); saveAll() }
                                                        .padding(horizontal = 12.dp, vertical = 9.dp)
                                                ) {
                                                    Text(lbl, color = Color.White, fontSize = 13.sp, textAlign = TextAlign.Center)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Two Lines
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(listOf(Color(0xFF181825), Color(0xFF111119))),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(14.dp))
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Column {
                                Text("Two Lines", color = Color.White, fontSize = 16.sp)
                                Text("Show time & date on separate lines", color = Color(0x66FFFFFF), fontSize = 12.sp)
                            }
                            Switch(checked = twoLines, onCheckedChange = { onTwoLinesChange(it); saveAll() })
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Alignment
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(listOf(Color(0xFF181825), Color(0xFF111119))),
                                    RoundedCornerShape(14.dp)
                                )
                                .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(14.dp))
                                .padding(horizontal = 20.dp, vertical = 16.dp)
                        ) {
                            Text("Alignment", color = Color(0x99FFFFFF), fontSize = 12.sp, letterSpacing = 0.5.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf(
                                    Triple("left", Icons.Default.FormatAlignLeft, "Left"),
                                    Triple("center", Icons.Default.FormatAlignCenter, "Center"),
                                    Triple("right", Icons.Default.FormatAlignRight, "Right"),
                                ).forEach { (value, icon, label) ->
                                    val selected = alignment == value
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (selected)
                                                    Brush.horizontalGradient(listOf(Color(0xFF5A4FD4), Color(0xFF7B6FEF)))
                                                else
                                                    Brush.horizontalGradient(listOf(Color(0xFF1C1C2A), Color(0xFF141420))),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) Color(0x507B6FEF) else Color(0x18FFFFFF),
                                                RoundedCornerShape(10.dp)
                                            )
                                            .clickable { onAlignmentChange(value); saveAll() }
                                            .padding(vertical = 12.dp)
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = label,
                                            tint = if (selected) Color.White else Color(0x88FFFFFF),
                                            modifier = Modifier.size(20.dp),
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Color picker
                        ColorPickerRow(
                            label = "Text Color",
                            colorArgb = textColorArgb,
                            onColorChange = { onTextColorChange(it); saveAll() },
                        )

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }

                // ── STATUS ICONS ──────────────────────────────────────
                "status_icons" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clipToBounds()
                        ) {
                            ReorderableColumn(
                                list = itemOrder,
                                onSettle = { from, to ->
                                    onItemOrderChange(itemOrder.toMutableList().apply { add(to, removeAt(from)) })
                                    saveAll()
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) { index, itemKey, _ ->
                                key(itemKey) {
                                    val label = when (itemKey) {
                                        "battery"  -> "Battery"
                                        "wifi"     -> "WiFi"
                                        "cellular" -> "Cellular"
                                        "speed"    -> "Network Speed"
                                        else       -> itemKey
                                    }
                                    val checked = when (itemKey) {
                                        "battery"  -> showBattery
                                        "wifi"     -> showWifi
                                        "cellular" -> showCellular
                                        "speed"    -> showSpeed
                                        else       -> false
                                    }
                                    ReorderableItem {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(bottom = if (index < itemOrder.lastIndex) 8.dp else 0.dp)
                                            .background(
                                                Brush.verticalGradient(listOf(Color(0xFF181825), Color(0xFF111119))),
                                                RoundedCornerShape(14.dp)
                                            )
                                            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(14.dp))
                                            .padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                        ) {
                                            Text(label, color = Color.White, fontSize = 16.sp)
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Switch(
                                                    checked = checked,
                                                    onCheckedChange = { newVal ->
                                                        when (itemKey) {
                                                            "battery"  -> onShowBatteryChange(newVal)
                                                            "wifi"     -> onShowWifiChange(newVal)
                                                            "cellular" -> onShowCellularChange(newVal)
                                                            "speed"    -> onShowSpeedChange(newVal)
                                                        }
                                                        saveAll()
                                                    },
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.Rounded.DragHandle,
                                                    contentDescription = "Drag to reorder",
                                                    tint = Color(0x66FFFFFF),
                                                    modifier = Modifier
                                                        .size(24.dp)
                                                        .draggableHandle(),
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── SPACINGS ──────────────────────────────────────────
                "spacings" -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    ) {
                        SettingsSliderRow(
                            label = "Icon Spacing",
                            valueLabel = "${spacingDp.toInt()}dp",
                            value = spacingDp,
                            onValueChange = { onSpacingChange(it); saveAll() },
                            valueRange = 0f..24f,
                            steps = 23,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsSliderRow(
                            label = "Time–Status Spacing",
                            valueLabel = "${clockStatusSpacingDp.toInt()}dp",
                            value = clockStatusSpacingDp,
                            onValueChange = { onClockStatusSpacingChange(it); saveAll() },
                            valueRange = 0f..24f,
                            steps = 23,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsSliderRow(
                            label = "Time Font Size",
                            valueLabel = "${timeFontSizeSp.toInt()}sp",
                            value = timeFontSizeSp,
                            onValueChange = { onTimeFontSizeChange(it); saveAll() },
                            valueRange = 10f..32f,
                            steps = 21,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsSliderRow(
                            label = "Status Icon Size",
                            valueLabel = "${iconSizeDp.toInt()}dp",
                            value = iconSizeDp,
                            onValueChange = { onIconSizeChange(it); saveAll() },
                            valueRange = 8f..24f,
                            steps = 15,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsSliderRow(
                            label = "Left Margin",
                            valueLabel = "${marginStartDp.toInt()}dp",
                            value = marginStartDp,
                            onValueChange = { onMarginStartChange(it); saveAll() },
                            valueRange = 0f..120f,
                            steps = 119,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsSliderRow(
                            label = "Right Margin",
                            valueLabel = "${marginEndDp.toInt()}dp",
                            value = marginEndDp,
                            onValueChange = { onMarginEndChange(it); saveAll() },
                            valueRange = 0f..120f,
                            steps = 119,
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        SettingsSliderRow(
                            label = "Opacity",
                            valueLabel = "${opacityPct.toInt()}%",
                            value = opacityPct,
                            onValueChange = { onOpacityChange(it); saveAll() },
                            valueRange = 0f..100f,
                            steps = 99,
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
