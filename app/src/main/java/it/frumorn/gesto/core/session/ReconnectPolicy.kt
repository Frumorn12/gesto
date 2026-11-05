package it.frumorn.gesto.core.session

/** Backoff for silent reconnects: 0, 1, 2, 3, 3, ... seconds, within a total time budget. */
class ReconnectPolicy(private val budgetMillis: Long = 30_000) {
    /** Delay before attempt number [attempt], or null when the budget is spent and we should stop. */
    fun nextDelay(attempt: Int, elapsedMillis: Long): Long? {
        val delay = (attempt.coerceAtLeast(0).coerceAtMost(3)) * 1000L
        return if (elapsedMillis + delay > budgetMillis) null else delay
    }
}
