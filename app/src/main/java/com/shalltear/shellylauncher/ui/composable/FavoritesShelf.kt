package com.shalltear.shellylauncher.ui.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shellylauncher.data.AppInfo

/**
 * Animated shelf of favourite/pinned apps at the bottom of the homescreen.
 * Slides up from the bottom edge on entrance and slides back down on exit.
 * Shows up to 5 pinned apps. Long-press the × badge to remove a pin.
 */
@Composable
fun FavoritesShelf(
    visible: Boolean,
    favorites: List<AppInfo>,
    onFavoriteClick: (AppInfo) -> Unit,
    onRemoveFavorite: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = visible && favorites.isNotEmpty(),
        enter = slideInVertically(
            animationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                dampingRatio = Spring.DampingRatioMediumBouncy,
            ),
            initialOffsetY = { it },  // Slide up from below
        ) + fadeIn(animationSpec = tween(200)),
        exit = slideOutVertically(
            animationSpec = tween(200),
            targetOffsetY = { it },   // Slide back down
        ) + fadeOut(animationSpec = tween(150)),
        modifier = modifier,
    ) {
        val haptic = LocalHapticFeedback.current

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = "Favourites",
                color = Color(0x77FFFFFF),
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 4.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0x0DFFFFFF), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0x1AFFFFFF), RoundedCornerShape(16.dp))
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                favorites.forEach { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            // App icon
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0x22FFFFFF))
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onFavoriteClick(app)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Image(
                                    bitmap = app.icon.asImageBitmap(),
                                    contentDescription = app.name,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(38.dp),
                                )
                            }

                            // Remove badge (×) in the top-right corner
                            Box(
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xCC181825))
                                    .border(1.dp, Color(0x447B6FEF), CircleShape)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        onRemoveFavorite(app)
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Remove favourite",
                                    tint = Color(0xCCFFFFFF),
                                    modifier = Modifier.size(9.dp),
                                )
                            }
                        }

                        // App name label
                        Text(
                            text = app.name,
                            color = Color(0xCCFFFFFF),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Pin hint — shown so user knows how to add more
            if (favorites.size < 5) {
                Text(
                    text = "Drag an app to ☆ to pin it",
                    color = Color(0x44FFFFFF),
                    fontSize = 10.sp,
                    modifier = Modifier.padding(start = 4.dp),
                )
            }
        }
    }
}
