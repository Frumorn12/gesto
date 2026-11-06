package it.frumorn.gesto.ui

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.frumorn.gesto.AddPcStep
import it.frumorn.gesto.GestoUiState
import it.frumorn.gesto.GestoViewModel
import it.frumorn.gesto.R
import it.frumorn.gesto.Screen
import it.frumorn.gesto.core.model.ConnectionState
import it.frumorn.gesto.core.model.NoticeAction
import it.frumorn.gesto.core.model.TrustedHost
import it.frumorn.gesto.isSession
import it.frumorn.gesto.ui.components.GestoPageBar
import it.frumorn.gesto.ui.navigation.ScreensCapturingExternalPointer
import it.frumorn.gesto.ui.screens.ControlsScreen
import it.frumorn.gesto.ui.screens.DiscoveryScreen
import it.frumorn.gesto.ui.screens.KeyboardScreen
import it.frumorn.gesto.ui.screens.LaunchScreen
import it.frumorn.gesto.ui.screens.PairingScreen
import it.frumorn.gesto.ui.screens.RemoteDisplayScreen
import it.frumorn.gesto.ui.screens.RemoteDisplayVideoSurface
import it.frumorn.gesto.ui.screens.RemotePadScreen
import it.frumorn.gesto.ui.screens.SettingsScreen
import it.frumorn.gesto.ui.screens.TroubleshootingScreen
import it.frumorn.gesto.ui.screens.TrustedHostsScreen
import it.frumorn.gesto.ui.session.GestoSnackbarHost
import it.frumorn.gesto.ui.session.SessionScaffold
import it.frumorn.gesto.ui.system.ExternalPointerCaptureEffect
import it.frumorn.gesto.ui.system.FullscreenSystemUiEffect
import it.frumorn.gesto.ui.system.RemoteScreenOrientationEffect
import it.frumorn.gesto.ui.system.SystemBarsThemeEffect
import it.frumorn.gesto.ui.system.findActivity
import it.frumorn.gesto.ui.text.resolve
import it.frumorn.gesto.ui.theme.GestoMotion
import it.frumorn.gesto.ui.theme.GestoTheme
import it.frumorn.gesto.ui.theme.ThemeMode
import it.frumorn.gesto.ui.theme.isDark

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
            val action = notice.action
            val result = snackbarHostState.showSnackbar(
                message = notice.text.resolve(noticeContext),
                actionLabel = action?.let { noticeContext.getString(it.label) },
                duration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed && action != null) viewModel.onNoticeAction(action)
        }
    }
    val actions = rememberGestoActions(viewModel)
    GestoTheme(mode = state.themeMode, roundedCorners = state.roundedCorners) {
        SystemBarsThemeEffect(dark = state.themeMode.isDark())
        GestoAppShell(
            state = state,
            actions = actions,
            // The ViewModel stops here: the shell receives a slot, not the session.
            videoSurface = { RemoteDisplayVideoSurface(viewModel.videoSession.renderer) },
            snackbarHostState = snackbarHostState,
        )
    }
}

@get:StringRes
private val NoticeAction.label: Int
    get() = when (this) {
        NoticeAction.Retry -> R.string.action_retry
        NoticeAction.OpenPcs -> R.string.nav_your_pcs
        NoticeAction.UseCode -> R.string.action_use_code
    }

/**
 * Plain holder written during composition. A page that is fading out still renders the latest
 * state, where its own data may already be gone (the session screen, the PC being connected to):
 * the holder keeps the last value worth showing.
 */
private class Last<T>(var value: T)

/**
 * App chrome and screen switcher.
 *
 * Navigation is a state machine owned by the ViewModel rather than a `NavHost` - see
 * [it.frumorn.gesto.ui.navigation.SessionDockItems] for the rationale. The three session screens
 * share one [SessionScaffold]; every other screen gets a page bar.
 */
@Composable
internal fun GestoAppShell(
    state: GestoUiState,
    actions: GestoActions,
    modifier: Modifier = Modifier,
    videoSurface: @Composable () -> Unit = {},
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() },
) {
    val remoteFullscreen = state.screen == Screen.Display && state.remoteScreenFullscreen
    val externalPointerCapture = state.connectionState == ConnectionState.Connected &&
        state.screen in ScreensCapturingExternalPointer

    RemoteScreenOrientationEffect(
        onRemoteDisplay = state.screen == Screen.Display,
        fullscreen = remoteFullscreen,
    )
    FullscreenSystemUiEffect(remoteFullscreen)
    ExternalPointerCaptureEffect(externalPointerCapture)
    val activity = LocalContext.current.findActivity()
    // Always on: the ViewModel decides, and only the PCs list or the launch screen let the app close.
    BackHandler { if (!actions.onBack()) activity?.finish() }

    val enterOffset = with(LocalDensity.current) { GestoMotion.ENTER_OFFSET_DP.dp.roundToPx() }
    var dockHeight by remember { mutableStateOf(0.dp) }
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        AnimatedContent(
            // The session is one page: switching mode animates inside it, the bars stay put.
            targetState = state.screen.takeUnless { it.isSession },
            transitionSpec = { GestoMotion.screenChange(enterOffset) },
            label = "page",
        ) { page ->
            if (page == null) {
                SessionPage(state, actions, videoSurface, enterOffset) { dockHeight = it }
            } else {
                PlainPage(page, state, actions)
            }
        }
        GestoSnackbarHost(
            hostState = snackbarHostState,
            dockHeight = if (state.screen.isSession && !remoteFullscreen) dockHeight else 0.dp,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SessionPage(
    state: GestoUiState,
    actions: GestoActions,
    videoSurface: @Composable () -> Unit,
    enterOffset: Int,
    onDockHeightChanged: (Dp) -> Unit,
) {
    val last = remember { Last(Screen.Touchpad) }
    if (state.screen.isSession) last.value = state.screen
    val session = state.toSessionState()
    val gutter = if (session.fullscreen) GestoTheme.spacing.none else GestoTheme.spacing.gutter
    SessionScaffold(
        state = session,
        actions = actions.session,
        onDockHeightChanged = onDockHeightChanged,
        // Until the new keyboard sheet arrives, the old keyboard screen lives in the sheet.
        keyboardSheet = { KeyboardScreen(actions.keyboard) },
    ) {
        AnimatedContent(
            targetState = last.value,
            transitionSpec = { GestoMotion.screenChange(enterOffset) },
            label = "session-mode",
        ) { screen ->
            Box(
                Modifier
                    .padding(horizontal = gutter)
                    .fillMaxSize()
            ) {
                when (screen) {
                    Screen.Touchpad -> RemotePadScreen(state.toRemotePadState(), actions.remotePad)
                    Screen.Display -> RemoteDisplayScreen(
                        state = state.toRemoteDisplayState(),
                        actions = actions.remoteDisplay,
                        videoSurface = videoSurface,
                    )
                    Screen.Commands -> ControlsScreen(state.toControlsState(), actions.controls)
                    else -> Unit
                }
            }
        }
    }
}

@Composable
private fun PlainPage(page: Screen, state: GestoUiState, actions: GestoActions) {
    val back: () -> Unit = { actions.onBack() }
    Scaffold(
        // Transparent so the Box background shows through; contentColorFor(Transparent) is
        // Unspecified, so the content colour is provided explicitly.
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        topBar = {
            when (page) {
                Screen.Launch -> Unit
                Screen.Home -> GestoPageBar(onOpenSettings = actions.onOpenSettings, error = state.error)
                else -> GestoPageBar(onBack = back, error = state.error)
            }
        },
    ) { padding ->
        Box(
            Modifier
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding()
                .padding(horizontal = GestoTheme.spacing.gutter)
                .fillMaxSize()
        ) {
            when (page) {
                Screen.Launch -> {
                    val launch = state.toLaunchState()
                    val shown = remember { Last(launch) }
                    if (launch.hostName.isNotEmpty()) shown.value = launch
                    LaunchScreen(shown.value, actions.launch)
                }
                // Home and Add a PC still show the old screens until their redesign lands.
                Screen.Home -> TrustedHostsScreen(state.toTrustedHostsState(), actions.trustedHosts)
                Screen.AddPc -> when (state.addPcStep) {
                    AddPcStep.Code -> PairingScreen(state.toPairingState(), actions.pairing)
                    // Approval has no screen yet: nothing leads there before the PIN flow exists.
                    AddPcStep.Search, AddPcStep.Approval, AddPcStep.Address, AddPcStep.Qr ->
                        DiscoveryScreen(state.toDiscoveryState(), actions.discovery)
                }
                Screen.Settings -> SettingsScreen(state.toSettingsState(), actions.settings)
                Screen.Diagnostics -> TroubleshootingScreen(state.toDiagnosticsState(), actions.diagnostics)
                // Advanced has no screen yet and no entry point leads to it.
                Screen.Advanced -> Unit
                Screen.Touchpad, Screen.Display, Screen.Commands -> Unit
            }
        }
    }
}

@Preview(name = "Shell - home - dark", widthDp = 412, heightDp = 900)
@Composable
private fun GestoAppShellPreviewDark() {
    GestoTheme(mode = ThemeMode.Dark) {
        GestoAppShell(state = GestoUiState(trustedHosts = listOf(previewHost)), actions = GestoActions())
    }
}

@Preview(name = "Shell - touchpad - light", widthDp = 412, heightDp = 900)
@Composable
private fun GestoAppShellPreviewLight() {
    GestoTheme(mode = ThemeMode.Light) {
        GestoAppShell(
            state = GestoUiState(
                screen = Screen.Touchpad,
                connectionState = ConnectionState.Connected,
                connectedHost = previewHost,
                latencyMs = 4,
            ),
            actions = GestoActions(),
        )
    }
}

@Preview(name = "Shell - launch - light", widthDp = 412, heightDp = 900)
@Composable
private fun GestoAppShellPreviewLaunch() {
    GestoTheme(mode = ThemeMode.Light) {
        GestoAppShell(
            state = GestoUiState(screen = Screen.Launch, launchHost = previewHost),
            actions = GestoActions(),
        )
    }
}

private val previewHost = TrustedHost(
    id = "host-1",
    hostName = "Legion",
    address = "192.168.0.22",
    port = 47771,
    fingerprint = "SHA256:7f1a3c9d20e4b8a6f0c1d2e3f4a5b6c7d8e9f0a1",
    deviceId = "device",
    sessionToken = "token",
    lastConnectedAt = 0L,
)
