package it.frumorn.gesto.core.session

import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull

class ReconnectPolicyTest {
    private val p = ReconnectPolicy()

    @Test fun backoff() {
        assertEquals(0L, p.nextDelay(0, 0))
        assertEquals(1000L, p.nextDelay(1, 0))
        assertEquals(2000L, p.nextDelay(2, 0))
        assertEquals(3000L, p.nextDelay(3, 0))
        assertEquals(3000L, p.nextDelay(7, 0))
    }

    @Test fun budget() {
        assertNull(p.nextDelay(5, 28_000))
        assertNull(p.nextDelay(0, 30_001))
    }
}
