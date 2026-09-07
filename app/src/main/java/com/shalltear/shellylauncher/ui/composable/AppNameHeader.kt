package com.shalltear.shellylauncher.ui.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.geometry.Offset as TextOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shellylauncher.data.AppInfo

@Composable
fun AppNameHeader(
    apps: List<AppInfo>,
    selectedIndex: State<Int>,
    isVisible: Boolean,
    labelTheme: String = "auto",
) {
    val idx = selectedIndex.value
    val selectedApp = if (idx != -1) apps.getOrNull(idx) else null

    AnimatedVisibility(
        visible = isVisible && selectedApp != null,
        enter = fadeIn(tween(200)) + expandVertically(tween(200)),
        exit = fadeOut(tween(200)) + shrinkVertically(tween(200)),
        modifier = Modifier.fillMaxWidth()
    ) {
        selectedApp?.let {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 80.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .background(
                            Color(0x26FFFFFF),
                            RoundedCornerShape(50)
                        )
                        .border(
                            1.dp,
                            Color(0x33FFFFFF),
                            RoundedCornerShape(50)
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Image(
                        bitmap = it.icon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val (textColor, shadowColor) = when (labelTheme) {
                        "light" -> Color.White to Color.Black.copy(alpha = 0.5f)
                        "dark" -> Color(0xFF1C1C1E) to Color.White.copy(alpha = 0.8f)
                        else -> Color.White to Color.Transparent
                    }
                    Text(
                        text = it.name.uppercase(),
                        color = textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(
                            shadow = if (labelTheme != "auto") Shadow(
                                color = shadowColor,
                                offset = TextOffset(1f, 1f),
                                blurRadius = 2f
                            ) else null
                        )
                    )
                }
            }
        }
    }
}
