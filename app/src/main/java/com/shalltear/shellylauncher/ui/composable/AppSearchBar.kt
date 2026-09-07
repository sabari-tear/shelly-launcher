package com.shalltear.shellylauncher.ui.composable

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Search pill rendered at the top of the app grid.
 *
 * Visibility and scale are handled entirely by the caller via [Modifier.graphicsLayer], so
 * this composable is always in the composition tree — [onGloballyPositioned] fires even before
 * the grid opens, letting the drag loop track the pill's screen bounds.
 *
 * When [isSearchMode] is false the pill shows a static "SEARCH APPS" label that matches the
 * [AppNameHeader] style. When [isSearchMode] is true a live [BasicTextField] appears.
 */
@Composable
fun AppSearchBar(
    searchQuery: String,
    onQueryChange: (String) -> Unit,
    isSearchMode: Boolean,
    focusRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(40.dp)
            .background(Color(0x26FFFFFF), RoundedCornerShape(50))
            .border(1.dp, Color(0x33FFFFFF), RoundedCornerShape(50))
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = null,
            tint = Color(0xAAFFFFFF),
            modifier = Modifier.size(14.dp),
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 10.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            // Static label shown when not actively searching
            if (!isSearchMode) {
                Text(
                    text = "SEARCH APPS",
                    color = Color(0x88FFFFFF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                )
            }

            // Live text field — only present in search mode
            if (isSearchMode) {
                // Placeholder sits behind the BasicTextField
                if (searchQuery.isEmpty()) {
                    Text(
                        text = "Type to search…",
                        color = Color(0x55FFFFFF),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    )
                }
                BasicTextField(
                    value = searchQuery,
                    onValueChange = onQueryChange,
                    singleLine = true,
                    cursorBrush = SolidColor(Color.White),
                    textStyle = TextStyle(
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                )
            }
        }

        // Clear button — only visible when there is text
        if (isSearchMode && searchQuery.isNotEmpty()) {
            IconButton(
                onClick = { onQueryChange("") },
                modifier = Modifier.size(28.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Clear search",
                    tint = Color(0xAAFFFFFF),
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}
