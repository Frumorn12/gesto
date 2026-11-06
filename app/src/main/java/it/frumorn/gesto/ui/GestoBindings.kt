package it.frumorn.gesto.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import it.frumorn.gesto.AddPcStep
import it.frumorn.gesto.R
import it.frumorn.gesto.Screen
import it.frumorn.gesto.GestoUiState
import it.frumorn.gesto.GestoViewModel
import it.frumorn.gesto.core.externalinput.ExternalInputDeviceClass
import it.frumorn.gesto.core.model.CapabilitySummary
import it.frumorn.gesto.core.model.ConnectionState
import it.frumorn.gesto.core.model.DiscoveredHost
import it.frumorn.gesto.core.model.label
import it.frumorn.gesto.core.model.shownQuality
import it.frumorn.gesto.core.network.identity
import it.frumorn.gesto.core.session.Presence
import it.frumorn.gesto.core.session.isLocalNetworkAddress
import it.frumorn.gesto.remoteScreenFullscreen
import it.frumorn.gesto.sessionMode
import it.frumorn.gesto.ui.navigation.quickMenuEntries
import it.frumorn.gesto.ui.state.AddPcActions
import it.frumorn.gesto.ui.state.AddPcUiState
import it.frumorn.gesto.ui.state.ControlsActions
import it.frumorn.gesto.ui.state.ControlsUiState
import it.frumorn.gesto.ui.state.DiagnosticsActions
import it.frumorn.gesto.ui.state.DiagnosticsUiState
import it.frumorn.gesto.ui.state.HomeActions
import it.frumorn.gesto.ui.state.HomeUiState
import it.frumorn.gesto.ui.state.KeyboardActions
import it.frumorn.gesto.ui.state.LaunchActions
import it.frumorn.gesto.ui.state.LaunchUiState
import it.frumorn.gesto.ui.state.FoundPcState
import it.frumorn.gesto.ui.state.PcCardState
import it.frumorn.gesto.ui.state.QuickMenuActions
import it.frumorn.gesto.ui.state.QuickMenuUiState
import it.frumorn.gesto.ui.state.RemoteDisplayActions
import it.frumorn.gesto.ui.state.RemoteDisplayUiState
import it.frumorn.gesto.ui.state.SessionActions
import it.frumorn.gesto.ui.state.SessionUiState
import it.frumorn.gesto.ui.state.SettingsActions
import it.frumorn.gesto.ui.state.SettingsUiState
import it.frumorn.gesto.ui.state.TouchpadActions
import it.frumorn.gesto.ui.state.TouchpadInput
import it.frumorn.gesto.ui.state.TouchpadUiState
import it.frumorn.gesto.ui.text.UiText

/**
 * The single seam between `GestoViewModel` and the UI tree.
 *
 * Everything below [GestoApp] receives plain state objects and lambdas; this file is the only place
 * in `ui/` that is allowed to mention the ViewModel or `GestoUiState`.
 */
@Immutable
class GestoActions(
    val onNavigate: (Screen) -> Unit = {},
    val onOpenSettings: () -> Unit = {},
    /** The system back button; false means the app should close. */
    val onBack: () -> Boolean = { true },
    val launch: LaunchActions = LaunchActions(),
    val session: SessionActions = SessionActions(),
    val quickMenu: QuickMenuActions = QuickMenuActions(),
    /** The window turned (true: wider than tall). */
    val onLandscapeChanged: (Boolean) -> Unit = {},
    val addPc: AddPcActions = AddPcActions(),
    val touchpad: TouchpadActions = TouchpadActions(),
    val remoteDisplay: RemoteDisplayActions = RemoteDisplayActions(),
    val keyboard: KeyboardActions = KeyboardActions(),
    val controls: ControlsActions = ControlsActions(),
    val settings: SettingsActions = SettingsActions(),
    val home: HomeActions = HomeActions(),
    val diagnostics: DiagnosticsActions = DiagnosticsActions(),
)

/** Builds the callback tree once per ViewModel instance. */
@Composable
fun rememberGestoActions(viewModel: GestoViewModel): GestoActions = remember(viewModel) {
    GestoActions(
        onNavigate = viewModel::go,
        onOpenSettings = { viewModel.go(Screen.Settings) },
        onBack = viewModel::back,
        launch = LaunchActions(onChooseAnotherPc = viewModel::cancelLaunch),
        session = SessionActions(
            onBackToPcs = { viewModel.go(Screen.Home) },
            onOpenSettings = { viewModel.go(Screen.Settings) },
            onSelectMode = { mode -> viewModel.go(mode.screen) },
            onSetKeyboardOpen = viewModel::setKeyboardOpen,
            onSetQuickMenuOpen = viewModel::setQuickMenuOpen,
            onDisconnect = viewModel::disconnect,
        ),
        quickMenu = QuickMenuActions(
            onSetOpen = viewModel::setQuickMenuOpen,
            onEntry = viewModel::onQuickMenuEntry,
            onMoveButton = viewModel::moveQuickButton,
        ),
        onLandscapeChanged = viewModel::setLandscape,
        addPc = AddPcActions(
            onSelectPc = viewModel::selectHostForPairing,
            onOpenQr = viewModel::openQr,
            onOpenAddress = viewModel::openAddress,
            onCancelApproval = viewModel::cancelApproval,
            onCodeChange = viewModel::setPairingCode,
            onPairWithCode = viewModel::pairWithCode,
            onAddressChange = viewModel::setManualAddress,
            onPortChange = viewModel::setManualPort,
            onUseAddress = viewModel::useAddress,
            onInviteScanned = viewModel::applyInvite,
            onCloseQr = { viewModel.setAddPcStep(AddPcStep.Search) },
        ),
        touchpad = TouchpadActions(
            onPrepareInput = viewModel::prepareInput,
            onCheckInput = viewModel::refreshCapabilities,
            onPointerMove = viewModel::pointerMove,
            onScroll = { dx, dy, finish -> viewModel.scroll(dx, dy, finish) },
            onPointerButton = viewModel::pointerButton,
            onReleasePointerButtons = viewModel::releasePointerButtons,
            onBeginInteraction = viewModel::beginRemoteInteraction,
            onEndInteraction = viewModel::endRemoteInteraction,
            onToggleKeyboard = { viewModel.setKeyboardOpen(!viewModel.state.value.keyboardOpen) },
            onPointerCancelled = viewModel::notePointerCancellation,
            onPointerFailed = viewModel::notePointerFailure,
        ),
        remoteDisplay = RemoteDisplayActions(
            onStartStream = viewModel::startScreenStream,
            onPointerMove = viewModel::pointerMove,
            onPointerButton = { button, state -> viewModel.pointerButton(button, state) },
            onReleasePointerButtons = viewModel::releasePointerButtons,
            onDesktopPointerMove = viewModel::remoteScreenPointerMove,
            onDesktopClick = { x, y -> viewModel.remoteScreenClick(x, y) },
            onScroll = { dx, dy, finish -> viewModel.scroll(dx, dy, finish) },
            onToggleAudioMute = viewModel::toggleAudioMute,
            onToggleKeyboard = { viewModel.setKeyboardOpen(!viewModel.state.value.keyboardOpen) },
        ),
        keyboard = KeyboardActions(
            onKeyboardEdit = viewModel::sendLiveKeyboardEdit,
            onKey = viewModel::sendKey,
        ),
        controls = ControlsActions(
            onMedia = viewModel::media,
            onVolume = viewModel::volume,
            onBrightness = viewModel::brightness,
            onSystem = viewModel::system,
        ),
        settings = SettingsActions(
            onSelectProfile = viewModel::setStreamProfile,
            onSetMaxFps = viewModel::setStreamMaxFps,
            onSetMaxDimension = viewModel::setStreamMaxDimension,
            onSetJpegQuality = viewModel::setStreamJpegQuality,
            onToggleStats = viewModel::toggleStreamStats,
            onToggleHaptics = viewModel::toggleHaptics,
            onSetGameMode = viewModel::setRemoteScreenGameMode,
            onOpenTrustedHosts = { viewModel.go(Screen.Home) },
            onOpenDiagnostics = { viewModel.go(Screen.Diagnostics) },
            onDisconnect = viewModel::disconnect,
        ),
        home = HomeActions(
            onConnect = viewModel::connectById,
            onResume = viewModel::resumeSession,
            onDisconnect = viewModel::disconnect,
            onWake = viewModel::wake,
            onRename = viewModel::rename,
            onForget = viewModel::forget,
            onAddPc = viewModel::startAddPc,
            onOpenSettings = { viewModel.go(Screen.Settings) },
        ),
        diagnostics = DiagnosticsActions(
            onRefreshCapabilities = viewModel::refreshCapabilities,
        ),
    )
}

internal fun GestoUiState.toSessionState(): SessionUiState = SessionUiState(
    hostName = connectedHost?.label.orEmpty(),
    reconnecting = reconnecting || connectionState != ConnectionState.Connected,
    latencyMs = latencyMs,
    mode = screen.sessionMode ?: lastMode,
    keyboardOpen = keyboardOpen,
    fullscreen = remoteScreenFullscreen,
)

/** The quick menu of the session mode on screen; the purple button only on the Screen. */
internal fun GestoUiState.toQuickMenuState(): QuickMenuUiState {
    val mode = screen.sessionMode ?: lastMode
    return QuickMenuUiState(
        open = quickMenuOpen,
        entries = quickMenuEntries(mode, capabilities.captureSupported),
        audioOn = !audioMuted,
        touchMode = touchMode,
        quality = shownQuality(streamSettings.profile),
        gameMode = remoteScreenGameMode,
        showButton = screen == Screen.Display,
        buttonX = quickButtonX,
        buttonY = quickButtonY,
        pulse = quickMenuPulse,
    )
}

internal fun GestoUiState.toLaunchState(): LaunchUiState = LaunchUiState(
    hostName = launchHost?.label.orEmpty(),
    mode = lastMode,
    sameNetwork = launchHost?.address?.let(::isLocalNetworkAddress) ?: false,
)

/** Add a PC. A found PC already paired shows the name the user gave it. */
internal fun GestoUiState.toAddPcState(): AddPcUiState = AddPcUiState(
    step = addPcStep,
    found = discoveredHosts.map { host ->
        val paired = trustedHosts.firstOrNull { host.fingerprint.isNotBlank() && it.fingerprint == host.fingerprint }
        FoundPcState(key = host.identity, name = paired?.label ?: host.hostName, alreadyAdded = paired != null)
    },
    pin = pairingPin,
    code = pairingCode,
    busy = pairingBusy,
    address = manualAddress,
    port = manualPort,
)

internal fun GestoUiState.toTouchpadState(): TouchpadUiState = TouchpadUiState(
    input = when {
        // Until the PC has answered, what it can do is not known yet.
        connectionState == ConnectionState.Connected && !capabilities.inputSupported -> TouchpadInput.Blocked
        capabilities.inputBackend == "wayland-portal" -> TouchpadInput.NeedsApproval
        else -> TouchpadInput.Ready
    },
    haptics = haptics,
    twoFingerRightClick = twoFingerRightClick,
    hints = touchpadHints,
    controllerConnected = externalInputDevices.any { device ->
        device.classes.contains(ExternalInputDeviceClass.Gamepad) ||
            device.classes.contains(ExternalInputDeviceClass.Joystick)
    },
)

internal fun GestoUiState.toRemoteDisplayState(): RemoteDisplayUiState = RemoteDisplayUiState(
    frameWidth = videoWidth,
    frameHeight = videoHeight,
    sourceKey = screenStreamInfo?.source?.id ?: selectedScreenSourceId,
    captureSupported = capabilities.captureSupported,
    screenStreaming = screenStreaming,
    stats = screenStreamStats,
    showStats = streamSettings.showStats,
    latencyMs = latencyMs,
    fullscreen = remoteScreenFullscreen,
    touchMode = touchMode,
    haptics = haptics,
    audioMuted = audioMuted,
    keyboardOpen = keyboardOpen,
    twoFingerRightClick = twoFingerRightClick,
)

internal fun GestoUiState.toControlsState(): ControlsUiState = ControlsUiState(
    mediaSupported = capabilities.media,
    volumeSupported = capabilities.volume,
    brightnessSupported = capabilities.brightness,
    systemSupported = capabilities.lock || capabilities.suspend,
)

internal fun GestoUiState.toSettingsState(): SettingsUiState = SettingsUiState(
    streamSettings = streamSettings,
    haptics = haptics,
    gameMode = remoteScreenGameMode,
)

/**
 * The PCs list. The PC of the session is always Online; the others take the presence of the last
 * scans. "Wake" is offered only for a PC that is Offline and told the app its MAC addresses.
 */
internal fun GestoUiState.toHomeState(): HomeUiState = HomeUiState(
    pcs = trustedHosts.map { host ->
        val connected = connectedHost?.id == host.id
        val presence = if (connected) Presence.Online else presence[host.id] ?: Presence.Unknown
        val seen = scannedHosts.firstOrNull { host.fingerprint.isNotBlank() && it.fingerprint == host.fingerprint }
        PcCardState(
            id = host.id,
            name = host.label,
            presence = presence,
            connected = connected,
            canWake = !connected && presence == Presence.Offline && host.macAddresses.isNotEmpty(),
            // An Offline PC shows none: its capabilities are from a reply that is now old.
            chips = if (presence == Presence.Offline) emptyList() else pcChips(seen, if (connected) capabilities else null),
        )
    },
)

/**
 * What a PC can do, from the last scan that found it ([seen]) and, for the PC of the session, from
 * what it reported on connecting ([session]). Only what is known to work is listed.
 */
internal fun pcChips(seen: DiscoveredHost?, session: CapabilitySummary?): List<UiText> = buildList {
    if ((session?.captureSupported ?: seen?.captureSupported) == true) add(UiText.Res(R.string.home_chip_screen))
    if (session?.audioSupported == true) add(UiText.Res(R.string.home_chip_audio))
    if (session?.externalControllerSupported == true) add(UiText.Res(R.string.home_chip_controller))
}

internal fun GestoUiState.toDiagnosticsState(): DiagnosticsUiState = DiagnosticsUiState(
    connectionLabel = connectionState.name,
    capabilities = capabilities,
    externalInputStatus = externalInputStatus,
    externalInputDevices = externalInputDevices,
    screenStreaming = screenStreaming,
    stats = screenStreamStats,
    screenStatus = screenStatus,
    screenError = screenError,
)
