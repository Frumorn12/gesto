package it.frumorn.gesto.ui.state

import it.frumorn.gesto.GestoUiState
import it.frumorn.gesto.core.model.CapabilitySummary
import it.frumorn.gesto.core.model.ConnectionState
import it.frumorn.gesto.core.model.TrustedHost
import it.frumorn.gesto.core.network.toCapabilitySummary
import it.frumorn.gesto.ui.toTouchpadState
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchpadInputTest {
    /** What `get_capabilities` / `auth_request` report for each input backend of the daemon. */
    private fun capabilities(backend: String, supported: Boolean = true, approval: Boolean = false): CapabilitySummary =
        JSONObject(
            """{"input":{"supported":$supported,"backend":"$backend","requires_user_approval":$approval}}""",
        ).toCapabilitySummary()

    private val portal = capabilities("wayland-portal", approval = true)
    private val uinput = capabilities("uinput")
    private val hyprland = capabilities("hyprland-ipc")

    @Test fun a_portal_pc_not_approved_yet_asks_for_approval() =
        assertEquals(TouchpadInput.NeedsApproval, touchpadInput(connected = true, capabilities = portal, inputApproved = false))

    @Test fun a_portal_pc_that_approved_is_ready_although_its_backend_name_is_unchanged() {
        assertEquals("wayland-portal", portal.inputBackend)
        assertEquals(TouchpadInput.Ready, touchpadInput(connected = true, capabilities = portal, inputApproved = true))
    }

    @Test fun a_uinput_pc_is_ready_at_once() =
        assertEquals(TouchpadInput.Ready, touchpadInput(connected = true, capabilities = uinput, inputApproved = false))

    @Test fun a_hyprland_pc_is_ready_at_once() =
        assertEquals(TouchpadInput.Ready, touchpadInput(connected = true, capabilities = hyprland, inputApproved = false))

    @Test fun a_pc_without_input_is_blocked() =
        assertEquals(
            TouchpadInput.Blocked,
            touchpadInput(connected = true, capabilities = capabilities("noop", supported = false), inputApproved = true),
        )

    @Test fun before_the_pc_answered_nothing_is_blocked() =
        assertEquals(TouchpadInput.Ready, touchpadInput(connected = false, capabilities = CapabilitySummary(), inputApproved = false))

    @Test fun clicks_are_sent_unless_the_pc_refuses_input() {
        assertTrue(TouchpadInput.Ready.sendsClicks)
        // The approval may already be there: the daemon decides, and says so if it is not.
        assertTrue(TouchpadInput.NeedsApproval.sendsClicks)
        assertFalse(TouchpadInput.Blocked.sendsClicks)
    }

    private val host = TrustedHost(
        id = "a",
        hostName = "pc",
        address = "192.168.0.22",
        port = 47771,
        fingerprint = "fp-a",
        deviceId = "device",
        sessionToken = "token",
        lastConnectedAt = 0L,
    )

    private fun touchpadOf(approved: Set<String>) = GestoUiState(
        connectionState = ConnectionState.Connected,
        connectedHost = host,
        capabilities = portal,
        inputApprovedHosts = approved,
    ).toTouchpadState()

    @Test fun the_touchpad_of_a_portal_pc_follows_the_approval_of_that_pc() {
        assertEquals(TouchpadInput.NeedsApproval, touchpadOf(emptySet()).input)
        assertEquals(TouchpadInput.Ready, touchpadOf(setOf("fp-a")).input)
        // Another PC's approval is not this one's.
        assertEquals(TouchpadInput.NeedsApproval, touchpadOf(setOf("fp-b")).input)
    }
}
