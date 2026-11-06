package it.frumorn.gesto.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

/** Wake-on-LAN: the "magic packet" that switches on a PC whose network card is set to listen for it. */
object WakeOnLan {
    private const val MAC_BYTES = 6
    private const val REPEATS = 16

    /** 6 bytes 0xFF, then the 6 bytes of [mac] 16 times (102 bytes). [mac] uses `:` or `-`, any case. */
    fun magicPacket(mac: String): ByteArray {
        val bytes = parseMac(mac)
        val packet = ByteArray(MAC_BYTES + REPEATS * MAC_BYTES)
        packet.fill(0xFF.toByte(), 0, MAC_BYTES)
        repeat(REPEATS) { copy -> bytes.copyInto(packet, MAC_BYTES + copy * MAC_BYTES) }
        return packet
    }

    /**
     * Sends the magic packet for [mac] to every address in [broadcast] on every port in [ports].
     * Throws when the address is not valid or nothing could be sent at all.
     */
    suspend fun send(
        mac: String,
        broadcast: List<String> = listOf("255.255.255.255"),
        ports: List<Int> = listOf(9, 7),
    ) = withContext(Dispatchers.IO) {
        val payload = magicPacket(mac)
        var sent = 0
        var lastError: Exception? = null
        DatagramSocket().use { socket ->
            socket.broadcast = true
            for (target in broadcast.distinct()) {
                val address = runCatching { InetAddress.getByName(target) }.getOrElse {
                    lastError = it as? Exception
                    null
                } ?: continue
                for (port in ports) {
                    try {
                        socket.send(DatagramPacket(payload, payload.size, address, port))
                        sent++
                    } catch (e: java.io.IOException) {
                        // One unreachable subnet must not stop the others.
                        lastError = e
                    }
                }
            }
        }
        if (sent == 0) throw lastError ?: IllegalStateException("No wake-up target")
    }

    private fun parseMac(mac: String): ByteArray {
        val groups = mac.trim().split(':', '-')
        require(groups.size == MAC_BYTES && groups.all { it.length == 2 && it.all(::isHexDigit) }) {
            "Not a MAC address: $mac"
        }
        return ByteArray(MAC_BYTES) { index -> groups[index].toInt(16).toByte() }
    }

    private fun isHexDigit(c: Char): Boolean = c in '0'..'9' || c.lowercaseChar() in 'a'..'f'
}
