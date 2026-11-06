package it.frumorn.gesto.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkAddress
import android.net.wifi.WifiManager
import it.frumorn.gesto.core.model.DiscoveredHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress

private val ACCEPTED_SERVICES = setOf("gesto", "dev.waypad.daemon")

/**
 * Sent in every discovery round: the current probe, and the one from before the rename, which old
 * daemons still waiting for it answer (current daemons answer both).
 */
val DISCOVERY_PROBES = listOf("GESTO_DISCOVER_V1", "WAYPAD_DISCOVER_V1")

/** What tells one PC from another in discovery replies: its fingerprint, or address and port without one. */
val DiscoveredHost.identity: String
    get() = fingerprint.ifBlank { "$address:$port" }

/**
 * One entry per PC: a daemon answers each probe, and may answer on more than one address. PCs are
 * told apart by [identity]; the first reply wins.
 */
fun dedupeDiscoveredHosts(hosts: List<DiscoveredHost>): List<DiscoveredHost> = hosts.distinctBy { it.identity }

/**
 * The last reply of every PC seen so far: [found] (the latest scan) wins over [previous], and a PC
 * a scan missed keeps its earlier reply, so what the cards show does not blink with one lost reply.
 */
fun mergeSightings(previous: List<DiscoveredHost>, found: List<DiscoveredHost>): List<DiscoveredHost> =
    dedupeDiscoveredHosts(found + previous)

/** Parses a discovery reply; returns null when [json] does not come from a known daemon. */
fun parseDiscoveryReply(json: JSONObject, address: String): DiscoveredHost? {
    if (json.optString("service") !in ACCEPTED_SERVICES) return null
    return DiscoveredHost(
        hostName = json.optString("host_name", address),
        address = address,
        port = json.optInt("control_port", 47771),
        fingerprint = json.optString("host_fingerprint"),
        inputSupported = json.optBoolean("input_supported"),
        inputBackend = json.optString("input_backend", "unknown"),
        captureSupported = json.optBoolean("capture_supported"),
        captureBackend = json.optString("capture_backend", "unknown"),
        approvalPairing = json.optBoolean("approval_pairing", false),
    )
}

class GestoDiscovery(context: Context) {
    private val appContext = context.applicationContext

    suspend fun discover(timeoutMillis: Int = 1800): List<DiscoveredHost> = withContext(Dispatchers.IO) {
        val results = ArrayList<DiscoveredHost>()
        val wifiLock = multicastLock()
        wifiLock?.setReferenceCounted(false)
        wifiLock?.acquire()
        try {
            DatagramSocket().use { socket ->
                socket.broadcast = true
                socket.reuseAddress = true
                socket.soTimeout = 350
                val payloads = DISCOVERY_PROBES.map { it.toByteArray(Charsets.UTF_8) }
                val targets = discoveryTargets()
                repeat(3) {
                    targets.forEach { target ->
                        payloads.forEach { payload ->
                            socket.send(DatagramPacket(payload, payload.size, target, 47770))
                        }
                    }
                    Thread.sleep(120)
                }

                val deadline = System.currentTimeMillis() + timeoutMillis
                val buffer = ByteArray(4096)
                // isActive: a scan that is no longer wanted stops listening at the next timeout.
                while (isActive && System.currentTimeMillis() < deadline) {
                    try {
                        val reply = DatagramPacket(buffer, buffer.size)
                        socket.receive(reply)
                        val json = runCatching {
                            JSONObject(String(reply.data, 0, reply.length, Charsets.UTF_8))
                        }.getOrNull() ?: continue
                        val address = reply.address.hostAddress ?: continue
                        parseDiscoveryReply(json, address)?.let(results::add)
                    } catch (_: java.net.SocketTimeoutException) {
                        // Continue until the overall discovery window closes.
                    }
                }
            }
        } finally {
            if (wifiLock?.isHeld == true) wifiLock.release()
        }
        dedupeDiscoveredHosts(results)
    }

    /**
     * Where a Wake-on-LAN packet goes: the limited broadcast plus the broadcast address of every
     * IPv4 network the phone is on.
     */
    fun broadcastAddresses(): List<String> = discoveryTargets().mapNotNull { it.hostAddress }

    private fun multicastLock(): WifiManager.MulticastLock? {
        val wifi = appContext.getSystemService(Context.WIFI_SERVICE) as? WifiManager ?: return null
        return wifi.createMulticastLock("gesto-discovery")
    }

    private fun discoveryTargets(): Set<InetAddress> {
        val targets = linkedSetOf(InetAddress.getByName("255.255.255.255"))
        val connectivity =
            appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return targets
        connectivity.allNetworks
            .mapNotNull { network -> connectivity.getLinkProperties(network) }
            .flatMap { properties -> properties.linkAddresses }
            .mapNotNullTo(targets) { address -> address.broadcastAddress() }
        return targets
    }

    private fun LinkAddress.broadcastAddress(): InetAddress? {
        val ipv4 = address as? Inet4Address ?: return null
        val raw = ipv4.address
        val ip = ((raw[0].toInt() and 0xff) shl 24) or
            ((raw[1].toInt() and 0xff) shl 16) or
            ((raw[2].toInt() and 0xff) shl 8) or
            (raw[3].toInt() and 0xff)
        val mask = if (prefixLength == 0) 0 else -1 shl (32 - prefixLength)
        val broadcast = ip or mask.inv()
        val bytes = byteArrayOf(
            ((broadcast ushr 24) and 0xff).toByte(),
            ((broadcast ushr 16) and 0xff).toByte(),
            ((broadcast ushr 8) and 0xff).toByte(),
            (broadcast and 0xff).toByte(),
        )
        return InetAddress.getByAddress(bytes)
    }
}
