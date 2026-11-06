package it.frumorn.gesto.core.session

import it.frumorn.gesto.core.model.TrustedHost
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class LaunchDecisionTest {
    private fun host(id: String, lastConnectedAt: Long) = TrustedHost(
        id = id,
        hostName = "pc-$id",
        address = "192.168.0.22",
        port = 47771,
        fingerprint = "fp-$id",
        deviceId = "device",
        sessionToken = "token",
        lastConnectedAt = lastConnectedAt,
    )

    @Test fun no_hosts_means_no_target() {
        assertNull(launchTarget(emptyList()))
    }

    @Test fun picks_the_most_recently_connected() {
        val a = host("a", 10)
        val b = host("b", 30)
        val c = host("c", 20)
        assertSame(b, launchTarget(listOf(a, b, c)))
    }

    @Test fun ties_keep_the_first_in_the_list() {
        val a = host("a", 30)
        val b = host("b", 30)
        assertSame(a, launchTarget(listOf(a, b)))
    }

    @Test fun private_addresses_are_the_home_network() {
        for (address in listOf("192.168.0.22", "10.0.0.5", "172.16.1.2", "172.31.255.1", "169.254.3.4", "fe80::1", "fd12::3")) {
            assertTrue(address, isLocalNetworkAddress(address))
        }
    }

    @Test fun public_addresses_and_names_are_not() {
        for (address in listOf("8.8.8.8", "172.32.0.1", "100.64.0.1", "my-pc.example.org", "2001:db8::1", "")) {
            assertFalse(address, isLocalNetworkAddress(address))
        }
    }
}
