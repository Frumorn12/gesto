package it.frumorn.gesto.core.video

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyFrameRequestGateTest {
    @Test fun the_first_request_goes_out() = assertTrue(KeyFrameRequestGate().tryRequest(5_000L))

    @Test fun requests_within_a_second_are_dropped() {
        val gate = KeyFrameRequestGate()
        assertTrue(gate.tryRequest(10_000L))
        assertFalse(gate.tryRequest(10_100L))
        assertFalse(gate.tryRequest(10_999L))
        assertTrue(gate.tryRequest(11_000L))
    }

    @Test fun a_dropped_request_does_not_push_the_next_one_back() {
        val gate = KeyFrameRequestGate()
        assertTrue(gate.tryRequest(0L))
        assertFalse(gate.tryRequest(900L))
        assertTrue(gate.tryRequest(1_000L))
    }
}
