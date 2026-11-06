package it.frumorn.gesto.core.network

/**
 * The pairing in flight, so a cancel can stop it by closing its connection. That connection is the
 * attempt's only until the client adopts it: from then on it carries the session, and a cancel that
 * arrives late leaves it alone.
 *
 * Not thread-safe: [GestoClient] calls it under its channel lock.
 */
internal class PairingAttempts<C : AutoCloseable> {
    class Attempt<C : AutoCloseable> internal constructor() {
        internal var channel: C? = null

        /** Stopped by a cancel or by a newer attempt. */
        var cancelled = false
            internal set
    }

    private var current: Attempt<C>? = null

    /** Starts tracking a new attempt; one still running is cancelled. */
    fun begin(): Attempt<C> {
        cancel()
        return Attempt<C>().also { current = it }
    }

    /** [attempt] opened [channel]; false (and the channel is not kept) when it was cancelled meanwhile. */
    fun connected(attempt: Attempt<C>, channel: C): Boolean {
        if (attempt.cancelled) return false
        attempt.channel = channel
        return true
    }

    /** [attempt]'s channel is now the client's: nothing here closes it any more. */
    fun adopted(attempt: Attempt<C>) {
        attempt.channel = null
        if (current === attempt) current = null
    }

    /** [attempt] is over, however it ended. */
    fun finished(attempt: Attempt<C>) {
        if (current === attempt) current = null
    }

    /** Stops the attempt in flight and closes its connection. False when there was none to stop. */
    fun cancel(): Boolean {
        val attempt = current ?: return false
        current = null
        attempt.cancelled = true
        attempt.channel?.close()
        attempt.channel = null
        return true
    }
}
