package it.frumorn.gesto.core.video

/**
 * Spaces out the keyframe requests of a decoder that keeps asking (it does so for every picture it
 * cannot use): the daemon serves one a second anyway, and each request costs a round trip.
 */
class KeyFrameRequestGate(private val minIntervalMs: Long = 1_000L) {
    private var lastRequestAtMs: Long? = null

    /** True when a request may go out at [nowMs] (monotonic); it is then counted as sent. */
    @Synchronized
    fun tryRequest(nowMs: Long): Boolean {
        val last = lastRequestAtMs
        if (last != null && nowMs - last < minIntervalMs) return false
        lastRequestAtMs = nowMs
        return true
    }
}
