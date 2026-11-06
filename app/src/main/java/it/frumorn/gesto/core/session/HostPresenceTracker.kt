package it.frumorn.gesto.core.session

import it.frumorn.gesto.core.model.TrustedHost

/** Whether a paired PC answers on this network. */
enum class Presence { Unknown, Online, Offline }

/**
 * Turns network scans into a steady presence per PC, so a reply lost now and then does not make a
 * card blink: a PC is Online as soon as its fingerprint shows up in a scan, and Offline only after
 * [missesForOffline] scans in a row without it. Until then a PC never seen keeps Unknown.
 */
class HostPresenceTracker(private val missesForOffline: Int = 3) {
    /** Consecutive scans without the PC, by host id; no entry = not scanned yet. */
    private val misses = mutableMapOf<String, Int>()
    private val everSeen = mutableSetOf<String>()

    /** Records one scan that found [seenFingerprints]; returns the presence of each of [hosts]. */
    fun onScan(seenFingerprints: Set<String>, hosts: List<TrustedHost>): Map<String, Presence> {
        forgetMissing(hosts)
        hosts.forEach { host ->
            if (host.fingerprint.isNotBlank() && host.fingerprint in seenFingerprints) {
                misses[host.id] = 0
                everSeen += host.id
            } else {
                misses[host.id] = (misses[host.id] ?: 0) + 1
            }
        }
        return presence(hosts)
    }

    /** The presence of each of [hosts] after the scans so far, without a new scan. */
    fun presence(hosts: List<TrustedHost>): Map<String, Presence> = hosts.associate { host ->
        val missed = misses[host.id]
        host.id to when {
            missed == null -> Presence.Unknown
            missed >= missesForOffline -> Presence.Offline
            host.id in everSeen -> Presence.Online
            else -> Presence.Unknown
        }
    }

    /** A PC that was forgotten starts over if it is paired again. */
    private fun forgetMissing(hosts: List<TrustedHost>) {
        val ids = hosts.mapTo(HashSet()) { it.id }
        misses.keys.retainAll(ids)
        everSeen.retainAll(ids)
    }
}
