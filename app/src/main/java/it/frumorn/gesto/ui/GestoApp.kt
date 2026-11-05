package it.frumorn.gesto.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import it.frumorn.gesto.ui.text.resolve
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.frumorn.gesto.Screen
import it.frumorn.gesto.GestoUiState
import it.frumorn.gesto.GestoViewModel
import it.frumorn.gesto.core.model.ConnectionState
import it.frumorn.gesto.ui.components.GestoNavigationBar
import it.frumorn.gesto.ui.components.GestoTopBar
import it.frumorn.gesto.ui.navigation.ScreensCapturingExternalPointer
import it.frumorn.gesto.ui.navigation.ScreensReturningToDiscovery
import it.frumorn.gesto.ui.navigation.GestoNavItems
import it.frumorn.gesto.ui.screens.ControlsScreen
import it.frumorn.gesto.ui.screens.DiscoveryScreen
import it.frumorn.gesto.ui.screens.KeyboardScreen
import it.frumorn.gesto.ui.screens.OnboardingScreen
import it.frumorn.gesto.ui.screens.PairingScreen
import it.frumorn.gesto.ui.screens.RemoteDisplayScreen
import it.frumorn.gesto.ui.screens.RemoteDisplayVideoSurface
import it.frumorn.gesto.ui.screens.RemotePadScreen
import it.frumorn.gesto.ui.screens.SettingsScreen
import it.frumorn.gesto.ui.screens.TroubleshootingScreen
import it.frumorn.gesto.ui.screens.TrustedHostsScreen
import it.frumorn.gesto.ui.system.ExternalPointerCaptureEffect
import it.frumorn.gesto.ui.system.FullscreenSystemUiEffect
import it.frumorn.gesto.ui.system.RemoteScreenOrientationEffect
import it.frumorn.gesto.ui.theme.GestoTheme
import it.frumorn.gesto.ui.theme.ThemeMode

/**
 * Root of the Gesto UI: the only composable that knows about [GestoViewModel].
 *
 * It collects the ViewModel state, builds the callback tree once, and hands both down as plain
 * values. Every screen underneath is a pure function of its own immutable state plus lambdas, which
 * is what makes the `@Preview`s under `ui.screens` and `ui.components` possible.
 */
@Composable
fun GestoApp(viewModel: GestoViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val noticeContext = LocalContext.current
    LaunchedEffect(viewModel) {
        // Resolved with the context here, not stringResource, because this runs outside composition.
        viewModel.notices.collect { notice ->
            snackbarHostState.showSnackbar(notice.text.resolve(noticeContext))
        }
    }
    val actions = rememberGestoActions(viewModel)
    GestoTheme(mode = state.themeMode, roundedCorners = state.roundedCorners) {
        GestoAppShell(
            state = state,
            actions = actions,
            // The ViewModel stops here: the shell receives a slot, not the session.
            videoSurface = { RemoteDisplayVideoSurface(viewModel.videoSession.renderer) },
            snackbarHostState = snackbarHostState,
        )
    }
}

/**
 * App chrome and screen switcher.
 *
 * Navigation is a state machine owned by the ViewModel rather than a `NavHost` - see
 * [it.frumorn.gesto.ui.navigation.GestoNavItems] for the rationale.
 */
@Composable
internal fun GestoAppShell(
    state: GestoUiState,
    actions: GestoActions,
    modifier: Modifier = Modifier,
    videoSurface: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val remoteFullscreen = state.screen == Screen.RemoteDisplay && state.remoteScreenFullscreen
    val externalPointerCapture = state.connectionState == ConnectionState.Connected &&
        state.screen in ScreensCapturingExternalPointer
    val connected = state.connectionState == ConnectionState.Connected

    RemoteScreenOrientationEffect(
        onRemoteDisplay = state.screen == Screen.RemoteDisplay,
        fullscreen = remoteFullscreen,
    )
    FullscreenSystemUiEffect(remoteFullscreen)
    ExternalPointerCaptureEffect(externalPointerCapture)
    BackHandler(remoteFullscreen) { actions.onExitFullscreen() }
    BackHandler(!remoteFullscreen && state.screen in ScreensReturningToDiscovery) {
        actions.onNavigate(Screen.Discovery)
    }

    val gutter = GestoTheme.spacing.gutter
    val none = GestoTheme.spacing.none
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold(
            // Transparent so the Box background shows through; contentColorFor(Transparent) is
            // Unspecified, so the content colour is provided explicitly.
            containerColor = Color.Transparent,
            contentColor = MaterialTheme.colorScheme.onBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                if (!remoteFullscreen) {
                    GestoTopBar(state.toShellState(), actions.onOpenSettings)
                }
            },
            bottomBar = {
                if (connected && !remoteFullscreen) {
                    GestoNavigationBar(
                        items = GestoNavItems,
                        selectedKey = state.screen,
                        onSelect = { key -> actions.onNavigate(key as Screen) },
                    )
                }
            },
        ) { padding ->
            Box(
                Modifier
                    .padding(if (remoteFullscreen) PaddingValues(none) else padding)
                    .padding(horizontal = if (remoteFullscreen) none else gutter)
                    .fillMaxSize()
            ) {
                when (state.screen) {
                    Screen.Onboarding -> OnboardingScreen(actions.onboarding)
                    Screen.Discovery -> DiscoveryScreen(state.toDiscoveryState(), actions.discovery)
                    Screen.Pairing -> PairingScreen(state.toPairingState(), actions.pairing)
                    Screen.Remote -> RemotePadScreen(state.toRemotePadState(), actions.remotePad)
                    Screen.RemoteDisplay -> RemoteDisplayScreen(
                        state = state.toRemoteDisplayState(),
                        actions = actions.remoteDisplay,
                        videoSurface = videoSurface,
                    )
                    Screen.Keyboard -> KeyboardScreen(actions.keyboard)
                    Screen.Controls -> ControlsScreen(state.toControlsState(), actions.controls)
                    Screen.Settings -> SettingsScreen(state.toSettingsState(), actions.settings)
                    Screen.TrustedHosts -> TrustedHostsScreen(
                        state = state.toTrustedHostsState(),
                        actions = actions.trustedHosts,
                    )
                    Screen.Troubleshooting -> TroubleshootingScreen(
                        state = state.toDiagnosticsState(),
                        actions = actions.diagnostics,
                    )
                }
            }
        }
    }
}

@Preview(name = "Shell - onboarding - dark", widthDp = 412, heightDp = 900)
@Composable
private fun GestoAppShellPreviewDark() {
    GestoTheme(mode = ThemeMode.Dark) {
        GestoAppShell(state = GestoUiState(), actions = GestoActions())
    }
}

@Preview(name = "Shell - connected - light", widthDp = 412, heightDp = 900)
@Composable
private fun GestoAppShellPreviewLight() {
    GestoTheme(mode = ThemeMode.Light) {
        GestoAppShell(
            state = GestoUiState(
                screen = Screen.Controls,
                connectionState = ConnectionState.Connected,
                status = "Connected to frumorn-desktop",
            ),
            actions = GestoActions(),
        )
    }
}
