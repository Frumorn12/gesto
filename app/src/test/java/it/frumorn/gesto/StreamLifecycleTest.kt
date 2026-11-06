package it.frumorn.gesto

import it.frumorn.gesto.core.model.ConnectionState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StreamLifecycleTest {
    private fun autoStart(
        screen: Screen = Screen.Display,
        state: ConnectionState = ConnectionState.Connected,
        capture: Boolean = true,
        wanted: Boolean = false,
        started: Boolean = true,
    ) = shouldAutoStartStream(screen, state, capture, wanted, started)

    @Test fun the_screen_starts_its_stream_in_the_foreground() = assertTrue(autoStart())

    @Test fun nothing_starts_while_the_app_is_in_the_background() {
        // A reconnect in the background must not start capturing the PC's screen again.
        assertFalse(autoStart(started = false))
    }

    @Test fun nothing_starts_away_from_the_screen() {
        assertFalse(autoStart(screen = Screen.Touchpad))
        assertFalse(autoStart(screen = Screen.Settings))
    }

    @Test fun nothing_starts_without_a_connected_pc_that_shares_its_screen() {
        assertFalse(autoStart(state = ConnectionState.Connecting))
        assertFalse(autoStart(capture = false))
    }

    @Test fun a_stream_already_wanted_is_not_started_twice() = assertFalse(autoStart(wanted = true))
}
