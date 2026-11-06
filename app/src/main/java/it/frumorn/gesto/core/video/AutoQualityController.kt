package it.frumorn.gesto.core.video

/**
 * The Automatic quality's decision to step down a rung.
 *
 * The stream's frame rate is judged in windows of [windowMillis]: a window is bad when the frames
 * shown average less than [threshold] of the target. After [badWindows] bad windows in a row the
 * quality goes one rung down (until [maxRung]) and the count starts over; a good window starts it
 * over too. It never goes back up: [reset] returns to the first rung, for a new session.
 *
 * A window is judged when the first sample after its end arrives, so a pause in the samples (a
 * still screen, a restart) never produces windows with nothing in them.
 */
class AutoQualityController(
    private val windowMillis: Long = 2_000,
    private val badWindows: Int = 3,
    private val threshold: Double = 0.75,
    private val maxRung: Int = 3,
) {
    /** 0 is the best rung, [maxRung] the lowest. */
    var rung: Int = 0
        private set

    private var windowStart = 0L
    private var fpsSum = 0.0
    private var samples = 0
    private var targetFps = 0
    private var badInARow = 0

    /**
     * One reading of the stream: [deliveredFps] shown of the [targetFps] asked for, at [nowMillis]
     * (a monotonic clock). Returns the rung to stream with when it just stepped down, else null.
     */
    fun onStats(deliveredFps: Double, targetFps: Int, nowMillis: Long): Int? {
        if (targetFps <= 0 || !deliveredFps.isFinite()) return null
        if (samples > 0 && nowMillis - windowStart >= windowMillis) {
            val lowered = closeWindow()
            // The stream restarts with the new rung: this reading belongs to the old one.
            if (lowered != null) return lowered
        }
        if (samples == 0) windowStart = nowMillis
        fpsSum += deliveredFps
        samples += 1
        this.targetFps = targetFps
        return null
    }

    /** A new stream started: the windows of the old one do not count, the rung stays. */
    fun onStreamRestarted() {
        clearWindow()
        badInARow = 0
    }

    /** Back to the first rung, with nothing counted: a new session. */
    fun reset() {
        onStreamRestarted()
        rung = 0
    }

    private fun closeWindow(): Int? {
        val bad = fpsSum / samples < threshold * targetFps
        clearWindow()
        if (!bad) {
            badInARow = 0
            return null
        }
        badInARow += 1
        if (badInARow < badWindows) return null
        badInARow = 0
        if (rung >= maxRung) return null
        rung += 1
        return rung
    }

    private fun clearWindow() {
        fpsSum = 0.0
        samples = 0
    }
}

/**
 * Whether the frames arrive late: later than the quickest frame of this stream by [lateMillis] or
 * more.
 *
 * The PC sends a frame only when its screen changes, so a low frame rate alone may be a still
 * screen or a video playing at 30 fps rather than a slow network. Frames that queue up on the way
 * do mean the network is not keeping up. The ages compare the PC's clock with the phone's, so they
 * carry the difference between the two clocks; measuring from the quickest frame cancels it.
 */
class StreamDelay(private val lateMillis: Long = DEFAULT_LATE_MS) {
    private var quickest: Long? = null

    /** A frame [ageMillis] old (phone clock minus the PC's timestamp) arrived. */
    fun isLate(ageMillis: Long): Boolean {
        val base = quickest?.coerceAtMost(ageMillis) ?: ageMillis
        quickest = base
        return ageMillis - base >= lateMillis
    }

    /** A new stream: its own quickest frame is the reference. */
    fun reset() {
        quickest = null
    }

    companion object {
        const val DEFAULT_LATE_MS = 150L
    }
}
