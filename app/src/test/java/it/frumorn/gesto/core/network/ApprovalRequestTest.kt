package it.frumorn.gesto.core.network

import java.io.EOFException
import java.net.SocketException
import java.net.SocketTimeoutException
import org.json.JSONException
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class ApprovalRequestTest {
    @Test fun the_request_has_exactly_the_five_keys() {
        val json = JSONObject(buildApprovalPairRequest("r1", "Nothing Phone (3a)", "4821", "0.2.0").toString())
        assertEquals(setOf("type", "request_id", "device_name", "pin", "app_version"), json.keys().asSequence().toSet())
        assertEquals("approval_pair_request", json.getString("type"))
        assertEquals("r1", json.getString("request_id"))
        assertEquals("Nothing Phone (3a)", json.getString("device_name"))
        assertEquals("4821", json.getString("pin"))
        assertEquals("0.2.0", json.getString("app_version"))
    }

    @Test fun a_pin_with_leading_zeros_stays_a_string() {
        val json = buildApprovalPairRequest("r2", "Phone", "0042", "0.2.0")
        assertEquals("0042", json.get("pin"))
    }

    // An old daemon does not know approval_pair_request: it drops the connection at once.

    @Test fun a_connection_closed_at_once_means_an_old_daemon() {
        val closed = IllegalStateException("Connection closed")
        val result = classifyApprovalFailure(closed, elapsedMs = 40)
        assertTrue(result is ApprovalNotSupportedException)
        assertTrue(result is EOFException)
        assertSame(closed, result.cause)
    }

    @Test fun a_reset_at_once_means_an_old_daemon() {
        assertTrue(classifyApprovalFailure(SocketException("Connection reset"), 200) is ApprovalNotSupportedException)
    }

    @Test fun an_unreadable_answer_at_once_means_an_old_daemon() {
        assertTrue(classifyApprovalFailure(JSONException("No value for data"), 100) is ApprovalNotSupportedException)
        assertTrue(
            classifyApprovalFailure(IllegalStateException("Unexpected server message"), 100) is ApprovalNotSupportedException,
        )
    }

    @Test fun a_connection_closed_after_the_wait_began_is_not_an_old_daemon() {
        val closed = IllegalStateException("Connection closed")
        assertSame(closed, classifyApprovalFailure(closed, elapsedMs = 45_000))
    }

    @Test fun the_window_is_three_seconds() {
        val closed = IllegalStateException("Connection closed")
        assertTrue(classifyApprovalFailure(closed, 3_000) is ApprovalNotSupportedException)
        assertSame(closed, classifyApprovalFailure(closed, 3_001))
    }

    @Test fun a_daemon_answer_is_never_an_old_daemon() {
        val busy = GestoApiException("pairing_busy", "busy", true)
        assertSame(busy, classifyApprovalFailure(busy, 10))
    }

    @Test fun a_timeout_is_never_an_old_daemon() {
        val timeout = SocketTimeoutException("Read timed out")
        assertSame(timeout, classifyApprovalFailure(timeout, 10))
    }

    @Test fun other_failures_pass_through() {
        val other = IllegalStateException("Host fingerprint changed. Refusing to connect.")
        assertSame(other, classifyApprovalFailure(other, 10))
    }
}
