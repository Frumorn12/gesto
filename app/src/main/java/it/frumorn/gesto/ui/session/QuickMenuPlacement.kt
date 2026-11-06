package it.frumorn.gesto.ui.session

/** Where the quick menu goes (top-left corner, pixels) and the point it grows from (fractions of its size). */
data class QuickMenuPlacement(val x: Int, val y: Int, val originX: Float, val originY: Float)

/** The purple button, in pixels inside the area the menu lives in. */
data class QuickButtonBounds(val left: Int, val top: Int, val size: Int) {
    val right: Int get() = left + size
    val bottom: Int get() = top + size
    val centreX: Int get() = left + size / 2
    val centreY: Int get() = top + size / 2
}

/**
 * Places a [menuWidth] × [menuHeight] menu in an [areaWidth] × [areaHeight] area, next to the
 * purple [button]: above it when the button is in the lower half, below it in the upper half, the
 * other way round when it does not fit, and beside it when neither does (a phone on its side is
 * not tall enough for the menu and the button stacked). It lines up with the button's edge nearest
 * to the side of the screen the button is on, and never leaves the area.
 *
 * Without a button (the menu opened with the back key on a mode that has none) it sits in the
 * bottom-right corner, [inset] from the edges.
 */
fun placeQuickMenu(
    areaWidth: Int,
    areaHeight: Int,
    button: QuickButtonBounds?,
    menuWidth: Int,
    menuHeight: Int,
    gap: Int,
    inset: Int,
): QuickMenuPlacement {
    val maxX = (areaWidth - menuWidth).coerceAtLeast(0)
    val maxY = (areaHeight - menuHeight).coerceAtLeast(0)
    if (button == null) {
        return QuickMenuPlacement((areaWidth - inset - menuWidth).coerceIn(0, maxX), (areaHeight - inset - menuHeight).coerceIn(0, maxY), 1f, 1f)
    }
    val endAligned = button.centreX > areaWidth / 2
    val lower = button.centreY > areaHeight / 2
    val originX = if (endAligned) 1f else 0f
    val x = (if (endAligned) button.right - menuWidth else button.left).coerceIn(0, maxX)
    val above = button.top - gap - menuHeight
    val below = button.bottom + gap
    val fitsAbove = above >= 0
    val fitsBelow = below + menuHeight <= areaHeight
    return when {
        lower && fitsAbove -> QuickMenuPlacement(x, above, originX, 1f)
        !lower && fitsBelow -> QuickMenuPlacement(x, below, originX, 0f)
        fitsAbove -> QuickMenuPlacement(x, above, originX, 1f)
        fitsBelow -> QuickMenuPlacement(x, below, originX, 0f)
        else -> {
            val sideX = if (endAligned) button.left - gap - menuWidth else button.right + gap
            val sideY = if (lower) button.bottom - menuHeight else button.top
            QuickMenuPlacement(sideX.coerceIn(0, maxX), sideY.coerceIn(0, maxY), originX, if (lower) 1f else 0f)
        }
    }
}
