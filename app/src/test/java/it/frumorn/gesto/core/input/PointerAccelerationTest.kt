package it.frumorn.gesto.core.input

import org.junit.Assert.assertEquals
import org.junit.Test

class PointerAccelerationTest {
    @Test
    fun speed_one_without_acceleration_sends_the_move_as_it_is() {
        assertMove(3f, 4f, PointerAcceleration.apply(3f, 4f, speed = 1f, accelerate = false))
    }

    @Test
    fun speed_two_doubles_the_move() {
        assertMove(6f, 8f, PointerAcceleration.apply(3f, 4f, speed = 2f, accelerate = false))
    }

    @Test
    fun acceleration_grows_with_the_length_of_the_move() {
        // |(3,4)| = 5, so k = 1 + 5/12.
        assertMove(4.25f, 5.6667f, PointerAcceleration.apply(3f, 4f, speed = 1f, accelerate = true))
    }

    @Test
    fun acceleration_stops_at_three_times() {
        assertMove(108f, 0f, PointerAcceleration.apply(36f, 0f, speed = 1f, accelerate = true))
    }

    @Test
    fun speed_is_limited_to_three() {
        assertMove(9f, 12f, PointerAcceleration.apply(3f, 4f, speed = 10f, accelerate = false))
    }

    @Test
    fun speed_is_limited_to_one_half() {
        assertMove(1.5f, 2f, PointerAcceleration.apply(3f, 4f, speed = 0.1f, accelerate = false))
    }

    private fun assertMove(dx: Float, dy: Float, actual: Pair<Float, Float>) {
        assertEquals(dx, actual.first, 0.001f)
        assertEquals(dy, actual.second, 0.001f)
    }
}
