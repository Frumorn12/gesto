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
        assertEquals("b", matchResponse("b", pending, reader(ok("a"), ok("b"))).getString("request_id"))
        assertTrue(pending.isEmpty())
    }

    @Test fun unknownIdIsAProtocolError() {
        val r = runCatching { matchResponse("b", mutableListOf(), reader(ok("x-unknown"))) }
        assertTrue(r.exceptionOrNull() is IllegalStateException)
    }

    @Test fun errorAnswerThrowsApiException() {
        val err = JSONObject().put("type", "response").put("request_id", "b").put("ok", false)
            .put("error", JSONObject().put("code", "c").put("message", "m"))
        val e = runCatching { matchResponse("b", mutableListOf(), reader(err)) }.exceptionOrNull()
        assertEquals("c", (e as GestoApiException).code)
    }
}
