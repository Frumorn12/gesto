package it.frumorn.gesto.ui

import it.frumorn.gesto.AddPcStep
import it.frumorn.gesto.GestoUiState
import it.frumorn.gesto.core.network.GestoInvite
import org.junit.Assert.assertEquals
import org.junit.Test

class AddPcStateTest {
    private val invite = GestoInvite.parse("gesto://invite?host=Legion&address=192.168.0.22&code=123456&expires=1770000000")

    @Test fun the_confirmation_names_the_pc_and_where_it_is() {
        val state = GestoUiState(addPcStep = AddPcStep.Invite, selectedHost = invite.toDiscoveredHost()).toAddPcState()
        assertEquals(AddPcStep.Invite, state.step)
        assertEquals("Legion", state.inviteName)
        assertEquals("192.168.0.22", state.inviteAddress)
    }

    @Test fun an_invite_without_a_name_is_named_by_its_address() {
        val anonymous = GestoInvite.parse("gesto://invite?address=203.0.113.44&port=50000&code=123456")
        val state = GestoUiState(addPcStep = AddPcStep.Invite, selectedHost = anonymous.toDiscoveredHost()).toAddPcState()
        assertEquals("203.0.113.44", state.inviteName)
        assertEquals("203.0.113.44:50000", state.inviteAddress)
    }

    @Test fun other_steps_show_no_invite() {
        val state = GestoUiState(addPcStep = AddPcStep.Code, selectedHost = invite.toDiscoveredHost()).toAddPcState()
        assertEquals("", state.inviteName)
        assertEquals("", state.inviteAddress)
    }

    @Test fun an_ipv6_address_keeps_its_port_apart() {
        assertEquals("fe80::1", inviteWhere("fe80::1", 47771))
        assertEquals("[fe80::1]:50000", inviteWhere("fe80::1", 50000))
    }
}
