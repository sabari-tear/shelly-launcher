package com.shalltear.shellylauncher.ui.overlay

import android.content.Intent
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessAlarm
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.Wallpaper
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shellylauncher.ui.components.SettingsOverlayScaffold

private data class NavTile(
    val icon: ImageVector,
    val label: String,
    val accent: Color,
    val onClick: () -> Unit,
)

private val LsCardGradient = Brush.verticalGradient(listOf(Color(0xFF181825), Color(0xFF111119)))
private val LsCardBorder = Color(0x18FFFFFF)
private val LsAccent = Color(0xFF7B6FEF)

/**
 * Full-screen Launcher Settings overlay.
 *
 * Caller is responsible for wrapping with [AnimatedVisibility] if desired, or
 * this composable handles its own visibility via [visible].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LauncherSettingsOverlay(
    visible: Boolean,
    isLowEndDevice: Boolean,
    appLabelTheme: String,
    onLowEndDeviceChange: (Boolean) -> Unit,
    onAppLabelThemeChange: (String) -> Unit,
    onTimeWidget: () -> Unit,
    onWallpaper: () -> Unit,
    onCustomText: () -> Unit,
    onAppDrawer: () -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)),
    ) {
        SettingsOverlayScaffold(
            title = "Settings",
            onBack = onDismiss,
            backIcon = Icons.Default.Close,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Set as Default Launcher — gradient accent button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF5A4FD4), LsAccent))
                        )
                        .clickable {
                            context.startActivity(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Text("Set as Default Launcher", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    }
                }

                // Performance Mode
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LsCardGradient, RoundedCornerShape(14.dp))
                        .border(1.dp, LsCardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text("Performance Mode", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        Text("Reduce GPU load on older devices", color = Color(0x66FFFFFF), fontSize = 12.sp)
                    }
                    Switch(checked = isLowEndDevice, onCheckedChange = onLowEndDeviceChange)
                }

                // App Label Theme picker
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(LsCardGradient, RoundedCornerShape(14.dp))
                        .border(1.dp, LsCardBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp)
                ) {
                    Text("App Label Theme", color = Color(0x99FFFFFF), fontSize = 12.sp, letterSpacing = 0.5.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        listOf("auto" to "Auto", "light" to "Light", "dark" to "Dark").forEach { (value, label) ->
                            val selected = appLabelTheme == value
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (selected)
                                            Brush.horizontalGradient(listOf(Color(0xFF5A4FD4), LsAccent))
                                        else
                                            Brush.horizontalGradient(listOf(Color(0xFF1C1C2A), Color(0xFF141420))),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .border(
                                        1.dp,
                                        if (selected) LsAccent.copy(alpha = 0.5f) else Color(0x18FFFFFF),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable { onAppLabelThemeChange(value) }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(
                                    label,
                                    color = if (selected) Color.White else Color(0x88FFFFFF),
                                    fontSize = 13.sp,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Text(
                    "CUSTOMIZE",
                    color = Color(0x55FFFFFF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )

                // Navigation tiles grid
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    maxItemsInEachRow = 2,
                ) {
                    listOf(
                        NavTile(Icons.Default.AccessAlarm, "Time Widget", Color(0xFF00C8FF), onTimeWidget),
                        NavTile(Icons.Default.Wallpaper, "Wallpaper", Color(0xFFFF6B9D), onWallpaper),
                        NavTile(Icons.Default.Title, "Custom Text", Color(0xFFB78DFF), onCustomText),
                        NavTile(Icons.Default.Apps, "App Drawer", Color(0xFFFFB347), onAppDrawer),
                    ).forEach { tile ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .background(
                                    Brush.verticalGradient(listOf(Color(0xFF1C1C2C), Color(0xFF0F0F1B))),
                                    RoundedCornerShape(20.dp)
                                )
                                .border(1.dp, Color(0x22FFFFFF), RoundedCornerShape(20.dp))
                                .clickable(onClick = tile.onClick)
                                .padding(vertical = 28.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(CircleShape)
                                        .background(tile.accent.copy(alpha = 0.13f))
                                        .border(1.dp, tile.accent.copy(alpha = 0.4f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tile.icon,
                                        contentDescription = tile.label,
                                        tint = tile.accent,
                                        modifier = Modifier.size(26.dp),
                                    )
                                }
                                Text(
                                    text = tile.label,
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    textAlign = TextAlign.Center,
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}
