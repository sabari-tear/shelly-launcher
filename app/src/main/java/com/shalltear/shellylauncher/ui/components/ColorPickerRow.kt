package com.shalltear.shellylauncher.ui.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Preset swatch palette
private val PRESET_COLORS = listOf(
    0xFFFFFFFF.toInt(), // White
    0xFFCCCCCC.toInt(), // Light grey
    0xFF888888.toInt(), // Grey
    0xFFFFD60A.toInt(), // iOS yellow
    0xFFFF9F0A.toInt(), // Orange
    0xFFFF453A.toInt(), // iOS red
    0xFFFF2D55.toInt(), // Pink
    0xFFBF5AF2.toInt(), // Purple
    0xFF0A84FF.toInt(), // iOS blue
    0xFF32D74B.toInt(), // iOS green
    0xFF5AC8FA.toInt(), // Light blue
    0xFF30B0C7.toInt(), // Teal
)

/**
 * An inline color picker with:
 *  - Live preview swatch
 *  - Preset swatches for quick selection
 *  - R / G / B sliders for fine-tuning
 *
 * [colorArgb] is a packed ARGB Int (produce via [Color.toArgb], consume via [Color]).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerRow(
    label: String,
    colorArgb: Int,
    onColorChange: (Int) -> Unit,
) {
    val initial = Color(colorArgb)
    var r by remember(colorArgb) { mutableFloatStateOf(initial.red) }
    var g by remember(colorArgb) { mutableFloatStateOf(initial.green) }
    var b by remember(colorArgb) { mutableFloatStateOf(initial.blue) }

    fun emit() = onColorChange(Color(r, g, b, 1f).toArgb())

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF181825), Color(0xFF111119))),
                RoundedCornerShape(14.dp)
            )
            .border(1.dp, Color(0x18FFFFFF), RoundedCornerShape(14.dp))
            .padding(horizontal = 20.dp, vertical = 16.dp),
    ) {
        // Header row: label + preview swatch
        val animatedPreviewColor by animateColorAsState(
            targetValue = Color(colorArgb),
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "previewColor",
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(label, color = Color.White, fontSize = 16.sp)
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(animatedPreviewColor)
                    .border(1.5.dp, Color.White.copy(alpha = 0.3f), CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Preset palette
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            PRESET_COLORS.forEach { presetArgb ->
                val isSelected = colorArgb == presetArgb
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(Color(presetArgb))
                        .border(
                            width = if (isSelected) 2.5.dp else 1.dp,
                            color = if (isSelected) Color(0xFF7B6FEF) else Color.White.copy(alpha = 0.2f),
                            shape = CircleShape,
                        )
                        .clickable {
                            val c = Color(presetArgb)
                            r = c.red; g = c.green; b = c.blue
                            onColorChange(presetArgb)
                        }
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // R / G / B sliders
        ColorChannelSlider("R", r, Color(1f, 0f, 0f, 1f)) { r = it; emit() }
        ColorChannelSlider("G", g, Color(0f, 0.78f, 0f, 1f)) { g = it; emit() }
        ColorChannelSlider("B", b, Color(0.2f, 0.6f, 1f, 1f)) { b = it; emit() }
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Float,
    trackColor: Color,
    onValueChange: (Float) -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = label,
            color = Color(0x99FFFFFF),
            fontSize = 12.sp,
            modifier = Modifier.weight(0.06f),
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = trackColor,
                inactiveTrackColor = Color(0xFF444444),
            ),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = "${(value * 255).toInt()}",
            color = Color(0x99FFFFFF),
            fontSize = 11.sp,
            modifier = Modifier
                .weight(0.15f)
                .padding(start = 6.dp),
        )
    }
}
