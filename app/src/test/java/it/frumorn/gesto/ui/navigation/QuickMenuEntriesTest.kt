package it.frumorn.gesto.ui.navigation

import it.frumorn.gesto.SessionMode
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickMenuEntriesTest {
    @Test
    fun theScreenHasTheWholeMenu() {
        assertEquals(
            listOf(
                QuickMenuEntry.Keyboard,
                QuickMenuEntry.RightClick,
                QuickMenuEntry.Audio,
                QuickMenuEntry.TouchMode,
                QuickMenuEntry.Quality,
                QuickMenuEntry.GameMode,
                QuickMenuEntry.BackToTouchpad,
            ),
            quickMenuEntries(SessionMode.Display, captureSupported = true),
        )
    }

    @Test
    fun theTouchpadLeavesOutTheScreenEntriesAndLeadsBackToThePcs() {
        assertEquals(
            listOf(QuickMenuEntry.Keyboard, QuickMenuEntry.RightClick, QuickMenuEntry.GameMode, QuickMenuEntry.BackToPcs),
            quickMenuEntries(SessionMode.Touchpad, captureSupported = true),
        )
    }

    @Test
    fun commandsLeadBackToTheTouchpad() {
        assertEquals(
            QuickMenuEntry.BackToTouchpad,
            quickMenuEntries(SessionMode.Commands, captureSupported = true).last(),
        )
    }

    @Test
    fun noGameModeWithoutScreenSharing() {
        assert(QuickMenuEntry.GameMode !in quickMenuEntries(SessionMode.Display, captureSupported = false))
        assert(QuickMenuEntry.GameMode !in quickMenuEntries(SessionMode.Touchpad, captureSupported = false))
    }
}
