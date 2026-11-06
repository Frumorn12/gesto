package it.frumorn.gesto.core.session

import it.frumorn.gesto.core.model.TrustedHost

/**
 * The PC to reconnect to when the app opens: the one used most recently (the first of the list on
 * a tie), null when no PC is paired. A PC that no longer knows this phone is skipped: it would
 * only refuse.
 */
fun launchTarget(hosts: List<TrustedHost>): TrustedHost? =
    hosts.filterNot { it.needsRepair }.maxByOrNull { it.lastConnectedAt }

/**
 * True for an address on the home network: IPv4 private (10/8, 172.16/12, 192.168/16) or
 * link-local (169.254/16), IPv6 link-local (fe80::/10) or unique-local (fc00::/7). Names and
 * public or carrier-grade addresses (a VPN such as 100.64/10) are not.
 */
fun isLocalNetworkAddress(address: String): Boolean {
    val text = address.trim().removePrefix("[").removeSuffix("]")
    if (':' in text) {
        val head = text.substringBefore(':').toIntOrNull(16) ?: return false
        return head and 0xFFC0 == 0xFE80 || head and 0xFE00 == 0xFC00
    }
    val octets = text.split('.').map { it.toIntOrNull() ?: return false }
    if (octets.size != 4 || octets.any { it !in 0..255 }) return false
    val (a, b) = octets
    return a == 10 ||
        (a == 172 && b in 16..31) ||
        (a == 192 && b == 168) ||
        (a == 169 && b == 254)
}
