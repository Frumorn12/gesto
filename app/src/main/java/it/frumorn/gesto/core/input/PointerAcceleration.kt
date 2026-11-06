package it.frumorn.gesto.core.input

import kotlin.math.hypot
import kotlin.math.min

/** Turns a finger move on the touchpad into the move sent to the PC. */
object PointerAcceleration {
    const val MIN_SPEED = 0.5f
    const val MAX_SPEED = 3f
    const val DEFAULT_SPEED = 1f

    /** A move this long (in pixels, in one step) gets twice the gain on top of the speed. */
    private const val ACCELERATION_STEP = 12f

    /** The most acceleration adds on top of the speed. */
    private const val MAX_EXTRA_GAIN = 2f

    /**
     * Scales ([dx], [dy]) by [speed] (limited to [MIN_SPEED]..[MAX_SPEED]) and, when [accelerate] is
     * on, by up to three times more for a fast move: the longer the step, the further it goes.
     */
    fun apply(dx: Float, dy: Float, speed: Float, accelerate: Boolean): Pair<Float, Float> {
        val gain = clampSpeed(speed) * if (accelerate) 1f + min(hypot(dx, dy) / ACCELERATION_STEP, MAX_EXTRA_GAIN) else 1f
        return dx * gain to dy * gain
    }

    /** [speed] limited to [MIN_SPEED]..[MAX_SPEED]; anything that is not a number becomes [DEFAULT_SPEED]. */
    fun clampSpeed(speed: Float): Float = if (speed.isNaN()) DEFAULT_SPEED else speed.coerceIn(MIN_SPEED, MAX_SPEED)
}
