package it.frumorn.gesto

import it.frumorn.gesto.core.model.CapabilitySummary
import it.frumorn.gesto.core.model.ConnectionState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ModelTest {
    @Test
    fun defaultCapabilitySummaryIsSafe() {
        val summary = CapabilitySummary()
        assertFalse(summary.inputSupported)
        assertEquals("unknown", summary.inputBackend)
        assertFalse(summary.captureSupported)
        assertEquals("unknown", summary.captureBackend)
    }

    @Test
    fun initialUiStateIsDisconnected() {
        val state = GestoUiState()
        assertEquals(ConnectionState.Disconnected, state.connectionState)
        assertEquals(Screen.Onboarding, state.screen)
    }
}
