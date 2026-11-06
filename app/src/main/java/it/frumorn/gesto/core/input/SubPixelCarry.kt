package it.frumorn.gesto.core.input

/**
 * Keeps the fraction of a pixel each move leaves behind. The virtual mouse on the PC moves in whole
 * pixels and rounds every move on its own, so a slow finger (or a low pointer speed) would otherwise
 * lose its small steps, or have them doubled.
 */
class SubPixelCarry {
    private var x = 0f
    private var y = 0f

    /** The whole pixels of ([dx], [dy]) plus what earlier moves left; the rest waits for the next move. */
    fun take(dx: Float, dy: Float): Pair<Int, Int> {
        x += dx
        y += dy
        val wholeX = x.toInt()
        val wholeY = y.toInt()
        x -= wholeX
        y -= wholeY
        return wholeX to wholeY
    }

    fun reset() {
        x = 0f
        y = 0f
    }
}
