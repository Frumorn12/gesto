package it.frumorn.gesto.core.system

import org.junit.Assert.assertEquals
import org.junit.Test

class DeviceNameTest {
    @Test fun the_name_the_user_gave_the_phone_wins() {
        assertEquals("Nothing Phone (3a)", DeviceName.choose("Nothing Phone (3a)", "Nothing", "A059"))
    }

    @Test fun without_a_name_maker_and_model() {
        assertEquals("Google Pixel 8", DeviceName.choose(null, "Google", "Pixel 8"))
        assertEquals("Google Pixel 8", DeviceName.choose("  ", "Google", "Pixel 8"))
    }

    @Test fun the_maker_is_capitalised_and_not_repeated() {
        assertEquals("Samsung SM-X200", DeviceName.choose(null, "samsung", "SM-X200"))
        assertEquals("Nothing Phone (3a)", DeviceName.choose(null, "Nothing", "Nothing Phone (3a)"))
    }

    @Test fun never_longer_than_eighty() {
        assertEquals(80, DeviceName.choose("x".repeat(200), "a", "b").length)
    }

    @Test fun never_empty() {
        assertEquals("Android", DeviceName.choose(null, "", ""))
    }
}
