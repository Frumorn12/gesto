package it.frumorn.gesto.core.network

/** An error answer from the daemon, with its machine-readable [code]. */
class GestoApiException(val code: String, message: String, val retryable: Boolean) : IllegalStateException(message)
