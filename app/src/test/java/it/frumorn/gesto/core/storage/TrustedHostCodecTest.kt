package it.frumorn.gesto.core.storage

import it.frumorn.gesto.core.model.TrustedHost
import it.frumorn.gesto.core.model.label
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrustedHostCodecTest {
    private val host = TrustedHost(
        id = "192.168.0.22:47771",
        hostName = "frumorn",
        address = "192.168.0.22",
        port = 47771,
        fingerprint = "SHA256:ab",
        deviceId = "device-1",
        sessionToken = "token-1",
        lastConnectedAt = 1_762_400_000_000L,
        displayName = "Desk PC",
        macAddresses = listOf("aa:bb:cc:dd:ee:01", "aa:bb:cc:dd:ee:02"),
    )

    @Test fun round_trip_keeps_every_field() {
        assertEquals(listOf(host), TrustedHostCodec.decode(TrustedHostCodec.encode(listOf(host))))
    }

    @Test fun round_trip_keeps_a_missing_display_name_missing() {
        val plain = host.copy(displayName = null, macAddresses = emptyList())
        val decoded = TrustedHostCodec.decode(TrustedHostCodec.encode(listOf(plain))).single()
        assertNull(decoded.displayName)
        assertTrue(decoded.macAddresses.isEmpty())
        assertEquals("frumorn", decoded.label)
    }

    @Test fun json_from_before_the_new_fields_still_loads() {
        val old = """[{"id":"192.168.0.22:47771","hostName":"frumorn","address":"192.168.0.22","port":47771,""" +
            """"fingerprint":"SHA256:ab","deviceId":"device-1","sessionToken":"token-1","lastConnectedAt":5}]"""
        val decoded = TrustedHostCodec.decode(old).single()
        assertNull(decoded.displayName)
        assertEquals(emptyList<String>(), decoded.macAddresses)
        assertEquals(5L, decoded.lastConnectedAt)
        assertEquals("frumorn", decoded.hostName)
    }

    @Test fun missing_last_connected_at_is_zero() {
        val old = """[{"id":"x","hostName":"h","address":"10.0.0.2","port":1,"fingerprint":"f",""" +
            """"deviceId":"d","sessionToken":"t"}]"""
        assertEquals(0L, TrustedHostCodec.decode(old).single().lastConnectedAt)
    }

    @Test fun label_prefers_the_name_the_user_chose() {
        assertEquals("Desk PC", host.label)
    }

    @Test fun upsert_keeps_the_order_of_the_list() {
        val a = host.copy(id = "a", fingerprint = "fa")
        val b = host.copy(id = "b", fingerprint = "fb")
        val c = host.copy(id = "c", fingerprint = "fc")
        val newB = b.copy(lastConnectedAt = 99)
        assertEquals(listOf(a, newB, c), upsertInPlace(listOf(a, b, c), newB))
        // Same PC paired again under a new address: it takes the old entry's place.
        val repaired = b.copy(id = "b2")
        assertEquals(listOf(a, repaired, c), upsertInPlace(listOf(a, b, c), repaired))
        val d = host.copy(id = "d", fingerprint = "fd")
        assertEquals(listOf(a, b, c, d), upsertInPlace(listOf(a, b, c), d))
    }

    @Test fun empty_list_round_trips() {
        assertEquals(emptyList<TrustedHost>(), TrustedHostCodec.decode(TrustedHostCodec.encode(emptyList())))
    }
}
