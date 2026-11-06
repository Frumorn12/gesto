package it.frumorn.gesto.core.network

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PairingAttemptsTest {
    private class FakeChannel : AutoCloseable {
        var closed = false
        override fun close() {
            closed = true
        }
    }

    @Test
    fun cancel_stops_the_attempt_and_closes_its_channel() {
        val attempts = PairingAttempts<FakeChannel>()
        val attempt = attempts.begin()
        val channel = FakeChannel()
        assertTrue(attempts.connected(attempt, channel))

        assertTrue(attempts.cancel())

        assertTrue(attempt.cancelled)
        assertTrue(channel.closed)
    }

    @Test
    fun a_cancel_after_the_channel_was_adopted_leaves_it_open() {
        val attempts = PairingAttempts<FakeChannel>()
        val attempt = attempts.begin()
        val channel = FakeChannel()
        attempts.connected(attempt, channel)

        attempts.adopted(attempt)
        // Cancel tapped just as the PC's Allow arrived: the session keeps its channel.
        val stopped = attempts.cancel()

        assertFalse(stopped)
        assertFalse(attempt.cancelled)
        assertFalse(channel.closed)
    }

    @Test
    fun a_new_attempt_cancels_the_one_running() {
        val attempts = PairingAttempts<FakeChannel>()
        val first = attempts.begin()
        val channel = FakeChannel()
        attempts.connected(first, channel)

        val second = attempts.begin()

        assertTrue(first.cancelled)
        assertTrue(channel.closed)
        assertFalse(second.cancelled)
    }

    @Test
    fun a_channel_that_opens_after_cancel_is_refused() {
        val attempts = PairingAttempts<FakeChannel>()
        val attempt = attempts.begin()
        attempts.cancel()

        assertFalse(attempts.connected(attempt, FakeChannel()))
    }

    @Test
    fun a_finished_attempt_is_no_longer_cancelled() {
        val attempts = PairingAttempts<FakeChannel>()
        val attempt = attempts.begin()
        attempts.finished(attempt)

        assertFalse(attempts.cancel())
        assertFalse(attempt.cancelled)
    }
}
