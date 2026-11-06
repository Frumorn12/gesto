package it.frumorn.gesto.core.system

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    // The daemon counts the 80 in UTF-8 bytes.
    @Test fun a_long_accented_name_is_cut_at_eighty_bytes() {
        val name = DeviceName.choose("Telefono di Niccolò ".repeat(3) + "è".repeat(60), "a", "b")
        assertTrue(name.toByteArray(Charsets.UTF_8).size <= 80)
        assertTrue(name.toByteArray(Charsets.UTF_8).size >= 79)
        assertTrue(("Telefono di Niccolò ".repeat(3) + "è".repeat(60)).startsWith(name))
    }

    @Test fun a_cut_never_splits_a_character() {
        // 4 bytes each, and two UTF-16 units: 20 fit, the 21st would need 84 bytes.
        val name = DeviceName.choose("😀".repeat(30), "a", "b")
        assertEquals("😀".repeat(20), name)

        val cyrillic = DeviceName.choose("x" + "Ж".repeat(50), "a", "b")
        assertEquals("x" + "Ж".repeat(39), cyrillic)
    }

    @Test fun a_cut_leaves_no_trailing_space() {
        assertEquals("a".repeat(78), DeviceName.choose("a".repeat(78) + " è", "a", "b"))
    }

    @Test fun never_empty() {
        assertEquals("Android", DeviceName.choose(null, "", ""))
    }
}
