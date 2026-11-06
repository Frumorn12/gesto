package it.frumorn.gesto.core.input

import org.junit.Assert.assertEquals
import org.junit.Test

class SubPixelCarryTest {
    @Test
    fun slow_moves_add_up_to_whole_pixels() {
        val carry = SubPixelCarry()

        assertEquals(0 to 0, carry.take(0.4f, 0.3f))
        assertEquals(0 to 0, carry.take(0.4f, 0.3f))
        assertEquals(1 to 0, carry.take(0.4f, 0.3f))
        assertEquals(0 to 1, carry.take(0f, 0.2f))
    }

    @Test
    fun whole_pixels_go_through_and_the_rest_waits() {
        val carry = SubPixelCarry()

        assertEquals(3 to -2, carry.take(3.7f, -2.6f))
        assertEquals(1 to -1, carry.take(0.4f, -0.5f))
    }

    @Test
    fun reset_forgets_what_was_left() {
        val carry = SubPixelCarry()
        carry.take(0.9f, -0.9f)

        carry.reset()

        assertEquals(0 to 0, carry.take(0.2f, -0.2f))
    }
}
