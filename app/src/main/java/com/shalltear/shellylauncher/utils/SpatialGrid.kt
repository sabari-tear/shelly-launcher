package com.shalltear.shellylauncher.utils

import androidx.compose.ui.geometry.Offset

/**
 * Divides screen space into a coarse grid of rectangular buckets so nearest-icon
 * lookup during drag events is O(1) average instead of O(n).
 *
 * Cell size should be at least the full icon diameter (2 × hexRadiusPx) so that
 * checking the 3×3 neighbourhood around the touch point always covers the closest icon.
 *
 * Example: 400 × 800 dp screen, 90 dp cell → 5 × 9 = 45 cells, avg ~2 icons/cell.
 * Each move event checks at most 9 cells × ~2 items = ~18 comparisons vs 100+.
 */
class SpatialGrid(
    screenWidth: Float,
    screenHeight: Float,
    cellSizePx: Float,
) {
    private val cols = (screenWidth / cellSizePx).toInt().coerceAtLeast(1)
    private val rows = (screenHeight / cellSizePx).toInt().coerceAtLeast(1)
    private val cellW = screenWidth / cols
    private val cellH = screenHeight / rows

    // Pre-allocated arrays per cell — cleared and refilled on every build() call.
    private val buckets: Array<MutableList<Int>> = Array(cols * rows) { mutableListOf() }

    fun build(positions: List<Offset>) {
        buckets.forEach { it.clear() }
        positions.forEachIndexed { index, pos ->
            val col = (pos.x / cellW).toInt().coerceIn(0, cols - 1)
            val row = (pos.y / cellH).toInt().coerceIn(0, rows - 1)
            buckets[row * cols + col].add(index)
        }
    }

    /**
     * Returns the index of the closest position within [thresholdSq] squared pixels,
     * or -1 if nothing qualifies. Only the 3×3 neighbourhood around the touch cell
     * is inspected, which is sufficient when cell size ≥ icon diameter.
     */
    fun findNearest(touch: Offset, positions: List<Offset>, thresholdSq: Float): Int {
        val col = (touch.x / cellW).toInt().coerceIn(0, cols - 1)
        val row = (touch.y / cellH).toInt().coerceIn(0, rows - 1)

        var closestIdx = -1
        var minDistSq = thresholdSq // acts as the admission gate

        for (dr in -1..1) {
            for (dc in -1..1) {
                val nc = col + dc
                val nr = row + dr
                if (nc < 0 || nc >= cols || nr < 0 || nr >= rows) continue
                for (idx in buckets[nr * cols + nc]) {
                    val pos = positions[idx]
                    val dx = pos.x - touch.x
                    val dy = pos.y - touch.y
                    val distSq = dx * dx + dy * dy
                    if (distSq < minDistSq) {
                        minDistSq = distSq
                        closestIdx = idx
                    }
                }
            }
        }
        return closestIdx
    }
}
