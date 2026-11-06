package it.frumorn.gesto.core.network

import it.frumorn.gesto.core.model.RemoteApp
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CapabilityParsingTest {
    private fun parse(json: String) = JSONObject(json).toCapabilitySummary()

    @Test
    fun parses_the_apps_the_pc_offers() {
        val caps = parse("""{"apps":[{"id":"steam","name":"Steam Big Picture"}]}""")
        assertEquals(listOf(RemoteApp("steam", "Steam Big Picture")), caps.apps)
    }

    @Test
    fun no_apps_key_means_an_empty_list() {
        assertTrue(parse("""{"system":{"media":true}}""").apps.isEmpty())
    }

    @Test
    fun an_empty_apps_list_stays_empty() {
        assertTrue(parse("""{"apps":[]}""").apps.isEmpty())
    }

    @Test
    fun an_app_without_an_id_is_dropped() {
        val caps = parse("""{"apps":[{"name":"Nameless"},{"id":"","name":"Blank"},{"id":"kodi","name":"Kodi"}]}""")
        assertEquals(listOf(RemoteApp("kodi", "Kodi")), caps.apps)
    }

    @Test
    fun an_app_without_a_name_is_shown_by_its_id() {
        assertEquals(listOf(RemoteApp("kodi", "kodi")), parse("""{"apps":[{"id":"kodi"}]}""").apps)
    }
}
