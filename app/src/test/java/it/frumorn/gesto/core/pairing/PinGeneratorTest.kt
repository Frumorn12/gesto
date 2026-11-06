package it.frumorn.gesto.core.pairing

import java.util.Random
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinGeneratorTest {
    @Test fun four_digits() {
        val pin = PinGenerator.newPin(Random(42))
        assertEquals(4, pin.length)
        assertTrue(pin, pin.all { it in '0'..'9' })
    }

    @Test fun small_numbers_keep_their_leading_zeros() {
        val seven = object : Random() {
            override fun nextInt(bound: Int): Int = 7
        }
        assertEquals("0007", PinGenerator.newPin(seven))
    }

    @Test fun every_pin_has_four_digits() {
        val random = Random(7)
        repeat(500) {
            val pin = PinGenerator.newPin(random)
            assertTrue(pin, pin.length == 4 && pin.all { it in '0'..'9' })
        }
    }
}
