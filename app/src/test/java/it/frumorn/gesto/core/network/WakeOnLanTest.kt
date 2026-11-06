package it.frumorn.gesto.core.network

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress

class WakeOnLanTest {
    private val mac = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0x01)

    @Test fun magic_packet_is_six_ff_then_sixteen_copies_of_the_mac() {
        val packet = WakeOnLan.magicPacket("AA:BB:CC:DD:EE:01")
        assertEquals(102, packet.size)
        assertArrayEquals(ByteArray(6) { 0xFF.toByte() }, packet.copyOfRange(0, 6))
        assertArrayEquals(mac, packet.copyOfRange(6, 12))
        assertArrayEquals(mac, packet.copyOfRange(96, 102))
        for (copy in 0 until 16) {
            assertArrayEquals(mac, packet.copyOfRange(6 + copy * 6, 12 + copy * 6))
        }
    }

    @Test fun dashes_and_lower_case_give_the_same_packet() {
        assertArrayEquals(WakeOnLan.magicPacket("AA:BB:CC:DD:EE:01"), WakeOnLan.magicPacket("aa-bb-cc-dd-ee-01"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun non_hex_digits_are_refused() {
        WakeOnLan.magicPacket("zz:bb:cc:dd:ee:01")
    }

    @Test(expected = IllegalArgumentException::class)
    fun a_short_address_is_refused() {
        WakeOnLan.magicPacket("aa:bb:cc")
    }

    @Test(expected = IllegalArgumentException::class)
    fun single_digit_groups_are_refused() {
        WakeOnLan.magicPacket("a:bb:cc:dd:ee:01")
    }

    @Test fun send_delivers_the_magic_packet_to_each_port() {
        DatagramSocket(0, InetAddress.getLoopbackAddress()).use { receiver ->
            receiver.soTimeout = 2_000
            runBlocking {
                WakeOnLan.send("aa:bb:cc:dd:ee:01", broadcast = listOf("127.0.0.1"), ports = listOf(receiver.localPort))
            }
            val buffer = ByteArray(256)
            val packet = DatagramPacket(buffer, buffer.size)
            receiver.receive(packet)
            assertArrayEquals(WakeOnLan.magicPacket("aa:bb:cc:dd:ee:01"), buffer.copyOf(packet.length))
        }
    }
}
