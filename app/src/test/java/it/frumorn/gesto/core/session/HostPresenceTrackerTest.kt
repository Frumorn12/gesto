package it.frumorn.gesto.core.session

import it.frumorn.gesto.core.model.TrustedHost
import org.junit.Assert.assertEquals
import org.junit.Test

class HostPresenceTrackerTest {
    private fun host(id: String) = TrustedHost(
        id = id,
        hostName = "pc-$id",
        address = "192.168.0.22",
        port = 47771,
        fingerprint = "fp-$id",
        deviceId = "device",
        sessionToken = "token",
        lastConnectedAt = 0L,
    )

    private val a = host("a")
    private val b = host("b")

    @Test fun seen_in_the_first_scan_is_online() {
        val tracker = HostPresenceTracker()
        assertEquals(Presence.Online, tracker.onScan(setOf("fp-a"), listOf(a))["a"])
    }

    @Test fun offline_only_after_three_scans_in_a_row_without_it() {
        val tracker = HostPresenceTracker()
        tracker.onScan(setOf("fp-a"), listOf(a))
        assertEquals(Presence.Online, tracker.onScan(emptySet(), listOf(a))["a"])
        assertEquals(Presence.Online, tracker.onScan(emptySet(), listOf(a))["a"])
        assertEquals(Presence.Offline, tracker.onScan(emptySet(), listOf(a))["a"])
    }

    @Test fun seen_again_is_online_at_once() {
        val tracker = HostPresenceTracker()
        tracker.onScan(setOf("fp-a"), listOf(a))
        repeat(3) { tracker.onScan(emptySet(), listOf(a)) }
        assertEquals(Presence.Online, tracker.onScan(setOf("fp-a"), listOf(a))["a"])
    }

    @Test fun a_miss_between_sightings_starts_the_count_again() {
        val tracker = HostPresenceTracker()
        tracker.onScan(setOf("fp-a"), listOf(a))
        tracker.onScan(emptySet(), listOf(a))
        tracker.onScan(emptySet(), listOf(a))
        tracker.onScan(setOf("fp-a"), listOf(a))
        tracker.onScan(emptySet(), listOf(a))
        assertEquals(Presence.Online, tracker.onScan(emptySet(), listOf(a))["a"])
    }

    @Test fun never_scanned_is_unknown() {
        val tracker = HostPresenceTracker()
        assertEquals(Presence.Unknown, tracker.presence(listOf(a))["a"])
        tracker.onScan(setOf("fp-a"), listOf(a))
        // b was paired after that scan: nothing is known about it yet.
        assertEquals(Presence.Unknown, tracker.presence(listOf(a, b))["b"])
        assertEquals(Presence.Online, tracker.presence(listOf(a, b))["a"])
    }

    @Test fun never_seen_stays_unknown_until_it_counts_as_offline() {
        val tracker = HostPresenceTracker()
        assertEquals(Presence.Unknown, tracker.onScan(emptySet(), listOf(b))["b"])
        assertEquals(Presence.Unknown, tracker.onScan(emptySet(), listOf(b))["b"])
        assertEquals(Presence.Offline, tracker.onScan(emptySet(), listOf(b))["b"])
    }

    @Test fun the_threshold_is_configurable() {
        val tracker = HostPresenceTracker(missesForOffline = 1)
        tracker.onScan(setOf("fp-a"), listOf(a))
        assertEquals(Presence.Offline, tracker.onScan(emptySet(), listOf(a))["a"])
    }

    @Test fun a_forgotten_pc_leaves_the_result_and_starts_over_if_paired_again() {
        val tracker = HostPresenceTracker()
        tracker.onScan(setOf("fp-a", "fp-b"), listOf(a, b))
        val result = tracker.onScan(setOf("fp-a"), listOf(a))
        assertEquals(setOf("a"), result.keys)
        assertEquals(Presence.Unknown, tracker.presence(listOf(a, b))["b"])
    }

    @Test fun a_pc_without_a_fingerprint_is_never_matched() {
        val tracker = HostPresenceTracker()
        val blank = a.copy(fingerprint = "")
        assertEquals(Presence.Unknown, tracker.onScan(setOf(""), listOf(blank))["a"])
    }
}
