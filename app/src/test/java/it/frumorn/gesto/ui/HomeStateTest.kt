package it.frumorn.gesto.ui

import it.frumorn.gesto.GestoUiState
import it.frumorn.gesto.R
import it.frumorn.gesto.core.model.CapabilitySummary
import it.frumorn.gesto.core.model.DiscoveredHost
import it.frumorn.gesto.core.model.TrustedHost
import it.frumorn.gesto.core.session.Presence
import it.frumorn.gesto.ui.text.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class HomeStateTest {
    private fun host(id: String, macs: List<String> = emptyList(), name: String? = null) = TrustedHost(
        id = id,
        hostName = "pc-$id",
        address = "192.168.0.22",
        port = 47771,
        fingerprint = "fp-$id",
        deviceId = "device",
        sessionToken = "token",
        lastConnectedAt = 0L,
        displayName = name,
        macAddresses = macs,
    )

    private fun found(fingerprint: String, capture: Boolean) = DiscoveredHost(
        hostName = "x",
        address = "192.168.0.22",
        port = 47771,
        fingerprint = fingerprint,
        inputSupported = true,
        inputBackend = "portal",
        captureSupported = capture,
    )

    @Test fun the_connected_pc_is_online_whatever_the_scans_say() {
        val a = host("a")
        val card = GestoUiState(
            trustedHosts = listOf(a),
            connectedHost = a,
            presence = mapOf("a" to Presence.Offline),
        ).toHomeState().pcs.single()
        assertEquals(Presence.Online, card.presence)
        assertTrue(card.connected)
        assertFalse(card.canWake)
    }

    @Test fun wake_is_offered_only_offline_and_with_a_mac() {
        val withMac = host("a", macs = listOf("aa:bb:cc:dd:ee:01"))
        val noMac = host("b")
        val onlineWithMac = host("c", macs = listOf("aa:bb:cc:dd:ee:02"))
        val cards = GestoUiState(
            trustedHosts = listOf(withMac, noMac, onlineWithMac),
            presence = mapOf("a" to Presence.Offline, "b" to Presence.Offline, "c" to Presence.Online),
        ).toHomeState().pcs.associateBy { it.id }
        assertTrue(cards.getValue("a").canWake)
        assertFalse(cards.getValue("b").canWake)
        assertFalse(cards.getValue("c").canWake)
    }

    @Test fun a_pc_not_scanned_yet_is_unknown_and_named_by_its_label() {
        val card = GestoUiState(trustedHosts = listOf(host("a", name = "Desk"))).toHomeState().pcs.single()
        assertEquals(Presence.Unknown, card.presence)
        assertEquals("Desk", card.name)
    }

    @Test fun chips_come_from_the_scan_and_from_the_session() {
        assertEquals(listOf(UiText.Res(R.string.home_chip_screen)), pcChips(found("fp", capture = true), null))
        assertEquals(emptyList<UiText>(), pcChips(found("fp", capture = false), null))
        assertEquals(emptyList<UiText>(), pcChips(null, null))
        assertEquals(
            listOf(
                UiText.Res(R.string.home_chip_screen),
                UiText.Res(R.string.home_chip_audio),
                UiText.Res(R.string.home_chip_controller),
            ),
            pcChips(
                found("fp", capture = false),
                CapabilitySummary(captureSupported = true, audioSupported = true, externalControllerSupported = true),
            ),
        )
    }

    @Test fun an_offline_pc_shows_no_chips() {
        val a = host("a")
        val card = GestoUiState(
            trustedHosts = listOf(a),
            presence = mapOf("a" to Presence.Offline),
            scannedHosts = listOf(found("fp-a", capture = true)),
        ).toHomeState().pcs.single()
        assertEquals(emptyList<UiText>(), card.chips)
    }

    @Test fun the_scan_is_matched_by_fingerprint() {
        val a = host("a")
        val card = GestoUiState(
            trustedHosts = listOf(a),
            presence = mapOf("a" to Presence.Online),
            scannedHosts = listOf(found("fp-other", capture = true), found("fp-a", capture = false)),
        ).toHomeState().pcs.single()
        assertEquals(emptyList<UiText>(), card.chips)
    }

    @Test fun a_pc_that_no_longer_knows_the_phone_offers_to_be_added_again() {
        val a = host("a").copy(needsRepair = true)
        val b = host("b")
        val cards = GestoUiState(trustedHosts = listOf(a, b)).toHomeState().pcs
        assertTrue(cards[0].needsRepair)
        assertFalse(cards[1].needsRepair)
    }
}
