package it.frumorn.gesto.core.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StickyModifiersTest {
    @Test
    fun no_modifier_means_normal_typing() {
        assertNull(StickyModifiers().consume("c"))
    }

    @Test
    fun ctrl_turns_the_next_character_into_a_shortcut_and_switches_off() {
        val sticky = StickyModifiers()
        sticky.toggle(ModifierKey.Ctrl)

        assertEquals(listOf("ctrl", "c"), sticky.consume("c"))
        assertTrue(sticky.active.isEmpty())
        assertNull(sticky.consume("c"))
    }

    @Test
    fun modifiers_come_out_in_fixed_order_whatever_the_press_order() {
        val sticky = StickyModifiers()
        sticky.toggle(ModifierKey.Shift)
        sticky.toggle(ModifierKey.Ctrl)

        assertEquals(listOf("ctrl", "shift", "x"), sticky.consume("X"))
    }

    @Test
    fun all_four_in_order() {
        val sticky = StickyModifiers()
        ModifierKey.entries.reversed().forEach(sticky::toggle)

        assertEquals(listOf("ctrl", "alt", "super", "shift", "a"), sticky.consume("a"))
    }

    @Test
    fun toggling_twice_switches_off() {
        val sticky = StickyModifiers()
        sticky.toggle(ModifierKey.Alt)
        sticky.toggle(ModifierKey.Alt)

        assertTrue(sticky.active.isEmpty())
        assertNull(sticky.consume("a"))
    }

    @Test
    fun only_the_first_character_counts() {
        val sticky = StickyModifiers()
        sticky.toggle(ModifierKey.Ctrl)

        assertEquals(listOf("ctrl", "a"), sticky.consume("abc"))
    }

    @Test
    fun clear_switches_everything_off() {
        val sticky = StickyModifiers()
        sticky.toggle(ModifierKey.Ctrl)
        sticky.toggle(ModifierKey.Super)
        sticky.clear()

        assertTrue(sticky.active.isEmpty())
    }

    @Test
    fun special_keys_take_the_active_modifiers_and_switch_them_off() {
        val sticky = StickyModifiers()
        sticky.toggle(ModifierKey.Ctrl)
        sticky.toggle(ModifierKey.Alt)

        assertEquals(listOf("ctrl", "alt", "left"), sticky.withModifiers("left"))
        assertTrue(sticky.active.isEmpty())
        assertEquals(listOf("enter"), sticky.withModifiers("enter"))
    }

    @Test
    fun an_empty_typed_text_is_left_alone() {
        val sticky = StickyModifiers()
        sticky.toggle(ModifierKey.Ctrl)

        assertNull(sticky.consume(""))
        assertEquals(setOf(ModifierKey.Ctrl), sticky.active)
    }
}
