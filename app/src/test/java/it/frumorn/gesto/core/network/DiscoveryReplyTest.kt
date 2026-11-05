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
}
