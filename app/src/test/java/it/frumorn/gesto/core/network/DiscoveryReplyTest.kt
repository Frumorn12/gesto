package it.frumorn.gesto.core.network

import it.frumorn.gesto.core.model.DiscoveredHost
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

class DiscoveryReplyTest {
    private fun reply(service: String, extra: String = "") = JSONObject(
        """{"service":"$service","host_name":"legion","control_port":47771,"host_fingerprint":"ab",""" +
            """"input_supported":true,"input_backend":"portal","capture_supported":true,""" +
            """"capture_backend":"pipewire"$extra}""",
    )

    @Test
    fun parses_gesto_reply_with_approval_pairing() {
        val host = parseDiscoveryReply(reply("gesto", ""","approval_pairing":true"""), "192.168.0.22")
        assertEquals(
            DiscoveredHost(
                hostName = "legion",
                address = "192.168.0.22",
                port = 47771,
                fingerprint = "ab",
                inputSupported = true,
                inputBackend = "portal",
                captureSupported = true,
                captureBackend = "pipewire",
                approvalPairing = true,
            ),
            host,
        )
    }

    @Test
    fun parses_legacy_daemon_reply_without_approval_pairing() {
        val host = parseDiscoveryReply(reply("dev.waypad.daemon"), "192.168.0.22")
        assertFalse(host!!.approvalPairing)
        assertEquals("legion", host.hostName)
    }

    @Test
    fun ignores_unknown_service() {
        assertNull(parseDiscoveryReply(reply("other"), "192.168.0.22"))
    }

    @Test
    fun both_probes_are_sent() {
        assertEquals(listOf("GESTO_DISCOVER_V1", "WAYPAD_DISCOVER_V1"), DISCOVERY_PROBES)
    }

    private fun found(address: String, fingerprint: String, port: Int = 47771) =
        parseDiscoveryReply(reply("gesto"), address)!!.copy(address = address, port = port, fingerprint = fingerprint)

    @Test
    fun one_pc_answering_twice_or_on_two_addresses_is_listed_once() {
        val hosts = dedupeDiscoveredHosts(
            listOf(found("192.168.0.22", "fp-1"), found("192.168.0.22", "fp-1"), found("10.0.0.4", "fp-1"), found("192.168.0.30", "fp-2")),
        )
        assertEquals(listOf("192.168.0.22" to "fp-1", "192.168.0.30" to "fp-2"), hosts.map { it.address to it.fingerprint })
    }

    @Test
    fun sightings_keep_the_last_reply_of_each_pc_even_when_a_scan_misses_it() {
        val old = listOf(found("192.168.0.22", "fp-1"), found("192.168.0.30", "fp-2"))
        val merged = mergeSightings(old, listOf(found("192.168.0.99", "fp-1")))
        assertEquals(listOf("192.168.0.99" to "fp-1", "192.168.0.30" to "fp-2"), merged.map { it.address to it.fingerprint })
    }

    @Test
    fun without_a_fingerprint_the_address_and_port_tell_hosts_apart() {
        val hosts = dedupeDiscoveredHosts(
            listOf(found("192.168.0.22", ""), found("192.168.0.22", ""), found("192.168.0.22", "", port = 47772)),
        )
        assertEquals(listOf(47771, 47772), hosts.map { it.port })
    }
}
