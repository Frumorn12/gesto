package it.frumorn.gesto.core.session

import it.frumorn.gesto.core.model.DiscoveredHost
import it.frumorn.gesto.core.model.TrustedHost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HostAddressesTest {
    private val pc = TrustedHost(
        id = "192.168.0.184:47771",
        hostName = "frumorn",
        address = "192.168.0.184",
        port = 47771,
        fingerprint = "fp-pc",
        deviceId = "device",
        sessionToken = "token",
        lastConnectedAt = 0L,
        displayName = "Desk PC",
    )

    private fun reply(address: String, port: Int = 47771, fingerprint: String = "fp-pc") = DiscoveredHost(
        hostName = "frumorn",
        address = address,
        port = port,
        fingerprint = fingerprint,
        inputSupported = true,
        inputBackend = "uinput",
    )

    @Test fun a_pc_seen_at_a_new_address_moves_there() {
        val moved = movedHosts(listOf(pc), listOf(reply("192.168.0.22"))).single()
        assertEquals("192.168.0.22", moved.address)
        assertEquals(47771, moved.port)
        // Everything else, the pairing above all, stays as it was.
        assertEquals(pc.copy(address = "192.168.0.22"), moved)
    }

    @Test fun a_pc_seen_on_a_new_port_moves_too() {
        assertEquals(47800, movedHosts(listOf(pc), listOf(reply("192.168.0.184", port = 47800))).single().port)
    }

    @Test fun a_pc_seen_where_it_is_stored_does_not_move() {
        assertTrue(movedHosts(listOf(pc), listOf(reply("192.168.0.184"))).isEmpty())
    }

    @Test fun a_pc_that_also_answers_at_its_stored_address_keeps_it() {
        // Two network cards: the wired one stored, the Wi-Fi one answering too.
        assertTrue(movedHosts(listOf(pc), listOf(reply("192.168.0.40"), reply("192.168.0.184"))).isEmpty())
    }

    @Test fun another_pc_at_the_address_is_not_this_one() {
        assertTrue(movedHosts(listOf(pc), listOf(reply("192.168.0.22", fingerprint = "fp-other"))).isEmpty())
    }

    @Test fun a_pc_without_a_fingerprint_is_never_matched() {
        val unpinned = pc.copy(fingerprint = "")
        assertTrue(movedHosts(listOf(unpinned), listOf(reply("192.168.0.22", fingerprint = ""))).isEmpty())
    }

    @Test fun connecting_tries_the_scanned_address_first_then_the_stored_one() {
        val order = connectAddresses(pc, listOf(reply("192.168.0.22"))).map { "${it.address}:${it.port}" }
        assertEquals(listOf("192.168.0.22:47771", "192.168.0.184:47771"), order)
    }

    @Test fun connecting_without_a_newer_sighting_dials_the_stored_address() {
        assertEquals(listOf(pc), connectAddresses(pc, emptyList()))
        assertEquals(listOf(pc), connectAddresses(pc, listOf(reply("192.168.0.184"))))
        assertEquals(listOf(pc), connectAddresses(pc, listOf(reply("192.168.0.22", fingerprint = "fp-other"))))
    }
}
