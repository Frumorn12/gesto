package it.frumorn.gesto.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import it.frumorn.gesto.GestoUiState
import it.frumorn.gesto.Screen
import it.frumorn.gesto.core.model.ConnectionState
import it.frumorn.gesto.core.model.TrustedHost
import it.frumorn.gesto.ui.theme.GestoTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The system back button reaches the shell's single back handler, and a handled back keeps the app open. */
@RunWith(AndroidJUnit4::class)
class BackHandlerWiringTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun pressBackCallsOnBackOnceAndKeepsTheActivityOpen() {
        var backCalls = 0
        val actions = GestoActions(onBack = { backCalls++; true })
        val host = TrustedHost(
            id = "host-1",
            hostName = "Legion",
            address = "192.168.0.22",
            port = 47771,
            fingerprint = "SHA256:7f1a3c9d20e4b8a6f0c1d2e3f4a5b6c7d8e9f0a1",
            deviceId = "device",
            sessionToken = "token",
            lastConnectedAt = 0L,
        )
        rule.setContent {
            GestoTheme {
                GestoAppShell(
                    state = GestoUiState(
                        screen = Screen.Touchpad,
                        connectionState = ConnectionState.Connected,
                        connectedHost = host,
                    ),
                    actions = actions,
                )
            }
        }
        rule.waitForIdle()

        Espresso.pressBack()
        rule.waitForIdle()

        assertEquals(1, backCalls)
        assertFalse(rule.activity.isFinishing)
    }
}
