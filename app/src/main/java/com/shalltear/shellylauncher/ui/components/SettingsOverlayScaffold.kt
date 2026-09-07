package com.shalltear.shellylauncher.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val ScaffoldBgGradient = Brush.verticalGradient(
    listOf(Color(0xFF0C0C14), Color(0xFF07070D))
)
private val DividerGlow = Brush.horizontalGradient(
    listOf(Color.Transparent, Color(0x507B6FEF), Color.Transparent)
)

/**
 * Full-screen black overlay with a standard back/close header row.
 *
 * @param title     Screen title shown after the back button.
 * @param onBack    Called when the back button is tapped.
 * @param backIcon  Vector icon for the back/close button. Defaults to ArrowBack.
 * @param content   Content below the header, receives [ColumnScope] so callers
 *                  can use `Modifier.weight()` directly.
 */
@Composable
fun SettingsOverlayScaffold(
    title: String,
    onBack: () -> Unit,
    backIcon: ImageVector = Icons.AutoMirrored.Filled.ArrowBack,
    content: @Composable ColumnScope.() -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ScaffoldBgGradient)
            .pointerInput(Unit) {
                awaitEachGesture {
                    val event = awaitPointerEvent()
                    event.changes.forEach { it.consume() }
                }
            }
            .padding(top = 48.dp, start = 24.dp, end = 24.dp, bottom = 24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0x18FFFFFF))
                        .clickable(onClick = onBack),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = backIcon,
                        contentDescription = "Back",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 16.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(DividerGlow)
            )
            Spacer(modifier = Modifier.height(8.dp))
            this.content()
        }
    }
}
