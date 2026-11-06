package it.frumorn.gesto.core.network

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class MacAddressesTest {
    @Test fun reads_the_addresses_the_daemon_reports() {
        val data = JSONObject("""{"mac_addresses":["aa:bb:cc:dd:ee:01","AA:BB:CC:DD:EE:02"]}""")
        assertEquals(listOf("aa:bb:cc:dd:ee:01", "aa:bb:cc:dd:ee:02"), data.macAddresses())
    }

    @Test fun an_old_daemon_reports_none() {
        assertEquals(emptyList<String>(), JSONObject("""{"authenticated":true}""").macAddresses())
    }

    @Test fun malformed_and_repeated_entries_are_dropped() {
        val data = JSONObject("""{"mac_addresses":["aa:bb:cc:dd:ee:01","nonsense",7,"aa:bb:cc:dd:ee:01"]}""")
        assertEquals(listOf("aa:bb:cc:dd:ee:01"), data.macAddresses())
    }
}
