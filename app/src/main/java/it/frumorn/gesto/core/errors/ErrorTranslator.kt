package it.frumorn.gesto.core.errors

import it.frumorn.gesto.R
import it.frumorn.gesto.core.network.GestoApiException
import it.frumorn.gesto.ui.text.UiText
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

enum class ErrorContext { Pairing, Connecting, Command, Stream }

/** Turns exceptions into plain-language [UiText]. Pure: no Android Context. */
object ErrorTranslator {
    fun translate(error: Throwable, context: ErrorContext, port: Int = 47771): UiText {
        val chain = generateSequence(error) { it.cause }.take(8).toList()
        chain.firstOrNull { it is ConnectException }?.let { return UiText.Res(R.string.error_pc_refused, listOf(port)) }
        if (chain.any { it is SocketTimeoutException }) return UiText.Res(R.string.error_pc_timeout)
        if (chain.any { it is NoRouteToHostException || it is UnknownHostException }) {
            return UiText.Res(R.string.error_pc_unreachable)
        }
        if (chain.any { it.message?.contains("fingerprint changed", ignoreCase = true) == true }) {
            return UiText.Res(R.string.error_fingerprint_changed)
        }
        val api = chain.firstOrNull { it is GestoApiException } as? GestoApiException
        if (api != null) {
            val id = when (api.code) {
                "pairing_denied" -> R.string.error_code_wrong
                "rate_limited" -> R.string.error_too_many_attempts
                "public_pairing_denied" -> R.string.error_public_pairing
                "pairing_rejected" -> R.string.error_rejected_on_pc
                "pairing_timeout" -> R.string.error_no_answer
                "approval_unavailable" -> R.string.error_approval_unavailable
                "pairing_busy" -> R.string.error_pairing_busy
                "unknown_app" -> R.string.error_unknown_app
                "unauthenticated" -> R.string.error_session_expired
                else -> null
            }
            if (id != null) return UiText.Res(id)
        }
        return UiText.Res(R.string.error_generic, listOf(error.message ?: error::class.java.simpleName))
    }
}
