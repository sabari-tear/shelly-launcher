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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatAlignLeft
import androidx.compose.material.icons.filled.FormatAlignRight
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.shalltear.shellylauncher.ui.components.SettingsOverlayScaffold
import com.shalltear.shellylauncher.ui.components.SettingsSliderRow
import com.shalltear.shellylauncher.ui.components.ColorPickerRow

@Composable
fun CustomTextSettingsOverlay(
    visible: Boolean,
    customText: String,
    customFontSizeSp: Float,
    customLetterSpacing: Float,
    customAlignment: String,
    customFontFilename: String?,
    customFontFamily: FontFamily?,
    customTextVisible: Boolean,
    customTextColorArgb: Int,
    customOpacityPct: Float,
    onTextChange: (String) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onLetterSpacingChange: (Float) -> Unit,
    onAlignmentChange: (String) -> Unit,
    onVisibilityChange: (Boolean) -> Unit,
    onColorChange: (Int) -> Unit,
    onOpacityChange: (Float) -> Unit,
    onUploadFont: () -> Unit,
    onResetFont: () -> Unit,
    onMoveResize: () -> Unit,
    onDismiss: () -> Unit,
    saveCustomText: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(spring(stiffness = Spring.StiffnessMedium)),
        exit = fadeOut(spring(stiffness = Spring.StiffnessMediumLow)),
    ) {
        SettingsOverlayScaffold(
            title = "Custom Text",
            onBack = onDismiss,
        ) {
            // The scaffold adds the header; content goes into a scrollable column.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                Spacer(modifier = Modifier.height(8.dp))

                // Live preview card
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(listOf(Color(0xFF141420), Color(0xFF0C0C16))),
                            RoundedCornerShape(16.dp)
                        )
                        .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(16.dp))
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    if (customText.isNotBlank()) {
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
                            color = Color(customTextColorArgb).copy(alpha = customOpacityPct / 100f),
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        Text("Preview", color = Color(0x44FFFFFF), fontSize = 14.sp)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Show widget toggle
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
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Text("Show on Homescreen", color = Color.White, fontSize = 16.sp)
                    Switch(checked = customTextVisible, onCheckedChange = { onVisibilityChange(it); saveCustomText() })
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Text input
                OutlinedTextField(
                    value = customText,
                    onValueChange = { onTextChange(it); saveCustomText() },
                    label = { Text("Text", color = Color(0x99FFFFFF)) },
                    textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF7B6FEF),
                        unfocusedBorderColor = Color(0x30FFFFFF),
                        cursorColor = Color(0xFF7B6FEF),
                    ),
                    minLines = 2,
                    maxLines = 5,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Font Size slider
                SettingsSliderRow(
                    label = "Font Size",
                    valueLabel = "${customFontSizeSp.toInt()}sp",
                    value = customFontSizeSp,
                    onValueChange = { onFontSizeChange(it); saveCustomText() },
                    valueRange = 10f..80f,
                    steps = 69,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Opacity slider
                SettingsSliderRow(
                    label = "Opacity",
                    valueLabel = "${customOpacityPct.toInt()}%",
                    value = customOpacityPct,
                    onValueChange = { onOpacityChange(it); saveCustomText() },
                    valueRange = 0f..100f,
                    steps = 99,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Letter Spacing slider
                SettingsSliderRow(
                    label = "Letter Spacing",
                    valueLabel = "${"%.2f".format(customLetterSpacing)}em",
                    value = customLetterSpacing,
                    onValueChange = { onLetterSpacingChange(it); saveCustomText() },
                    valueRange = 0f..0.5f,
                    steps = 49,
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Alignment picker
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
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            Triple("left", Icons.Default.FormatAlignLeft, "Left"),
                            Triple("center", Icons.Default.FormatAlignCenter, "Center"),
                            Triple("right", Icons.Default.FormatAlignRight, "Right"),
                        ).forEach { (key, icon, label) ->
                            val selected = customAlignment == key
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
                                    .clickable { onAlignmentChange(key); saveCustomText() }
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

                Spacer(modifier = Modifier.height(12.dp))

                // Color picker
                ColorPickerRow(
                    label = "Text Color",
                    colorArgb = customTextColorArgb,
                    onColorChange = { onColorChange(it); saveCustomText() },
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Font picker
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
                        Column {
                            Text("Font", color = Color.White, fontSize = 16.sp)
                            Text(
                                text = if (customFontFilename != null) "Custom font loaded" else "Default system font",
                                color = Color(0x99FFFFFF), fontSize = 12.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = onUploadFont,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C2A)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) { Text("Upload Font (.ttf/.otf)", color = Color.White, fontSize = 13.sp) }

                        if (customFontFilename != null) {
                            Button(
                                onClick = onResetFont,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2A1010)),
                                shape = RoundedCornerShape(10.dp),
                            ) { Text("Reset", color = Color(0xFFFF6B6B), fontSize = 13.sp) }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Move & Resize button
                Button(
                    onClick = onMoveResize,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1C1C2C)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.FormatAlignCenter, contentDescription = null, tint = Color(0xFF7B6FEF), modifier = Modifier.size(18.dp))
                        Text("Move & Resize on Screen", color = Color.White, fontSize = 15.sp)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}
