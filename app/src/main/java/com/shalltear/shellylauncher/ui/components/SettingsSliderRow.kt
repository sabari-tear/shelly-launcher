package com.shalltear.shellylauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val SliderAccent = Color(0xFF7B6FEF)

/**
 * A card with a label + current-value label on top, and a [Slider] below.
 * Fires a light haptic tick whenever the slider moves to a new discrete step.
 */
@Composable
fun SettingsSliderRow(
    label: String,
    valueLabel: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
) {
    val haptic = LocalHapticFeedback.current
    var lastHapticValue by remember { mutableFloatStateOf(value) }

    Column(
        modifier = modifier
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
            Text(label, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(valueLabel, color = SliderAccent, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Slider(
            value = value,
            onValueChange = { newValue ->
                // Fire haptic when value crosses a discrete step boundary
                val stepSize = if (steps > 0) {
                    (valueRange.endInclusive - valueRange.start) / steps
                } else {
                    (valueRange.endInclusive - valueRange.start) / 100f
                }
                if (kotlin.math.abs(newValue - lastHapticValue) >= stepSize) {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    lastHapticValue = newValue
                }
                onValueChange(newValue)
            },
            valueRange = valueRange,
            steps = steps,
            colors = SliderDefaults.colors(
                thumbColor = SliderAccent,
                activeTrackColor = SliderAccent,
                inactiveTrackColor = SliderAccent.copy(alpha = 0.2f),
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
