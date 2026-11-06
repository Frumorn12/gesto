package it.frumorn.gesto.core.video

/**
 * Pinch zoom of the remote screen: the view shows the content scaled by [scale] around its top-left
 * corner and moved by [offsetX], [offsetY] (view pixels), so a content point `c` is drawn at
 * `c * scale + offset`.
 *
 * The scale stays between [MIN_SCALE] and [MAX_SCALE], and the offset is always held so the content
 * covers the whole view: zooming in never shows the background around the PC's screen, and at
 * scale 1 there is no offset at all.
 */
data class ZoomTransform(
    val scale: Float = MIN_SCALE,
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
) {
    /** Multiplies the scale by [factor], keeping the content under ([focusX], [focusY]) where it is. */
    fun zoomBy(factor: Float, focusX: Float, focusY: Float, width: Float, height: Float): ZoomTransform {
        if (!factor.isFinite() || factor <= 0f) return this
        val next = (scale * factor).coerceIn(MIN_SCALE, MAX_SCALE)
        val (contentX, contentY) = toContent(focusX, focusY)
        return ZoomTransform(next, focusX - contentX * next, focusY - contentY * next).clamped(width, height)
    }

    /** Moves the zoomed content by ([dx], [dy]) view pixels, no further than its edges. */
    fun panBy(dx: Float, dy: Float, width: Float, height: Float): ZoomTransform {
        if (!dx.isFinite() || !dy.isFinite()) return this
        return copy(offsetX = offsetX + dx, offsetY = offsetY + dy).clamped(width, height)
    }

    /** The point of the unzoomed content that is drawn at view point ([x], [y]). */
    fun toContent(x: Float, y: Float): Pair<Float, Float> = (x - offsetX) / scale to (y - offsetY) / scale

    private fun clamped(width: Float, height: Float): ZoomTransform {
        if (scale <= MIN_SCALE) return ZoomTransform()
        return copy(
            offsetX = offsetX.coerceIn(width - width * scale, 0f),
            offsetY = offsetY.coerceIn(height - height * scale, 0f),
        )
    }

    companion object {
        const val MIN_SCALE = 1f
        const val MAX_SCALE = 3f
    }
}
