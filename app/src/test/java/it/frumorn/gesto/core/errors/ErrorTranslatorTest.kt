package it.frumorn.gesto.core.errors

import it.frumorn.gesto.R
import it.frumorn.gesto.core.network.GestoApiException
import it.frumorn.gesto.ui.text.UiText
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue

class ErrorTranslatorTest {
    private fun t(e: Throwable, c: ErrorContext = ErrorContext.Command, port: Int = 47771) =
        ErrorTranslator.translate(e, c, port)

    private fun api(code: String) = GestoApiException(code, "msg", false)

    @Test fun refusedCarriesPort() =
        assertEquals(UiText.Res(R.string.error_pc_refused, listOf(1234)), t(ConnectException("x"), port = 1234))

    @Test fun timeout() = assertEquals(UiText.Res(R.string.error_pc_timeout), t(SocketTimeoutException()))
    @Test fun noRoute() = assertEquals(UiText.Res(R.string.error_pc_unreachable), t(NoRouteToHostException()))
    @Test fun unknownHost() = assertEquals(UiText.Res(R.string.error_pc_unreachable), t(UnknownHostException()))
    @Test fun fingerprint() =
        assertEquals(UiText.Res(R.string.error_fingerprint_changed), t(IllegalStateException("Host fingerprint changed")))
    @Test fun pairingDenied() = assertEquals(UiText.Res(R.string.error_code_wrong), t(api("pairing_denied")))
    @Test fun rateLimited() = assertEquals(UiText.Res(R.string.error_too_many_attempts), t(api("rate_limited")))
    @Test fun publicPairing() = assertEquals(UiText.Res(R.string.error_public_pairing), t(api("public_pairing_denied")))
    @Test fun rejected() = assertEquals(UiText.Res(R.string.error_rejected_on_pc), t(api("pairing_rejected")))
    @Test fun noAnswer() = assertEquals(UiText.Res(R.string.error_no_answer), t(api("pairing_timeout")))
    @Test fun approvalUnavailable() =
        assertEquals(UiText.Res(R.string.error_approval_unavailable), t(api("approval_unavailable")))
    @Test fun busy() = assertEquals(UiText.Res(R.string.error_pairing_busy), t(api("pairing_busy")))
    @Test fun unknownApp() = assertEquals(UiText.Res(R.string.error_unknown_app), t(api("unknown_app")))
    @Test fun unauthenticated() = assertEquals(UiText.Res(R.string.error_session_expired), t(api("unauthenticated")))
    @Test fun generic() =
        assertEquals(UiText.Res(R.string.error_generic, listOf("boom")), t(GestoApiException("weird", "boom", false)))
    @Test fun genericPlain() =
        assertEquals(UiText.Res(R.string.error_generic, listOf("oops")), t(RuntimeException("oops")))

    private val notApproved = GestoApiException("command_failed", "Input injection requires portal approval on this session", true)

    @Test fun inputNotApprovedIsTranslated() =
        assertEquals(UiText.Res(R.string.error_input_not_approved), t(notApproved))

    @Test fun inputNotApprovedIsRecognised() {
        assertTrue(ErrorTranslator.isInputApprovalMissing(notApproved))
        assertFalse(ErrorTranslator.isInputApprovalMissing(GestoApiException("command_failed", "Text input rejected", true)))
        assertFalse(ErrorTranslator.isInputApprovalMissing(null))
    }
}
