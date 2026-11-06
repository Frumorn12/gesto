package it.frumorn.gesto

import it.frumorn.gesto.core.model.RemoteScreenConnectionState
import it.frumorn.gesto.core.model.ScreenSource
import it.frumorn.gesto.core.model.TrustedHost
import it.frumorn.gesto.core.network.GestoApiException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionResetTest {
    private val pcA = TrustedHost(
        id = "a",
        hostName = "pc-a",
        address = "192.168.0.22",
        port = 47771,
        fingerprint = "fp-a",
        deviceId = "device",
        sessionToken = "token",
        lastConnectedAt = 0L,
    )

    private val monitorOfA = ScreenSource(
        id = "hyprland:monitor:DP-1",
        label = "DP-1",
        kind = "monitor",
        backend = "hyprland-grim",
        width = 1920,
        height = 1080,
        x = 0,
        y = 0,
        scale = 1.0,
        focused = true,
    )

    @Test fun the_screen_source_of_the_last_pc_is_not_carried_to_the_next() {
        val withA = GestoUiState(
            connectedHost = pcA,
            screenSources = listOf(monitorOfA),
            selectedScreenSourceId = monitorOfA.id,
            videoWidth = 1920,
            videoHeight = 1080,
            screenStreaming = true,
            screenConnectionState = RemoteScreenConnectionState.Streaming,
            autoQualityRung = 2,
            latencyMs = 4,
            touchpadHints = true,
            quickMenuPulse = true,
        )
        val reset = withA.withoutSession()
        assertNull(reset.selectedScreenSourceId)
        assertTrue(reset.screenSources.isEmpty())
        assertNull(reset.screenStreamInfo)
        assertNull(reset.videoWidth)
        assertNull(reset.videoHeight)
        assertFalse(reset.screenStreaming)
        assertEquals(RemoteScreenConnectionState.Idle, reset.screenConnectionState)
        assertEquals(0, reset.autoQualityRung)
        assertNull(reset.latencyMs)
        assertFalse(reset.touchpadHints)
        assertFalse(reset.quickMenuPulse)
    }

    @Test fun what_the_user_set_stays() {
        val before = GestoUiState(connectedHost = pcA, haptics = false, pointerSpeed = 2f, remoteScreenGameMode = true)
        val reset = before.withoutSession()
        assertEquals(pcA, reset.connectedHost)
        assertFalse(reset.haptics)
        assertEquals(2f, reset.pointerSpeed)
        assertTrue(reset.remoteScreenGameMode)
    }

    @Test fun a_source_the_pc_does_not_have_is_recognised() {
        assertTrue(GestoApiException("command_failed", "Screen source not found: hyprland:monitor:DP-1", true).isScreenSourceGone())
        assertFalse(GestoApiException("command_failed", "No screen sources available", true).isScreenSourceGone())
        assertFalse(IllegalStateException("Screen source not found").isScreenSourceGone())
    }
}
