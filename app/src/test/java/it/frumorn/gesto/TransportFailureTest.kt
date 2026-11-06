package it.frumorn.gesto

import it.frumorn.gesto.core.model.ConnectionState
import it.frumorn.gesto.core.model.TrustedHost
import it.frumorn.gesto.core.network.GestoApiException
import java.io.EOFException
import java.net.SocketException
import java.net.SocketTimeoutException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TransportFailureTest {
    @Test fun daemonAnswerIsNotTransportEvenIfItSoundsLikeIo() {
        assertFalse(GestoApiException("command_failed", "Broken pipe (os error 32)", false).isTransportFailure())
        assertFalse(RuntimeException("wrapped", GestoApiException("x", "Connection reset", false)).isTransportFailure())
    }

    @Test fun ioErrorsAreTransport() {
        assertTrue(SocketException("x").isTransportFailure())
        assertTrue(EOFException().isTransportFailure())
        assertTrue(SocketTimeoutException("Read timed out").isTransportFailure())
    }

    @Test fun notConnectedAndClosedAreTransport() {
        assertTrue(IllegalStateException("Not connected").isTransportFailure())
        assertTrue(IllegalStateException("Connection closed").isTransportFailure())
    }

    @Test fun otherErrorsAreNot() {
        assertFalse(IllegalStateException("boom").isTransportFailure())
    }
}

class RetryAndDiscoveryTest {
    @Test fun requestMayHaveRunIsDetectedButStillTransport() {
        val e = it.frumorn.gesto.core.network.RequestMayHaveRunException(SocketTimeoutException("Read timed out"))
        assertTrue(e.isTransportFailure())
        assertTrue(e.mayHaveRun())
        assertFalse(SocketException("write failed").mayHaveRun())
    }

    @Test fun discoveryKeepsHeldSessionState() {
        val host = TrustedHost("a:1", "A", "a", 1, "f", "d", "t", 0)
        val held = GestoUiState(connectedHost = host, connectionState = ConnectionState.Connected)
        assertEquals(ConnectionState.Connected, held.stateDuringDiscovery(ConnectionState.Disconnected))
        assertEquals(ConnectionState.Connected, held.stateDuringDiscovery(ConnectionState.Discovering))
        assertEquals(ConnectionState.Discovering, GestoUiState().stateDuringDiscovery(ConnectionState.Discovering))
    }
}
