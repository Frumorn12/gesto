package it.frumorn.gesto.core.session

import it.frumorn.gesto.core.model.DiscoveredHost
import it.frumorn.gesto.core.model.TrustedHost

/**
 * The paired PCs that a network scan ([seen], its replies) found at another address or port,
 * each with the address it answered from. A PC is recognised by its fingerprint, never by its
 * address, and the handshake checks that fingerprint again on connecting, so following it is safe.
 * A PC that also answered at its stored address keeps it (two network cards); one seen at several
 * other addresses takes the first reply.
 */
fun movedHosts(hosts: List<TrustedHost>, seen: List<DiscoveredHost>): List<TrustedHost> =
    hosts.mapNotNull { host ->
        if (host.fingerprint.isBlank()) return@mapNotNull null
        val sightings = seen.filter { it.fingerprint == host.fingerprint }
        val stillThere = sightings.any { it.address == host.address && it.port == host.port }
        val at = sightings.firstOrNull()
        if (at == null || stillThere) null else host.copy(address = at.address, port = at.port)
    }

/**
 * Where to dial [host], in order: the address the latest scan saw it at ([seen]) when that differs
 * from the stored one, then the stored one.
 */
fun connectAddresses(host: TrustedHost, seen: List<DiscoveredHost>): List<TrustedHost> {
    val latest = seen.firstOrNull { host.fingerprint.isNotBlank() && it.fingerprint == host.fingerprint }
    if (latest == null || (latest.address == host.address && latest.port == host.port)) return listOf(host)
    return listOf(host.copy(address = latest.address, port = latest.port), host)
}
