package it.frumorn.gesto.ui

import it.frumorn.gesto.AddPcStep
import it.frumorn.gesto.GestoUiState
import it.frumorn.gesto.core.network.GestoInvite
import it.frumorn.gesto.ui.state.InviteEndpointState
import org.junit.Assert.assertEquals
import org.junit.Test

class AddPcStateTest {
    private val invite = GestoInvite.parse("gesto://invite?host=Legion&address=192.168.0.22&code=123456&expires=1770000000")

    @Test fun the_confirmation_names_the_pc_and_where_it_is() {
        val state = GestoUiState(
            addPcStep = AddPcStep.Invite,
            selectedHost = invite.toDiscoveredHost(),
            inviteEndpoints = invite.dialedEndpoints(),
        ).toAddPcState()
        assertEquals(AddPcStep.Invite, state.step)
        assertEquals("Legion", state.inviteName)
        assertEquals(listOf(InviteEndpointState("192.168.0.22", internet = false)), state.inviteEndpoints)
    }

    @Test fun an_invite_without_a_name_is_named_by_its_address() {
        val anonymous = GestoInvite.parse("gesto://invite?address=203.0.113.44&port=50000&code=123456")
        val state = GestoUiState(
            addPcStep = AddPcStep.Invite,
            selectedHost = anonymous.toDiscoveredHost(),
            inviteEndpoints = anonymous.dialedEndpoints(),
        ).toAddPcState()
        assertEquals("203.0.113.44", state.inviteName)
        assertEquals(listOf(InviteEndpointState("203.0.113.44:50000", internet = true)), state.inviteEndpoints)
    }

    @Test fun other_steps_show_no_invite() {
        val state = GestoUiState(addPcStep = AddPcStep.Code, selectedHost = invite.toDiscoveredHost()).toAddPcState()
        assertEquals("", state.inviteName)
        assertEquals(emptyList<InviteEndpointState>(), state.inviteEndpoints)
    }

    @Test fun the_confirmation_lists_every_endpoint_that_pairing_dials() {
        val both = GestoInvite.parse("gesto://invite?host=Legion&lan_address=192.168.0.22&remote_address=203.0.113.44&code=123456")
        val shown = GestoUiState(
            addPcStep = AddPcStep.Invite,
            selectedHost = both.toDiscoveredHost(),
            inviteEndpoints = both.dialedEndpoints(),
        ).toAddPcState().inviteEndpoints.map { it.where }
        val dialed = both.toCandidateHosts().map { inviteWhere(it.address, it.port) }
        assertEquals(dialed, shown)
        assertEquals(setOf("192.168.0.22", "203.0.113.44"), shown.toSet())
        assertEquals(listOf(true, false), GestoUiState(addPcStep = AddPcStep.Invite, inviteEndpoints = both.dialedEndpoints()).toAddPcState().inviteEndpoints.map { it.internet })
    }

    @Test fun an_ipv6_address_keeps_its_port_apart() {
        assertEquals("fe80::1", inviteWhere("fe80::1", 47771))
        assertEquals("[fe80::1]:50000", inviteWhere("fe80::1", 50000))
    }
}
