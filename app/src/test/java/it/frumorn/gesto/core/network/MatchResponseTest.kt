package it.frumorn.gesto.core.network

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchResponseTest {
    private fun ok(id: String) = JSONObject().put("type", "response").put("request_id", id).put("ok", true)
    private fun reader(vararg msgs: JSONObject): () -> JSONObject { val it = msgs.iterator(); return { it.next() } }

    @Test fun skipsStaleResponseOfEarlierRequest() {
        val pending = mutableListOf("a")
        assertEquals("b", matchResponse("b", pending, next = reader(ok("a"), ok("b"))).getString("request_id"))
        assertTrue(pending.isEmpty())
    }

    @Test fun unknownIdIsAProtocolError() {
        val r = runCatching { matchResponse("b", mutableListOf(), next = reader(ok("x-unknown"))) }
        assertTrue(r.exceptionOrNull() is IllegalStateException)
    }

    @Test fun errorAnswerThrowsApiException() {
        val err = JSONObject().put("type", "response").put("request_id", "b").put("ok", false)
            .put("error", JSONObject().put("code", "c").put("message", "m"))
        val e = runCatching { matchResponse("b", mutableListOf(), next = reader(err)) }.exceptionOrNull()
        assertEquals("c", (e as GestoApiException).code)
    }

    private fun refused(id: String) = JSONObject().put("type", "response").put("request_id", id).put("ok", false)
        .put("error", JSONObject().put("code", "command_failed").put("message", "Input injection requires portal approval on this session"))

    @Test fun anEarlierOneWayRefusalIsReportedApartAndTheStreamStaysInStep() {
        val pending = mutableListOf("move-1", "move-2")
        val reported = mutableListOf<String>()
        val stream = reader(refused("move-1"), ok("move-2"), ok("ping"), ok("text"))
        val pong = matchResponse("ping", pending, onEarlierError = { reported += it.code }, next = stream)
        assertEquals("ping", pong.getString("request_id"))
        assertEquals(listOf("command_failed"), reported)
        assertTrue(pending.isEmpty())
        // The next request reads its own answer, not the one before it.
        assertEquals("text", matchResponse("text", pending, next = stream).getString("request_id"))
    }
}
