package com.shalltear.shellylauncher.ui.composable

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shalltear.shellylauncher.data.AppInfo
import com.shalltear.shellylauncher.utils.IconCache
import kotlinx.coroutines.delay

/**
 * Grid of search result apps rendered directly below the search bar.
 *
 * Each app pops in with a staggered spring animation (scale 0 → 1, fade 0 → 1).
 * The whole grid fades out when [isVisible] becomes false.
 */
@Composable
fun SearchResultsGrid(
    filteredApps: List<AppInfo>,
    isVisible: Boolean,
    hexRadiusPx: Float,
    onAppClick: (AppInfo) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val iconSizeDp = with(density) { (hexRadiusPx * 1.4f).toDp() }
    val cellSizeDp = with(density) { (hexRadiusPx * 2.4f).toDp() }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(200)),
        modifier = modifier,
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = cellSizeDp),
            contentPadding = PaddingValues(vertical = 8.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(filteredApps, key = { _, app -> app.packageName }) { index, app ->
                // Each item manages its own entrance so stagger is per-item
                var itemVisible by remember { mutableStateOf(false) }
                LaunchedEffect(app.packageName) {
                    delay(index.coerceAtMost(8) * 35L)  // stagger up to ~280ms, cap at 8 items
                    itemVisible = true
                }

                AnimatedVisibility(
                    visible = itemVisible,
                    enter = scaleIn(
                        initialScale = 0.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessMedium,
                        ),
                    ) + fadeIn(tween(160)),
                    exit = scaleOut(
                        targetScale = 0.2f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessHigh,
                        ),
                    ) + fadeOut(tween(100)),
                ) {
                    SearchResultItem(
                        app = app,
                        iconSizeDp = iconSizeDp,
                        onClick = { onAppClick(app) },
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultItem(
    app: AppInfo,
    iconSizeDp: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .padding(vertical = 4.dp, horizontal = 4.dp),
    ) {
        val iconBitmap = remember(app.packageName) {
            IconCache.getImageBitmap(app.packageName) ?: app.icon.asImageBitmap()
        }
        Image(
            bitmap = iconBitmap,
            contentDescription = app.name,
            modifier = Modifier.size(iconSizeDp),
        )
        Text(
            text = app.name,
            color = Color(0xCCFFFFFF),
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.3.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
        )
    }
}
