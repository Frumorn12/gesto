package it.frumorn.gesto

import androidx.annotation.StringRes
import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import it.frumorn.gesto.core.externalinput.ExternalInputDeviceClass
import it.frumorn.gesto.core.externalinput.ExternalInputDeviceSummary
import it.frumorn.gesto.core.externalinput.ExternalInputEvent
import it.frumorn.gesto.core.errors.ErrorContext
import it.frumorn.gesto.core.errors.ErrorTranslator
import it.frumorn.gesto.core.input.PointerAcceleration
import it.frumorn.gesto.core.input.PointerDelta
import it.frumorn.gesto.core.input.SubPixelCarry
import it.frumorn.gesto.core.input.liveEditOps
import it.frumorn.gesto.core.model.ButtonState
import it.frumorn.gesto.core.model.CapabilitySummary
import it.frumorn.gesto.core.model.ConnectionState
import it.frumorn.gesto.core.model.DiscoveredHost
import it.frumorn.gesto.core.model.Notice
import it.frumorn.gesto.core.model.NoticeAction
import it.frumorn.gesto.core.model.shouldEmitNotice
import it.frumorn.gesto.core.model.PointerButton
import it.frumorn.gesto.core.model.RemoteApp
import it.frumorn.gesto.core.model.RemoteScreenConnectionState
import it.frumorn.gesto.core.model.ScreenSource
import it.frumorn.gesto.core.model.ScreenStreamInfo
import it.frumorn.gesto.core.model.ScreenStreamStats
import it.frumorn.gesto.core.model.QualityPreset
import it.frumorn.gesto.core.model.StreamSettings
import it.frumorn.gesto.core.model.TouchMode
import it.frumorn.gesto.core.model.atRung
import it.frumorn.gesto.core.model.gameModeChange
import it.frumorn.gesto.core.model.nextQuality
import it.frumorn.gesto.core.model.settings
import it.frumorn.gesto.core.model.TrustedHost
import it.frumorn.gesto.core.model.formatFps
import it.frumorn.gesto.core.model.MAX_PC_NAME_LENGTH
import it.frumorn.gesto.core.model.label
import it.frumorn.gesto.core.video.AutoQualityController
import it.frumorn.gesto.core.video.RemoteScreenVideoSession
import it.frumorn.gesto.core.video.StreamDelay
import it.frumorn.gesto.core.network.ConnectResult
import it.frumorn.gesto.core.network.GestoApiException
import it.frumorn.gesto.core.network.GestoClient
import it.frumorn.gesto.core.network.WakeOnLan
import it.frumorn.gesto.core.network.mergeSightings
import it.frumorn.gesto.core.network.RequestMayHaveRunException
import it.frumorn.gesto.core.network.GestoDiscovery
import it.frumorn.gesto.core.network.GestoInvite
import it.frumorn.gesto.core.network.PairingCancelledException
import it.frumorn.gesto.core.network.identity
import it.frumorn.gesto.core.pairing.AddPcFlow
import it.frumorn.gesto.core.pairing.AddPcNext
import it.frumorn.gesto.core.pairing.PinGenerator
import it.frumorn.gesto.core.system.DeviceName
import it.frumorn.gesto.core.screen.RemoteScreenSessionEvent
import it.frumorn.gesto.core.screen.RemoteScreenSessionMachine
import it.frumorn.gesto.core.session.HostPresenceTracker
import it.frumorn.gesto.core.session.Presence
import it.frumorn.gesto.core.session.ReconnectPolicy
import it.frumorn.gesto.core.session.launchTarget
import it.frumorn.gesto.ui.text.UiText
import it.frumorn.gesto.core.storage.SettingsRepository
import it.frumorn.gesto.core.storage.TrustedHostStore
import it.frumorn.gesto.ui.navigation.BackAction
import it.frumorn.gesto.ui.navigation.QuickMenuEntry
import it.frumorn.gesto.ui.navigation.backAction
import it.frumorn.gesto.ui.navigation.reachableScreen
import it.frumorn.gesto.ui.theme.ThemeMode
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.abs

enum class Screen { Launch, Home, AddPc, Touchpad, Display, Commands, Settings, Advanced, Diagnostics }

/** The three ways to drive a connected PC; the app reopens on the last one used. */
enum class SessionMode(val screen: Screen) {
    Touchpad(Screen.Touchpad),
    Display(Screen.Display),
    Commands(Screen.Commands),
}

/** Steps of "Add a PC": search the network, then approve a PIN, type a code, an address or scan a QR. */
enum class AddPcStep { Search, Approval, Code, Address, Qr }

/** The session mode this screen shows, null outside a session. */
val Screen.sessionMode: SessionMode?
    get() = SessionMode.entries.firstOrNull { it.screen == this }

/** Touchpad, Display and Commands: the screens that live inside the session shell. */
val Screen.isSession: Boolean
    get() = sessionMode != null

private val SettingsScreens = setOf(Screen.Settings, Screen.Advanced, Screen.Diagnostics)

/** The remote screen fills the display, bars hidden: the phone is on its side, or Game mode is on. */
val GestoUiState.remoteScreenFullscreen: Boolean
    get() = screen == Screen.Display && (landscape || remoteScreenGameMode)

/** What the screen streams with now: the chosen quality, at Auto's current rung. */
val GestoUiState.streamingSettings: StreamSettings
    get() = streamSettings.atRung(autoQualityRung)

/** Intent extra (and `gesto://connect?host=` parameter) naming the paired PC to open on. */
const val EXTRA_CONNECT_HOST = "it.frumorn.gesto.CONNECT_HOST"

data class GestoUiState(
    val screen: Screen = Screen.Home,
    /** Where Settings was opened from, so back returns there. */
    val previousScreen: Screen? = null,
    /** The keyboard sheet over the current session mode. */
    val keyboardOpen: Boolean = false,
    val quickMenuOpen: Boolean = false,
    val addPcStep: AddPcStep = AddPcStep.Search,
    val lastMode: SessionMode = SessionMode.Touchpad,
    /** The PC the launch screen is connecting to. */
    val launchHost: TrustedHost? = null,
    val connectionState: ConnectionState = ConnectionState.Disconnected,
    val discoveredHosts: List<DiscoveredHost> = emptyList(),
    val trustedHosts: List<TrustedHost> = emptyList(),
    /** Presence of each paired PC (by host id) while the PCs list scans the network. */
    val presence: Map<String, Presence> = emptyMap(),
    /** The last reply of each PC the presence scans found, for the capabilities on the PC cards. */
    val scannedHosts: List<DiscoveredHost> = emptyList(),
    val selectedHost: DiscoveredHost? = null,
    val connectedHost: TrustedHost? = null,
    val capabilities: CapabilitySummary = CapabilitySummary(),
    val pairingCode: String = "",
    /** The PIN shown while the PC is asked to let this phone in; empty outside that step. */
    val pairingPin: String = "",
    /** A pairing (PIN or code) is waiting for the PC's answer. */
    val pairingBusy: Boolean = false,
    val manualAddress: String = "",
    val manualPort: String = "47771",
    val status: String = "Ready",
    val error: String? = null,
    val haptics: Boolean = true,
    val themeMode: ThemeMode = ThemeMode.System,
    val roundedCorners: Boolean = false,
    val remoteInputSessionActive: Boolean = false,
    val remoteInputBacklog: Int = 0,
    /**
     * Fingerprints of the PCs that said their input is approved (the answer to "Ask the PC"), in
     * this run of the app. The daemon keeps the approval while it runs, so it outlives a reconnect;
     * a refusal for lack of approval takes the PC out again.
     */
    val inputApprovedHosts: Set<String> = emptySet(),
    /** Touchpad pointer speed, 0.5 to 3.0. */
    val pointerSpeed: Float = PointerAcceleration.DEFAULT_SPEED,
    val pointerAcceleration: Boolean = true,
    /** A two-finger tap on the touchpad is a right click. */
    val twoFingerRightClick: Boolean = true,
    /** The touchpad shows its gesture hints in this session (one of the first five). */
    val touchpadHints: Boolean = false,
    val externalInputDevices: List<ExternalInputDeviceSummary> = emptyList(),
    val externalInputStatus: String = "No external input devices detected",
    val screenSources: List<ScreenSource> = emptyList(),
    /** Advanced is waiting for the PC's list of screens. */
    val screenSourcesLoading: Boolean = false,
    val selectedScreenSourceId: String? = null,
    val screenStreamInfo: ScreenStreamInfo? = null,
    /**
     * Source resolution of the stream. With hardware decoding no frame object reaches the UI, but
     * the gesture mapping still needs the desktop size to convert touches, so it is published
     * explicitly instead of being read off a decoded bitmap.
     */
    val videoWidth: Int? = null,
    val videoHeight: Int? = null,
    val audioMuted: Boolean = false,
    val screenStreaming: Boolean = false,
    val screenConnectionState: RemoteScreenConnectionState = RemoteScreenConnectionState.Idle,
    /** The window is wider than tall (reported by the app shell): the remote screen goes full screen. */
    val landscape: Boolean = false,
    val remoteScreenGameMode: Boolean = false,
    /** What Game mode replaced, put back when it goes off; null while it is off. */
    val streamSettingsBeforeGame: StreamSettings? = null,
    /** How a finger on the remote screen drives the PC. */
    val touchMode: TouchMode = TouchMode.Touchpad,
    /** Where the purple quick-menu button sits, as fractions of the room it can move in. */
    val quickButtonX: Float = 1f,
    val quickButtonY: Float = 1f,
    /** The button pulses in this session (one of the first few) until the menu is opened. */
    val quickMenuPulse: Boolean = false,
    /** The quality chosen; for Auto, the values of its first rung (see [streamingSettings]). */
    val streamSettings: StreamSettings = StreamSettings(),
    /** The rung Auto has stepped down to in this session (0 = best); it only goes down. */
    val autoQualityRung: Int = 0,
    val screenStreamStats: ScreenStreamStats = ScreenStreamStats(),
    val screenStatus: String = "Screen stream idle",
    val screenError: String? = null,
    /** True while a lost connection is being re-established in the background. */
    val reconnecting: Boolean = false,
    /** Round-trip time of the last health ping, null when unknown. */
    val latencyMs: Int? = null,
)

class GestoViewModel(
    application: Application,
    private val savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    private companion object {
        const val TAG = "GestoViewModel"
        const val INPUT_QUEUE_CAPACITY = 48
        const val REALTIME_INPUT_BACKLOG_HIGH_WATERMARK = 24
        const val INTERACTION_GRACE_MS = 180L
        const val INTERACTION_KEEPALIVE_INITIAL_MS = 2_000L
        const val INTERACTION_KEEPALIVE_INTERVAL_MS = 5_000L
        const val SCREEN_STREAM_MAX_RETRIES = 3
        const val EXTERNAL_UNSUPPORTED_NOTICE_MS = 2_000L
        const val HEALTH_PING_INTERVAL_MS = 5_000L
        const val HEALTH_PING_IDLE_MS = 1_000L
        const val HEALTH_PING_MAX_FAILURES = 2
        const val LAUNCH_TIMEOUT_MS = 6_000L
        /** The PCs list scans the network this often while it is on screen. */
        const val PRESENCE_INTERVAL_MS = 3_000L
        /** How long one presence scan listens for replies. */
        const val PRESENCE_LISTEN_MS = 1_200
        /** Rest between two scans when one ran long, so the radio is never kept busy back to back. */
        const val PRESENCE_MIN_GAP_MS = 500L
        const val DEFAULT_PORT = 47771
        /** The touchpad's gesture hints show in this many sessions. */
        const val TOUCHPAD_HINT_SESSIONS = 5
        /** The quick-menu button pulses in this many sessions. */
        const val QUICK_MENU_HINT_SESSIONS = 5
        /**
         * The Automatic quality starts judging a stream this long after its first frame: the
         * frame rate is measured over the last 2 s, so it is still filling up before that.
         */
        const val AUTO_QUALITY_WARMUP_MS = 2_000L
    }

    private val discovery = GestoDiscovery(application)
    private val client = GestoClient()
    /** Owns the MediaCodec decoder and the render surface; the shell reads `renderer` from it. */
    val videoSession = RemoteScreenVideoSession()
    private val store = TrustedHostStore(application)
    private val settingsRepo = SettingsRepository(application)
    private val inputCommands = Channel<RemoteInputCommand>(capacity = INPUT_QUEUE_CAPACITY)
    private val queuedInputCommands = AtomicInteger(0)
    private var interactionKeepAliveJob: Job? = null
    private var interactionEndJob: Job? = null
    private var screenStreamJob: Job? = null
    private var screenStreamDesired = false
    /** Steps the Automatic quality down while the network can't keep up; reset for every session. */
    private val autoQuality = AutoQualityController()
    /** Whether this stream's frames queue up on the way (the network falls behind). */
    private val streamDelay = StreamDelay()
    /** When the stream on screen showed its first frame (monotonic ms); 0 before that. */
    private var streamShownAtMs = 0L
    private val screenSessionMachine = RemoteScreenSessionMachine()
    private var activeInteractionSessionId = 0L
    private var nextInteractionSessionId = 0L
    private var lastExternalUnsupportedNoticeAt = 0L
    private var handledInvitePayload: String? = null
    private var pendingInvite: GestoInvite? = null
    private var lastControllerCaptureNoticeAt = 0L
    private val _state = MutableStateFlow(
        GestoUiState(trustedHosts = store.load())
    )
    val state: StateFlow<GestoUiState> = _state

    // A channel, not a SharedFlow: a notice raised before the UI subscribes (a launch that fails
    // while the splash is still up) must wait for it instead of being dropped.
    private val _notices = Channel<Notice>(capacity = 8, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    /** One-shot messages for the user (shown as a snackbar), each delivered once. */
    val notices: Flow<Notice> = _notices.receiveAsFlow()
    private val reconnectPolicy = ReconnectPolicy()
    private var reconnectJob: Deferred<Boolean>? = null
    private var lastInputAtMs = 0L
    private var lastNotice: Notice? = null
    private var lastNoticeAtMs = 0L
    /** Id of the launch attempt the app is still waiting for; 0 when none. */
    private var liveLaunch = 0L
    private var launchCounter = 0L
    private var launchTimeoutJob: Job? = null
    /** The PC a "Try again" on a notice connects to. */
    private var retryHostId: String? = null
    private val presenceTracker = HostPresenceTracker()
    private var presenceJob: Job? = null
    /** When the PCs list stopped scanning; a longer pause makes its presence stale. */
    private var presencePausedAtMs = 0L
    /** The search step of Add a PC scans while it is on screen. */
    private var addPcScanJob: Job? = null
    private var lastAddPcScan: List<DiscoveredHost> = emptyList()
    /** Id of the pairing (PIN or code) the app is still waiting for; 0 when none. */
    private var livePairing = 0L
    private var pairingCounter = 0L
    /** The PC being paired was typed in by address: a failure goes back to that step. */
    private var pairingFromAddress = false
    /** Whether this session shows the touchpad hints has been decided (and counted). */
    private var touchpadHintsDecided = false
    /** Whether the quick-menu button pulses in this session has been decided (and counted). */
    private var quickMenuPulseDecided = false
    /** Fractions of a pixel the touchpad moves left behind, sent with the next move. */
    private val pointerCarry = SubPixelCarry()

    /**
     * Live typing and keys from the keyboard sheet go through one queue and one consumer, so the
     * backspaces and text of edit N are all sent before edit N+1 starts, in the order typed.
     */
    private val keyboardQueue = Channel<suspend () -> Unit>(Channel.UNLIMITED)

    init {
        // A refused pointer move or click is answered later, while another request is read.
        client.onOneWayError = { error -> viewModelScope.launch { reportCommandFailure(error) } }
        observeVideoSession()
        viewModelScope.launch { drainInputCommands() }
        viewModelScope.launch { for (job in keyboardQueue) runQuiet(job) }
        viewModelScope.launch { runHealthPing() }
        viewModelScope.launch {
            settingsRepo.streamSettings.collect { settings ->
                Log.i(TAG, "settings_loaded preset=${settings.preset} fps=${settings.maxFps} quality=${settings.jpegQuality} dimension=${settings.maxDimension}")
                _state.update { it.copy(streamSettings = settings) }
            }
        }
        viewModelScope.launch {
            settingsRepo.haptics.collect { haptics ->
                _state.update { it.copy(haptics = haptics) }
            }
        }
        viewModelScope.launch {
            settingsRepo.themeMode.collect { mode ->
                _state.update { it.copy(themeMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsRepo.roundedCorners.collect { rounded ->
                _state.update { it.copy(roundedCorners = rounded) }
            }
        }
        viewModelScope.launch {
            settingsRepo.gameMode.collect { gameMode ->
                _state.update { it.copy(remoteScreenGameMode = gameMode) }
            }
        }
        viewModelScope.launch {
            settingsRepo.lastMode.collect { mode ->
                _state.update { it.copy(lastMode = mode) }
            }
        }
        viewModelScope.launch {
            settingsRepo.pointerSpeed.collect { speed -> _state.update { it.copy(pointerSpeed = speed) } }
        }
        viewModelScope.launch {
            settingsRepo.pointerAcceleration.collect { on -> _state.update { it.copy(pointerAcceleration = on) } }
        }
        viewModelScope.launch {
            settingsRepo.twoFingerRightClick.collect { on -> _state.update { it.copy(twoFingerRightClick = on) } }
        }
        viewModelScope.launch {
            settingsRepo.settingsBeforeGame.collect { before -> _state.update { it.copy(streamSettingsBeforeGame = before) } }
        }
        viewModelScope.launch {
            settingsRepo.touchMode.collect { mode -> _state.update { it.copy(touchMode = mode) } }
        }
        viewModelScope.launch {
            settingsRepo.hudPosition.collect { (x, y) -> _state.update { it.copy(quickButtonX = x, quickButtonY = y) } }
        }
        // After process death the saved session wins; otherwise open the PC a launcher shortcut
        // asked for (its intent extra arrives here as a default argument), else the last PC used.
        if (!restoreSessionState()) {
            val hosts = store.load()
            val requested = savedStateHandle.remove<String>(EXTRA_CONNECT_HOST)?.let { id -> hosts.find { it.id == id } }
            (requested ?: launchTarget(hosts))?.let(::launchConnect)
        }
    }

    /** Reconnects the session saved before process death. Returns false when there is none. */
    private fun restoreSessionState(): Boolean {
        val savedHostId = savedStateHandle.get<String>("connectedHostId") ?: return false
        val savedScreen = savedStateHandle.get<String>("screen")
        val host = store.load().find { it.id == savedHostId }
        if (host == null) {
            Log.w(TAG, "session_restore saved host not found in store")
            savedStateHandle["connectedHostId"] = null
            return false
        }
        Log.i(TAG, "session_restore found saved host=${host.hostName} screen=$savedScreen")
        retryHostId = host.id
        val screen = savedScreen
            ?.let { s -> runCatching { Screen.valueOf(s) }.getOrNull() }
            ?.takeIf { it.isSession }
            ?: Screen.Touchpad
        _state.update {
            it.copy(
                connectedHost = host,
                connectionState = ConnectionState.Connecting,
                status = "Reconnecting to ${host.hostName}...",
            )
        }
        go(screen)
        viewModelScope.launch {
            runCatching { client.connect(host) }
                .onSuccess { result ->
                    Log.i(TAG, "session_restore reconnected host=${host.hostName}")
                    val updated = rememberConnection(host, result, touch = false)
                    _state.update {
                        it.copy(
                            trustedHosts = store.load(),
                            connectedHost = updated,
                            capabilities = result.capabilities,
                            connectionState = ConnectionState.Connected,
                            status = "Reconnected to ${host.hostName}",
                            error = null,
                        )
                    }
                    publishCurrentExternalDevices()
                    autoStartScreenStream()
                }
                .onFailure { throwable ->
                    if (throwable is CancellationException) throw throwable
                    Log.w(TAG, "session_restore reconnect failed", throwable)
                    _state.update {
                        it.copy(
                            connectionState = ConnectionState.Disconnected,
                            connectedHost = null,
                            status = "Ready",
                        )
                    }
                    go(Screen.Home)
                    savedStateHandle["connectedHostId"] = null
                    savedStateHandle["screen"] = null
                    emitNotice(Notice(connectFailureText(host, throwable), NoticeAction.Retry))
                }
        }
        return true
    }

    /**
     * Opens the launch screen and connects to [host], giving up after [LAUNCH_TIMEOUT_MS]: then the
     * user lands on the PCs list with a notice. A socket can take longer than that to give up, so the
     * attempt is not awaited past the deadline; whatever it opens afterwards is dropped.
     */
    private fun launchConnect(host: TrustedHost) {
        abandonLaunch()
        val attempt = ++launchCounter
        liveLaunch = attempt
        retryHostId = host.id
        // A new attempt deserves its own answer, even if it is the same as the last one.
        lastNotice = null
        Log.i(TAG, "launch_connect host=${host.hostName} address=${host.address}:${host.port}")
        go(Screen.Launch)
        _state.update {
            it.copy(
                launchHost = host,
                connectionState = ConnectionState.Connecting,
                status = "Connecting to ${host.hostName}...",
            )
        }
        viewModelScope.launch {
            // Read in parallel with the handshake so the saved mode is known when it ends.
            val savedMode = async { runCatching { settingsRepo.lastMode.first() }.getOrDefault(SessionMode.Touchpad) }
            val result = runCatching { client.connect(host) }
            if (result.exceptionOrNull() is CancellationException) return@launch
            val mode = if (result.isSuccess) savedMode.await() else SessionMode.Touchpad
            if (liveLaunch != attempt) {
                // Timed out, or the user chose another PC: nobody wants this channel any more.
                if (result.isSuccess) releaseLateLaunchChannel()
                return@launch
            }
            liveLaunch = 0L
            launchTimeoutJob?.cancel()
            launchTimeoutJob = null
            result
                .onSuccess { result -> onLaunchConnected(host, result, mode) }
                .onFailure { onLaunchFailed(host, it) }
        }
        launchTimeoutJob = viewModelScope.launch {
            delay(LAUNCH_TIMEOUT_MS)
            if (liveLaunch != attempt) return@launch
            Log.w(TAG, "launch_timeout host=${host.hostName}")
            liveLaunch = 0L
            launchTimeoutJob = null
            onLaunchFailed(host, null)
        }
    }

    private fun onLaunchConnected(host: TrustedHost, result: ConnectResult, mode: SessionMode) {
        Log.i(TAG, "connection_state=connected launch=true host=${host.hostName} mode=$mode")
        val updated = rememberConnection(host, result)
        savedStateHandle["connectedHostId"] = updated.id
        _state.update {
            it.copy(
                trustedHosts = store.load(),
                connectedHost = updated,
                launchHost = null,
                capabilities = result.capabilities,
                connectionState = ConnectionState.Connected,
                status = "Connected to ${host.hostName}",
                error = null,
            )
        }
        go(mode.screen)
    }

    /**
     * Stores what a connection to [host] taught: when it was used ([touch]) and the PC's MAC
     * addresses (kept from before when this daemon reports none). Works on the stored entry, so a
     * rename made meanwhile is kept; a PC forgotten meanwhile is not stored again.
     */
    private fun rememberConnection(host: TrustedHost, result: ConnectResult, touch: Boolean = true): TrustedHost {
        val macs = result.macAddresses
        if (!touch && (macs.isEmpty() || macs == host.macAddresses)) return host
        return store.update(host.id) { stored ->
            stored.copy(
                lastConnectedAt = if (touch) System.currentTimeMillis() else stored.lastConnectedAt,
                macAddresses = macs.ifEmpty { stored.macAddresses },
            )
        } ?: host
    }

    private fun onLaunchFailed(host: TrustedHost, error: Throwable?) {
        Log.w(TAG, "launch_failed host=${host.hostName}", error)
        _state.update {
            it.copy(
                launchHost = null,
                connectionState = ConnectionState.Disconnected,
                status = "Ready",
            )
        }
        go(Screen.Home)
        emitNotice(Notice(connectFailureText(host, error), NoticeAction.Retry))
    }

    /** Stops waiting for the launch connection; the attempt ends in the background and is dropped. */
    private fun abandonLaunch() {
        if (liveLaunch == 0L) return
        Log.i(TAG, "launch_abandoned")
        liveLaunch = 0L
        launchTimeoutJob?.cancel()
        launchTimeoutJob = null
        _state.update {
            it.copy(
                launchHost = null,
                connectionState = if (it.connectedHost == null) ConnectionState.Disconnected else it.connectionState,
            )
        }
    }

    /**
     * A launch connection that arrived after the app stopped waiting. Closed unless another
     * connection or pairing has been started since: that one owns the client now.
     */
    private fun releaseLateLaunchChannel() {
        val s = _state.value
        val busy = s.connectedHost != null || liveLaunch != 0L ||
            s.connectionState == ConnectionState.Connecting || s.connectionState == ConnectionState.Pairing
        if (busy) return
        Log.i(TAG, "launch_late_channel_closed")
        client.close()
    }

    /** "Choose another PC" on the launch screen. */
    fun cancelLaunch() {
        go(Screen.Home)
    }

    /** Connects again to the PC of the last failed or lost connection (the notice's "Try again"). */
    fun retryLastConnection() {
        if (_state.value.connectedHost != null) return
        lastNotice = null
        val host = retryHostId?.let { id -> store.load().find { it.id == id } } ?: return
        launchConnect(host)
    }

    fun onNoticeAction(action: NoticeAction) {
        when (action) {
            NoticeAction.Retry -> retryLastConnection()
            NoticeAction.OpenPcs -> go(Screen.Home)
            NoticeAction.UseCode -> {
                go(Screen.AddPc)
                // The code needs the PC it was meant for; without one, the search.
                setAddPcStep(if (_state.value.selectedHost != null) AddPcStep.Code else AddPcStep.Search)
            }
        }
    }

    /**
     * The only place the screen changes: it closes the keyboard sheet and the quick menu, starts
     * the screen stream on entering Display and stops it on leaving, remembers the session mode and
     * where Settings was opened from. A session screen without a connected PC becomes the PCs list.
     */
    fun go(requested: Screen) {
        val screen = reachableScreen(requested, connected = _state.value.connectedHost != null)
        if (screen != Screen.Launch) abandonLaunch()
        if (screen != Screen.AddPc) abandonPairing()
        val wasCapturingExternalInput = shouldCaptureExternalInput(_state.value)
        val previous = _state.value.screen
        if (previous == Screen.Display && screen != Screen.Display) {
            stopScreenStream()
        }
        if (screen.isSession) {
            savedStateHandle["screen"] = screen.name
        }
        screen.sessionMode?.let { mode -> viewModelScope.launch { settingsRepo.saveLastMode(mode) } }
        _state.update {
            it.copy(
                screen = screen,
                previousScreen = when {
                    screen == Screen.Settings && previous !in SettingsScreens -> previous
                    screen in SettingsScreens -> it.previousScreen
                    else -> null
                },
                keyboardOpen = false,
                quickMenuOpen = false,
                lastMode = screen.sessionMode ?: it.lastMode,
                error = null,
            )
        }
        if (!wasCapturingExternalInput && shouldCaptureExternalInput(_state.value)) {
            publishCurrentExternalDevices()
        }
        if (screen == Screen.Touchpad) decideTouchpadHints()
        // Advanced offers the PC's screens; the Screen itself streams the one the PC remembers.
        val connected = _state.value.connectedHost != null && _state.value.connectionState == ConnectionState.Connected
        if (screen == Screen.Advanced && previous != Screen.Advanced && connected) {
            loadScreenSources()
        }
        if (screen == Screen.Display) {
            decideQuickMenuPulse()
            autoStartScreenStream()
        }
    }

    /**
     * The Screen shows the PC's screen as soon as it opens, from the source the PC remembers:
     * there is no start button. Nothing happens while the PC is not connected yet (it is started
     * once it is), when it cannot share its screen, or when a stream is already wanted.
     */
    private fun autoStartScreenStream() {
        val s = _state.value
        if (s.screen != Screen.Display || s.connectionState != ConnectionState.Connected) return
        if (!s.capabilities.captureSupported || screenStreamDesired) return
        Log.i(TAG, "screen_stream_auto_start")
        startScreenStream()
    }

    /**
     * The first time a session shows the Screen: the quick-menu button pulses if fewer than
     * [QUICK_MENU_HINT_SESSIONS] sessions have pulsed it, and this session is counted.
     */
    private fun decideQuickMenuPulse() {
        if (quickMenuPulseDecided) return
        quickMenuPulseDecided = true
        viewModelScope.launch {
            val shown = runCatching { settingsRepo.hudHintSessions.first() }.getOrDefault(QUICK_MENU_HINT_SESSIONS)
            val pulse = shown < QUICK_MENU_HINT_SESSIONS
            _state.update { it.copy(quickMenuPulse = pulse) }
            if (pulse) settingsRepo.saveHudHintSessions(shown + 1)
        }
    }

    /**
     * The first time a session shows the touchpad: its gesture hints show if fewer than
     * [TOUCHPAD_HINT_SESSIONS] sessions have shown them, and this session is counted.
     */
    private fun decideTouchpadHints() {
        if (touchpadHintsDecided) return
        touchpadHintsDecided = true
        viewModelScope.launch {
            val shown = runCatching { settingsRepo.touchpadHintSessions.first() }.getOrDefault(TOUCHPAD_HINT_SESSIONS)
            val show = shown < TOUCHPAD_HINT_SESSIONS
            _state.update { it.copy(touchpadHints = show) }
            if (show) settingsRepo.saveTouchpadHintSessions(shown + 1)
        }
    }

    /**
     * The system back button. Returns false when the app should close: only from the PCs list or
     * the launch screen, never from a state in between.
     */
    fun back(): Boolean {
        val s = _state.value
        when (val action = backAction(s.screen, s.keyboardOpen, s.quickMenuOpen, s.addPcStep, s.previousScreen)) {
            BackAction.CloseKeyboard -> setKeyboardOpen(false)
            BackAction.CloseQuickMenu -> setQuickMenuOpen(false)
            BackAction.OpenQuickMenu -> setQuickMenuOpen(true)
            is BackAction.AddPcStepTo -> setAddPcStep(action.step)
            // Settings opened from a session that has since ended lands on Home (see go()).
            is BackAction.GoTo -> go(action.screen)
            BackAction.ExitApp -> return false
        }
        return true
    }

    fun setKeyboardOpen(open: Boolean) {
        _state.update {
            val allowed = open && it.screen.isSession
            it.copy(keyboardOpen = allowed, quickMenuOpen = if (allowed) false else it.quickMenuOpen)
        }
    }

    fun setQuickMenuOpen(open: Boolean) {
        _state.update {
            val allowed = open && it.screen.isSession
            // Once found, the button has done its job of being noticed for this session.
            it.copy(quickMenuOpen = allowed, quickMenuPulse = it.quickMenuPulse && !allowed)
        }
    }

    /** MODE, or SELECT and START together, on a controller: opens the quick menu, or closes it. */
    fun toggleQuickMenuFromController(reason: String) {
        val s = _state.value
        if (!s.screen.isSession) return
        Log.i(TAG, "quick_menu_controller reason=$reason open=${!s.quickMenuOpen}")
        if (s.keyboardOpen) setKeyboardOpen(false)
        setQuickMenuOpen(!s.quickMenuOpen)
    }

    /** A line of the quick menu. Audio, touch mode and quality keep it open, to be flipped again. */
    fun onQuickMenuEntry(entry: QuickMenuEntry) {
        val s = _state.value
        Log.i(TAG, "quick_menu_entry entry=$entry screen=${s.screen}")
        when (entry) {
            QuickMenuEntry.Keyboard -> setKeyboardOpen(true)
            QuickMenuEntry.RightClick -> {
                setQuickMenuOpen(false)
                pointerButton(PointerButton.Right, ButtonState.Pressed)
                pointerButton(PointerButton.Right, ButtonState.Released)
            }
            QuickMenuEntry.Audio -> toggleAudioMute()
            QuickMenuEntry.TouchMode -> setTouchMode(
                if (s.touchMode == TouchMode.Touchpad) TouchMode.Direct else TouchMode.Touchpad,
            )
            QuickMenuEntry.Quality -> setQualityPreset(nextQuality(s.streamSettings.preset))
            QuickMenuEntry.GameMode -> {
                setQuickMenuOpen(false)
                val enable = !s.remoteScreenGameMode
                // From another mode, Game mode means the Screen: its settings are in place before
                // the stream starts, so it starts once.
                setRemoteScreenGameMode(enable)
                if (enable && s.screen != Screen.Display) go(Screen.Display)
            }
            QuickMenuEntry.BackToTouchpad -> go(Screen.Touchpad)
            QuickMenuEntry.BackToPcs -> go(Screen.Home)
        }
    }

    fun setTouchMode(mode: TouchMode) {
        if (_state.value.touchMode == mode) return
        Log.i(TAG, "touch_mode mode=$mode")
        _state.update { it.copy(touchMode = mode) }
        viewModelScope.launch { settingsRepo.saveTouchMode(mode) }
    }

    /** The purple button was dragged here (fractions of the room it can move in). */
    fun moveQuickButton(x: Float, y: Float) {
        val fx = x.coerceIn(0f, 1f)
        val fy = y.coerceIn(0f, 1f)
        _state.update { it.copy(quickButtonX = fx, quickButtonY = fy) }
        viewModelScope.launch { settingsRepo.saveHudPosition(fx, fy) }
    }

    /** The window turned; on its side the remote screen is full screen and takes the controller. */
    fun setLandscape(landscape: Boolean) {
        val before = _state.value
        if (before.landscape == landscape) return
        _state.update { it.copy(landscape = landscape) }
        if (!before.remoteScreenFullscreen && _state.value.remoteScreenFullscreen) publishCurrentExternalDevices()
    }

    /**
     * Moves Add a PC to [step]. Leaving the PIN (or the code while it is being checked) stops the
     * pairing: its connection closes, so the PC withdraws its notification.
     */
    fun setAddPcStep(step: AddPcStep) {
        if (step != _state.value.addPcStep) abandonPairing()
        _state.update { it.copy(addPcStep = step, error = null) }
    }

    fun applyInvite(raw: String) {
        if (handledInvitePayload == raw) {
            Log.i(TAG, "qr_invite_ignored duplicate=true")
            return
        }
        runCatching { GestoInvite.parse(raw) }
            .onSuccess { invite ->
                pendingInvite = invite
                Log.i(
                    TAG,
                    "qr_invite_loaded host=${invite.hostName} endpoints=${invite.endpoints.joinToString { "${it.route}:${it.address}:${it.port}" }} route=${invite.route} policy=${invite.policy} publicPairingAllowed=${invite.publicPairingAllowed} expires=${invite.expiresAt}",
                )
                go(Screen.AddPc)
                // An invite replaces whatever pairing was running, even on the code step itself.
                abandonPairing()
                pairingFromAddress = false
                setAddPcStep(AddPcStep.Code)
                // After the abandons above, which forget the last invite, so this one is not run twice.
                handledInvitePayload = raw
                _state.update {
                    it.copy(
                        selectedHost = invite.toDiscoveredHost(),
                        pairingCode = invite.pairingCode,
                        manualAddress = invite.address,
                        manualPort = invite.port.toString(),
                        status = "Loaded ${invite.route} invite for ${invite.hostName.ifBlank { invite.address }}",
                        error = null,
                    )
                }
                pairWithCode()
            }
            .onFailure { throwable ->
                Log.w(TAG, "qr_invite_failed message=${throwable.message}", throwable)
                go(Screen.AddPc)
                setAddPcStep(AddPcStep.Search)
                emitNotice(Notice(UiText.Res(R.string.error_invite_invalid)))
            }
    }

    fun updateExternalInputDevices(devices: List<ExternalInputDeviceSummary>) {
        val previous = _state.value.externalInputDevices.associateBy { it.id }
        val current = devices.associateBy { it.id }
        val added = devices.filter { it.id !in previous }
        val removed = previous.values.filter { it.id !in current }
        if (added.isNotEmpty() || removed.isNotEmpty()) {
            Log.i(
                TAG,
                "external_devices_changed added=${added.map { it.name }} removed=${removed.map { it.name }} count=${devices.size}",
            )
        }
        _state.update {
            it.copy(
                externalInputDevices = devices,
                externalInputStatus = externalInputSummary(devices),
            )
        }
        val updated = _state.value
        if (shouldCaptureExternalInput(updated)) {
            added.forEach { device ->
                if (shouldForwardExternalDeviceConnected(updated, device)) {
                    enqueueExternalDeviceConnected(device)
                }
            }
            removed.forEach { device ->
                if (shouldForwardExternalDeviceConnected(updated, device)) {
                    enqueueInput(
                        RemoteInputCommand.External(
                            ExternalInputEvent.DeviceDisconnected(
                                deviceId = device.id,
                                deviceType = device.classes.primaryTypeForStatus(),
                            ),
                        ),
                    )
                }
            }
        }
    }

    private fun publishCurrentExternalDevices() {
        val current = _state.value
        if (!shouldCaptureExternalInput(current)) return
        current.externalInputDevices
            .filter { shouldForwardExternalDeviceConnected(current, it) }
            .forEach(::enqueueExternalDeviceConnected)
    }

    private fun enqueueExternalDeviceConnected(device: ExternalInputDeviceSummary) {
        enqueueInput(
            RemoteInputCommand.External(
                ExternalInputEvent.DeviceConnected(
                    deviceId = device.id,
                    deviceType = device.classes.primaryTypeForStatus(),
                    name = device.name,
                    classes = device.classes,
                ),
            ),
        )
    }

    fun handleExternalInputEvent(event: ExternalInputEvent): Boolean {
        val current = _state.value
        if (!shouldForwardExternalInput(current, event)) {
            if (shouldConsumeControllerLocally(current, event)) {
                noteControllerWaitingForGameMode(event)
                return true
            }
            return false
        }
        if (!isExternalInputSupported(current, event)) {
            noteExternalUnsupported(event)
            return true
        }
        enqueueInput(RemoteInputCommand.External(event))
        return true
    }

    private fun noteExternalUnsupported(event: ExternalInputEvent) {
        val now = System.currentTimeMillis()
        if (now - lastExternalUnsupportedNoticeAt < EXTERNAL_UNSUPPORTED_NOTICE_MS) return
        lastExternalUnsupportedNoticeAt = now
        val label = event.deviceType.unsupportedLabel()
        val reason = _state.value.capabilities.externalInputReason
        Log.w(TAG, "external_input_unsupported type=${event.deviceType.wireName} reason=$reason")
        _state.update {
            it.copy(
                externalInputStatus = "$label forwarding unsupported: $reason",
                status = "$label forwarding unsupported",
            )
        }
    }

    private fun noteControllerWaitingForGameMode(event: ExternalInputEvent) {
        val now = System.currentTimeMillis()
        if (now - lastControllerCaptureNoticeAt < EXTERNAL_UNSUPPORTED_NOTICE_MS) return
        lastControllerCaptureNoticeAt = now
        Log.d(
            TAG,
            "controller_input_held_for_fullscreen device=${event.deviceId} type=${event.deviceType.wireName}",
        )
        _state.update {
            it.copy(
                externalInputStatus = "Controller ready. Turn the phone on the Screen, or turn on Game mode, to use it on the PC.",
            )
        }
    }

    fun setPairingCode(code: String) {
        _state.update { it.copy(pairingCode = code.filter(Char::isDigit).take(6)) }
    }

    fun setManualAddress(value: String) {
        _state.update { it.copy(manualAddress = value.trim()) }
    }

    fun setManualPort(value: String) {
        _state.update { it.copy(manualPort = value.filter(Char::isDigit).take(5)) }
    }

    fun setThemeMode(mode: ThemeMode) {
        _state.update { it.copy(themeMode = mode) }
        viewModelScope.launch { settingsRepo.saveThemeMode(mode) }
    }

    fun setRoundedCorners(enabled: Boolean) {
        _state.update { it.copy(roundedCorners = enabled) }
        viewModelScope.launch { settingsRepo.saveRoundedCorners(enabled) }
    }

    fun toggleHaptics() {
        val enabled = !_state.value.haptics
        _state.update { it.copy(haptics = enabled) }
        viewModelScope.launch { settingsRepo.saveHaptics(enabled) }
    }

    fun setPointerSpeed(speed: Float) {
        val clamped = PointerAcceleration.clampSpeed(speed)
        _state.update { it.copy(pointerSpeed = clamped) }
        viewModelScope.launch { settingsRepo.savePointerSpeed(clamped) }
    }

    fun setPointerAcceleration(enabled: Boolean) {
        _state.update { it.copy(pointerAcceleration = enabled) }
        viewModelScope.launch { settingsRepo.savePointerAcceleration(enabled) }
    }

    fun setTwoFingerRightClick(enabled: Boolean) {
        _state.update { it.copy(twoFingerRightClick = enabled) }
        viewModelScope.launch { settingsRepo.saveTwoFingerRightClick(enabled) }
    }

    /** A quality from Settings or the quick menu; a running stream starts again with it. */
    fun setQualityPreset(preset: QualityPreset) {
        val current = _state.value.streamSettings
        // Custom is reached through Advanced; picked directly it keeps what streams now.
        val custom = if (current.preset == QualityPreset.Custom) current else _state.value.streamingSettings
        applyStreamSettings(preset.settings(custom = custom, showStats = current.showStats), userPick = true)
    }

    /** Advanced: own frames per second, which makes the quality Custom. */
    fun setStreamMaxFps(fps: Int) = applyCustom { it.copy(maxFps = fps) }

    /** Advanced: own encoder quality, which makes the quality Custom. */
    fun setStreamJpegQuality(quality: Int) = applyCustom { it.copy(jpegQuality = quality) }

    /** Advanced: own longest side of the picture, which makes the quality Custom. */
    fun setStreamMaxDimension(dimension: Int) = applyCustom { it.copy(maxDimension = dimension) }

    /** Advanced changes start from what streams now (Auto's current rung included). */
    private fun applyCustom(change: (StreamSettings) -> StreamSettings) {
        val base = _state.value.streamingSettings
        applyStreamSettings(QualityPreset.Custom.settings(custom = change(base), showStats = base.showStats))
    }

    /**
     * [userPick]: the user chose this preset, so choosing Automatic again starts it over from the
     * best rung even when the preset does not change.
     */
    private fun applyStreamSettings(settings: StreamSettings, userPick: Boolean = false) {
        val before = _state.value
        val switched = settings.preset != before.streamSettings.preset
        val toAuto = settings.preset == QualityPreset.Auto
        var rung = before.autoQualityRung
        if (switched || (userPick && toAuto)) {
            autoQuality.onPresetSwitched(toAuto)
            if (toAuto) rung = 0
        }
        if (settings == before.streamSettings && rung == before.autoQualityRung) return
        Log.i(TAG, "stream_settings preset=${settings.preset} fps=${settings.maxFps} quality=${settings.jpegQuality} max=${settings.maxDimension}")
        _state.update { it.copy(streamSettings = settings, autoQualityRung = rung) }
        if (settings != before.streamSettings) viewModelScope.launch { settingsRepo.saveStreamSettings(settings) }
        if (before.screenStreaming && _state.value.streamingSettings != before.streamingSettings) startScreenStream()
    }

    fun toggleStreamStats() {
        _state.update {
            it.copy(streamSettings = it.streamSettings.copy(showStats = !it.streamSettings.showStats))
        }
        viewModelScope.launch { settingsRepo.saveStreamSettings(_state.value.streamSettings) }
    }

    /**
     * Game mode: full screen, the Smooth quality and the controller forwarded to the PC. Turning it
     * off puts back the quality from before, unless another one was chosen meanwhile.
     */
    fun setRemoteScreenGameMode(enabled: Boolean) {
        val current = _state.value
        if (current.remoteScreenGameMode == enabled) return
        val (settings, before) = gameModeChange(enabled, current.streamSettings, current.streamSettingsBeforeGame)
        Log.i(TAG, "remote_screen_game_mode enabled=$enabled preset=${settings.preset} remembered=${before?.preset}")
        _state.update {
            it.copy(
                remoteScreenGameMode = enabled,
                streamSettings = settings,
                streamSettingsBeforeGame = before,
            )
        }
        viewModelScope.launch { settingsRepo.saveGameMode(enabled, settings, before) }
        if (settings.preset != current.streamSettings.preset) {
            autoQuality.onPresetSwitched(settings.preset == QualityPreset.Auto)
            if (settings.preset == QualityPreset.Auto) _state.update { it.copy(autoQualityRung = 0) }
        }
        if (enabled) publishCurrentExternalDevices()
        emitNotice(Notice(UiText.Res(if (enabled) R.string.notice_game_mode_on else R.string.notice_game_mode_off)))
        if (current.screenStreaming && settings.atRung(_state.value.autoQualityRung) != current.streamingSettings) {
            startScreenStream()
        }
    }

    /** "Add a PC" on the PCs list: the search step, which scans the network while it is shown. */
    fun startAddPc() {
        abandonPairing()
        pendingInvite = null
        pairingFromAddress = false
        lastAddPcScan = emptyList()
        go(Screen.AddPc)
        _state.update {
            it.copy(
                addPcStep = AddPcStep.Search,
                discoveredHosts = emptyList(),
                selectedHost = null,
                pairingCode = "",
                pairingPin = "",
                error = null,
            )
        }
    }

    /**
     * Scans the network every [PRESENCE_INTERVAL_MS] while [on]: the search step of Add a PC turns
     * it on while it is on screen and the app is in the foreground, and off otherwise.
     */
    fun setAddPcScanning(on: Boolean) {
        if (!on) {
            addPcScanJob?.cancel()
            addPcScanJob = null
            return
        }
        if (addPcScanJob?.isActive == true) return
        addPcScanJob = viewModelScope.launch {
            runScans { found ->
                val previous = lastAddPcScan
                lastAddPcScan = found
                _state.update { it.copy(discoveredHosts = AddPcFlow.searchResults(it.discoveredHosts, previous, found)) }
            }
        }
    }

    /** A PC found by the search: the PIN when its daemon can ask on the PC, else the code. */
    fun selectHostForPairing(key: String) {
        val host = _state.value.discoveredHosts.firstOrNull { it.identity == key } ?: return
        pendingInvite = null
        pairingFromAddress = false
        Log.i(TAG, "add_pc_selected host=${host.hostName} approval=${host.approvalPairing}")
        _state.update { it.copy(selectedHost = host, pairingCode = "") }
        when (AddPcFlow.firstStepFor(host)) {
            AddPcStep.Approval -> requestApprovalPairing()
            else -> setAddPcStep(AddPcStep.Code)
        }
    }

    /**
     * Asks the selected PC to let this phone in: the phone shows a new PIN, the PC a notification with
     * the same PIN. The answer opens the session; a refusal or silence goes back as [AddPcFlow] says.
     */
    fun requestApprovalPairing() {
        val host = _state.value.selectedHost ?: return
        abandonPairing()
        val attempt = ++pairingCounter
        livePairing = attempt
        val pin = PinGenerator.newPin()
        Log.i(TAG, "approval_pairing_request host=${host.hostName} address=${host.address}:${host.port}")
        _state.update {
            it.copy(
                addPcStep = AddPcStep.Approval,
                pairingPin = pin,
                pairingBusy = true,
                // A session that is held stays as it is: the wait does not touch its channel.
                connectionState = it.stateDuringDiscovery(ConnectionState.Pairing),
                error = null,
            )
        }
        // Undispatched: the client registers the attempt before this returns, so a Cancel that
        // follows always reaches it.
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            val result = runCatching {
                client.pairWithApproval(
                    address = host.address,
                    port = host.port,
                    deviceName = deviceName(),
                    pin = pin,
                    expectedFingerprint = host.fingerprint.ifBlank { null },
                )
            }
            if (result.exceptionOrNull() is CancellationException) return@launch
            when (val next = AddPcFlow.afterApproval(result, host.port)) {
                AddPcNext.Paired -> result.getOrNull()?.let { (paired, capabilities) -> onPaired(paired, capabilities, attempt) }
                is AddPcNext.Step -> onApprovalFailed(attempt, result.exceptionOrNull(), next)
            }
        }
    }

    /** Cancel on the PIN step: back to the search, and the PC withdraws its notification. */
    fun cancelApproval() = setAddPcStep(AddPcStep.Search)

    private fun onApprovalFailed(attempt: Long, error: Throwable?, next: AddPcNext.Step) {
        if (livePairing != attempt) {
            Log.i(TAG, "approval_pairing_dropped stale=true reason=${error?.javaClass?.simpleName}")
            // Abandoned: the channel a late launch left behind (if any) has nobody now.
            releaseStrayChannel()
            return
        }
        livePairing = 0L
        Log.w(TAG, "approval_pairing_failed next=${next.step}", error)
        // A typed-in address that failed goes back to the address, so it can be corrected.
        val step = if (next.step == AddPcStep.Search && pairingFromAddress) AddPcStep.Address else next.step
        _state.update {
            it.copy(
                addPcStep = step,
                pairingPin = "",
                pairingBusy = false,
                pairingCode = "",
                connectionState = it.stateDuringDiscovery(ConnectionState.Disconnected),
            )
        }
        releaseStrayChannel()
        next.notice?.let { emitNotice(Notice(it, next.action)) }
    }

    /** The QR step: the camera, reading the invite `gesto pair --qr` shows on the PC. */
    fun openQr() {
        // A fresh camera reads any invite, even the one read last time.
        handledInvitePayload = null
        setAddPcStep(AddPcStep.Qr)
    }

    fun openAddress() = setAddPcStep(AddPcStep.Address)

    /** "Continue" on the address step: the PC at that address is asked with a PIN, an old one falls back to the code. */
    fun useAddress() {
        val current = _state.value
        val address = current.manualAddress.trim()
        if (address.isEmpty()) return
        val port = current.manualPort.toIntOrNull()?.takeIf { it in 1..65535 } ?: DEFAULT_PORT
        pendingInvite = null
        pairingFromAddress = true
        _state.update {
            it.copy(
                selectedHost = DiscoveredHost(
                    hostName = address,
                    address = address,
                    port = port,
                    fingerprint = "",
                    inputSupported = false,
                    inputBackend = "manual",
                    captureSupported = false,
                    captureBackend = "manual",
                    // Not known for a typed address: asked first, the code follows if the daemon is old.
                    approvalPairing = true,
                ),
                pairingCode = "",
            )
        }
        requestApprovalPairing()
    }

    /** "Add the PC" on the code step (also run by a scanned invite, which carries its code). */
    fun pairWithCode() {
        val current = _state.value
        val host = current.selectedHost ?: return
        if (current.pairingCode.length != 6 || current.pairingBusy) return
        abandonPairing()
        val attempt = ++pairingCounter
        livePairing = attempt
        val invite = pendingInvite?.takeIf { it.pairingCode == current.pairingCode }
        val candidates = invite?.toCandidateHosts() ?: listOf(host)
        Log.i(
            TAG,
            "connection_state=pairing host=${host.hostName} candidates=${candidates.joinToString { "${it.address}:${it.port}" }}",
        )
        _state.update {
            it.copy(
                pairingBusy = true,
                connectionState = it.stateDuringDiscovery(ConnectionState.Pairing),
                status = "Pairing with ${host.hostName}...",
                error = null,
            )
        }
        viewModelScope.launch(start = CoroutineStart.UNDISPATCHED) {
            pairWithCandidates(
                candidates = candidates,
                deviceName = deviceName(),
                pairingCode = current.pairingCode,
                stillWanted = { livePairing == attempt },
            ).onSuccess { (paired, capabilities) ->
                onPaired(paired, capabilities, attempt)
            }.onFailure { error ->
                if (error is CancellationException) return@onFailure
                if (livePairing != attempt) {
                    // Abandoned: the channel a late launch left behind (if any) has nobody now.
                    releaseStrayChannel()
                    return@onFailure
                }
                livePairing = 0L
                Log.w(TAG, "code_pairing_failed", error)
                // The same invite may be scanned again to retry.
                handledInvitePayload = null
                _state.update {
                    it.copy(
                        pairingBusy = false,
                        pairingCode = "",
                        connectionState = it.stateDuringDiscovery(ConnectionState.Disconnected),
                        status = "Ready",
                    )
                }
                releaseStrayChannel()
                emitNotice(Notice(ErrorTranslator.translate(error, ErrorContext.Pairing, host.port)))
            }
        }
    }

    /**
     * The PC let this phone in: it is stored (keeping the name the user gave it, if it was paired
     * before) and its session opens on the Touchpad. An answer that arrives just after Cancel still
     * counts: the PC has already added the phone and the client has already switched to its channel.
     */
    private fun onPaired(paired: TrustedHost, capabilities: CapabilitySummary, attempt: Long) {
        if (livePairing != attempt) {
            Log.i(TAG, "pairing_answered_after_cancel")
            // A newer attempt is waiting for nothing now.
            if (livePairing != 0L) abandonPairing()
        }
        livePairing = 0L
        Log.i(TAG, "connection_state=connected paired_host=${paired.hostName} input_backend=${capabilities.inputBackend}")
        pendingInvite = null
        // The same invite may be scanned again later (after the PC is forgotten, say).
        handledInvitePayload = null
        val previous = _state.value.connectedHost
        if (previous != null && previous.fingerprint != paired.fingerprint) {
            // The session with the other PC lost its channel to this one.
            reconnectJob?.cancel()
            clearQueuedInput()
        }
        val previousName = store.load().firstOrNull { it.fingerprint == paired.fingerprint }?.displayName
        val trusted = paired.copy(displayName = previousName)
        store.upsert(trusted)
        retryHostId = trusted.id
        savedStateHandle["connectedHostId"] = trusted.id
        _state.update {
            it.copy(
                trustedHosts = store.load(),
                connectedHost = trusted,
                capabilities = capabilities,
                connectionState = ConnectionState.Connected,
                status = "Connected to ${trusted.hostName}",
                error = null,
                reconnecting = false,
                pairingBusy = false,
                pairingPin = "",
                pairingCode = "",
                selectedHost = null,
                addPcStep = AddPcStep.Search,
            )
        }
        go(Screen.Touchpad)
        emitNotice(Notice(UiText.Res(R.string.notice_pc_added, listOf(trusted.label))))
    }

    /**
     * Stops waiting for the pairing in flight, if any. Its connection is closed, so the daemon
     * withdraws the notification and stores nothing.
     *
     * The client's own channel is not touched here: the PC may have answered a moment ago, and then
     * that channel is the new session's, which [onPaired] is about to open. A pairing that was really
     * stopped fails on its own, and its failure path releases what nobody owns.
     */
    private fun abandonPairing() {
        if (livePairing == 0L) return
        livePairing = 0L
        val stopped = client.cancelPairing()
        Log.i(TAG, "pairing_abandoned stopped=$stopped")
        // An invite left half-way may be scanned again.
        handledInvitePayload = null
        _state.update {
            it.copy(
                pairingBusy = false,
                pairingPin = "",
                connectionState = if (it.connectionState == ConnectionState.Pairing) ConnectionState.Disconnected else it.connectionState,
            )
        }
    }

    /**
     * Closes a channel nobody owns: a launch connection that arrived late while a pairing was
     * running is kept for it, and must go once the pairing failed or was abandoned.
     */
    private fun releaseStrayChannel() {
        val s = _state.value
        if (s.connectedHost == null && liveLaunch == 0L && livePairing == 0L) client.close()
    }

    private fun deviceName(): String = DeviceName.get(getApplication())

    private suspend fun pairWithCandidates(
        candidates: List<DiscoveredHost>,
        deviceName: String,
        pairingCode: String,
        stillWanted: () -> Boolean,
    ): Result<Pair<TrustedHost, CapabilitySummary>> {
        var lastFailure: Throwable? = null
        var sawPolicyRejection = false
        for ((index, candidate) in candidates.distinctBy { "${it.address}:${it.port}" }.withIndex()) {
            if (!stillWanted()) return Result.failure(PairingCancelledException())
            Log.i(
                TAG,
                "qr_invite_connect_attempt index=$index address=${candidate.address}:${candidate.port} backend=${candidate.inputBackend}",
            )
            val result = runCatching {
                client.pair(
                    address = candidate.address,
                    port = candidate.port,
                    deviceName = deviceName,
                    pairingCode = pairingCode,
                    expectedFingerprint = candidate.fingerprint.ifBlank { null },
                )
            }
            if (result.isSuccess) return result
            lastFailure = result.exceptionOrNull()
            if (lastFailure is CancellationException || lastFailure is PairingCancelledException) return result
            val msg = lastFailure?.message.orEmpty()
            if (msg.contains("public_pairing_denied", ignoreCase = true) ||
                msg.contains("blocks pairing from public networks", ignoreCase = true)
            ) {
                sawPolicyRejection = true
                Log.w(
                    TAG,
                    "qr_invite_policy_rejected index=$index address=${candidate.address}:${candidate.port} message=$msg",
                )
            } else {
                Log.w(
                    TAG,
                    "qr_invite_connect_failed index=$index address=${candidate.address}:${candidate.port} message=${lastFailure?.message}",
                    lastFailure,
                )
            }
            _state.update {
                it.copy(status = "Invite endpoint ${candidate.address}:${candidate.port} failed; trying fallback...")
            }
        }
        if (sawPolicyRejection) {
            return Result.failure(
                GestoApiException(
                    code = "public_pairing_denied",
                    message = "Remote pairing blocked by host policy. " +
                        "The host admin must set allow_public_pairing=true or require_private_lan=false " +
                        "in ~/.config/gesto/config.json, then restart the daemon.",
                    retryable = false,
                ),
            )
        }
        return Result.failure(lastFailure ?: IllegalStateException("No usable invite endpoints"))
    }

    /**
     * Opens the paired PC with [id] through the launch screen (the PCs list, a launcher shortcut):
     * the session if it is the PC already connected, nothing new if it is already being opened.
     * A session with another PC is closed first.
     */
    fun connectById(id: String) {
        val s = _state.value
        val host = store.load().find { it.id == id }
        if (host == null) {
            Log.w(TAG, "connect_by_id unknown host")
            go(Screen.Home)
            return
        }
        when {
            s.connectedHost?.id == id -> go(s.lastMode.screen)
            liveLaunch != 0L && s.launchHost?.id == id -> go(Screen.Launch)
            else -> {
                if (s.connectedHost != null) performDisconnect(cancelReconnect = true)
                launchConnect(host)
            }
        }
    }

    /** "Back to the PC" on the PCs list: the session, on the mode last used. */
    fun resumeSession() {
        if (_state.value.connectedHost != null) go(_state.value.lastMode.screen)
    }

    fun disconnect() = performDisconnect(cancelReconnect = true)

    private fun performDisconnect(cancelReconnect: Boolean) {
        Log.i(TAG, "connection_state=disconnected user_requested=$cancelReconnect")
        if (cancelReconnect) reconnectJob?.cancel()
        stopScreenStream()
        interactionKeepAliveJob?.cancel()
        interactionEndJob?.cancel()
        activeInteractionSessionId = 0L
        // The next session decides the touchpad hints and the button's pulse again, and the
        // Automatic quality starts again from its best rung.
        touchpadHintsDecided = false
        quickMenuPulseDecided = false
        autoQuality.reset()
        pointerCarry.reset()
        savedStateHandle["connectedHostId"] = null
        savedStateHandle["screen"] = null
        // A lost link only moves the user out of the session; Settings or Add a PC stay put.
        val current = _state.value.screen
        val home = cancelReconnect || current.isSession || current == Screen.Launch
        _state.update {
            it.copy(
                connectionState = ConnectionState.Disconnected,
                connectedHost = null,
                status = "Disconnected",
                reconnecting = false,
                latencyMs = null,
                remoteInputSessionActive = false,
                remoteInputBacklog = 0,
                touchpadHints = false,
                externalInputStatus = externalInputSummary(it.externalInputDevices),
                screenSources = emptyList(),
                selectedScreenSourceId = null,
                screenStreamInfo = null,
                videoWidth = null,
                videoHeight = null,
                screenStreaming = false,
                screenConnectionState = RemoteScreenConnectionState.Idle,
                quickMenuPulse = false,
                autoQualityRung = 0,
                screenStatus = "Screen stream idle",
                screenError = null,
            )
        }
        clearQueuedInput()
        client.close()
        if (home) go(Screen.Home)
    }

    /** Forgets the paired PC with [id], closing the session first if it is the one connected. */
    fun forget(id: String) {
        if (_state.value.connectedHost?.id == id) performDisconnect(cancelReconnect = true)
        if (_state.value.launchHost?.id == id) go(Screen.Home)
        store.remove(id)
        if (retryHostId == id) retryHostId = null
        val hosts = store.load()
        _state.update { it.copy(trustedHosts = hosts, presence = presenceTracker.presence(hosts)) }
    }

    /** Gives the PC with [id] the name [name]; a blank name, or the PC's own, goes back to the PC's own. */
    fun rename(id: String, name: String) {
        val updated = store.update(id) { host ->
            val clean = name.trim().take(MAX_PC_NAME_LENGTH).takeIf { it.isNotEmpty() && it != host.hostName }
            host.copy(displayName = clean)
        } ?: return
        _state.update {
            it.copy(
                trustedHosts = store.load(),
                connectedHost = it.connectedHost?.let { host -> if (host.id == id) updated else host },
                launchHost = it.launchHost?.let { host -> if (host.id == id) updated else host },
            )
        }
    }

    /** Sends the Wake-on-LAN packet to every network card the PC with [id] reported. */
    fun wake(id: String) {
        val host = store.load().find { it.id == id } ?: return
        if (host.macAddresses.isEmpty()) return
        viewModelScope.launch {
            val targets = withContext(Dispatchers.IO) {
                runCatching { discovery.broadcastAddresses() }.getOrDefault(listOf("255.255.255.255"))
            }
            val sent = host.macAddresses.count { mac ->
                runCatching { WakeOnLan.send(mac, broadcast = targets) }
                    .onFailure { if (it is CancellationException) throw it else Log.w(TAG, "wake_failed", it) }
                    .isSuccess
            }
            Log.i(TAG, "wake host=${host.hostName} macs=${host.macAddresses.size} sent=$sent targets=${targets.size}")
            emitNotice(
                Notice(
                    if (sent > 0) UiText.Res(R.string.notice_waking, listOf(host.label))
                    else UiText.Res(R.string.notice_wake_failed, listOf(host.label)),
                ),
            )
        }
    }

    /**
     * Scans the network every [PRESENCE_INTERVAL_MS] while [on]: the PCs list turns it on while it is
     * on screen and the app is in the foreground, and off otherwise.
     */
    fun setPresenceScanning(on: Boolean) {
        if (!on) {
            if (presenceJob?.isActive == true) presencePausedAtMs = System.currentTimeMillis()
            presenceJob?.cancel()
            presenceJob = null
            return
        }
        if (presenceJob?.isActive == true) return
        // After a pause longer than a scan, what the cards said may no longer be true: start over
        // ("Looking...") rather than show a PC as on for the scans it takes to notice it is off.
        if (presencePausedAtMs != 0L && System.currentTimeMillis() - presencePausedAtMs > PRESENCE_INTERVAL_MS) {
            presenceTracker.reset()
            _state.update { it.copy(presence = presenceTracker.presence(it.trustedHosts)) }
        }
        presenceJob = viewModelScope.launch {
            runScans { found ->
                val hosts = _state.value.trustedHosts
                val seen = found.mapNotNullTo(HashSet()) { host -> host.fingerprint.takeIf { it.isNotBlank() } }
                val presence = presenceTracker.onScan(seen, hosts)
                _state.update { it.copy(presence = presence, scannedHosts = mergeSightings(it.scannedHosts, found)) }
            }
        }
    }

    /** Scans the network every [PRESENCE_INTERVAL_MS] until cancelled, handing each result to [onScan]. */
    private suspend fun runScans(onScan: (List<DiscoveredHost>) -> Unit) {
        while (true) {
            val startedAt = System.nanoTime()
            val found = try {
                discovery.discover(timeoutMillis = PRESENCE_LISTEN_MS)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // No network at all: nothing answered, which is what the count of misses is for.
                Log.d(TAG, "scan_failed ${e.message}")
                emptyList()
            }
            currentCoroutineContext().ensureActive()
            onScan(found)
            val elapsed = (System.nanoTime() - startedAt) / 1_000_000
            delay((PRESENCE_INTERVAL_MS - elapsed).coerceAtLeast(PRESENCE_MIN_GAP_MS))
        }
    }

    fun prepareInput() = launchCommand("Requesting portal approval...") {
        val fingerprint = _state.value.connectedHost?.fingerprint
        val ready = client.prepareInput()
        if (fingerprint != null) setInputApproved(fingerprint, ready)
    }

    /** Records whether the PC with [fingerprint] takes the phone's input without asking again. */
    private fun setInputApproved(fingerprint: String, approved: Boolean) {
        if (fingerprint.isBlank()) return
        if (approved != (fingerprint in _state.value.inputApprovedHosts)) {
            Log.i(TAG, "input_approval approved=$approved")
        }
        _state.update {
            it.copy(inputApprovedHosts = if (approved) it.inputApprovedHosts + fingerprint else it.inputApprovedHosts - fingerprint)
        }
    }

    fun beginRemoteInteraction(): Long {
        val sessionId = ++nextInteractionSessionId
        activeInteractionSessionId = sessionId
        interactionEndJob?.cancel()
        if (!_state.value.remoteInputSessionActive) {
            Log.d(TAG, "remote_interaction_start session=$sessionId")
        }
        _state.update { it.copy(remoteInputSessionActive = true) }
        startInteractionKeepAlive()
        return sessionId
    }

    fun endRemoteInteraction(sessionId: Long) {
        if (activeInteractionSessionId != sessionId) return
        interactionEndJob?.cancel()
        interactionEndJob = viewModelScope.launch {
            delay(INTERACTION_GRACE_MS)
            if (activeInteractionSessionId != sessionId) return@launch
            Log.d(TAG, "remote_interaction_end session=$sessionId")
            activeInteractionSessionId = 0L
            interactionKeepAliveJob?.cancel()
            _state.update { it.copy(remoteInputSessionActive = false) }
        }
    }

    fun notePointerCancellation(reason: String) {
        Log.w(TAG, "pointer_cancel reason=$reason")
    }

    fun notePointerFailure(throwable: Throwable) {
        Log.e(TAG, "pointer_failure", throwable)
    }

    /**
     * A finger move on the touchpad, scaled by the pointer speed and acceleration. Returns the move
     * queued for the PC, or null when none was (not connected, too much queued, not a number).
     */
    fun pointerMove(dx: Float, dy: Float): PointerDelta? {
        if (!dx.isFinite() || !dy.isFinite()) return null
        val s = _state.value
        val (sentX, sentY) = PointerAcceleration.apply(dx, dy, s.pointerSpeed, s.pointerAcceleration)
        return if (enqueueInput(RemoteInputCommand.PointerMove(sentX, sentY))) PointerDelta(sentX, sentY) else null
    }

    /** Sends a button change; false when it was dropped (no link, legacy backend, queue full). */
    fun pointerButton(button: PointerButton, state: ButtonState): Boolean {
        if (_state.value.capabilities.inputBackend == "hyprland-hyprctl") {
            if (state == ButtonState.Pressed) {
                _state.update { it.copy(status = "Legacy Hyprland fallback moves only the pointer; update waypad-daemon for IPC clicks.") }
            }
            return false
        }
        return enqueueInput(RemoteInputCommand.PointerButton(button, state))
    }

    fun releasePointerButtons() {
        enqueueInput(RemoteInputCommand.ReleasePointerButtons)
    }

    fun scroll(dx: Float, dy: Float, finish: Boolean = false) {
        if (_state.value.capabilities.inputBackend == "hyprland-hyprctl") {
            if (!finish) {
                _state.update { it.copy(status = "Legacy Hyprland fallback cannot scroll; update waypad-daemon for IPC scroll.") }
            }
            return
        }
        if (finish) {
            enqueueInput(RemoteInputCommand.ScrollFinish)
        } else if (dx.isFinite() && dy.isFinite()) {
            enqueueInput(RemoteInputCommand.Scroll(dx, dy))
        }
    }

    fun sendText(text: String) = launchCommand("Sending text...") { client.text(text) }

    fun sendLiveKeyboardEdit(previous: String, next: String) {
        val edit = liveEditOps(previous, next)
        if (edit.isEmpty) return
        keyboardQueue.trySend {
            repeat(edit.backspaces) { client.shortcut("backspace") }
            if (edit.insert.isNotEmpty()) client.text(edit.insert)
        }
    }

    /** One key press with its modifiers held, from the keyboard sheet; names as the daemon knows them. */
    fun sendKey(keys: List<String>) {
        if (keys.isNotEmpty()) keyboardQueue.trySend { client.shortcut(*keys.toTypedArray()) }
    }

    fun media(action: String) = launchCommand(null) { client.media(action) }

    fun volume(action: String) = launchCommand(null) { client.volume(action) }

    fun brightness(action: String) = launchCommand(null) { client.brightness(action) }

    fun system(action: String) = launchCommand(null) {
        client.system(action)
        emitNotice(Notice(UiText.Res(if (action == "suspend") R.string.notice_pc_suspending else R.string.notice_pc_locked)))
    }

    /** A shortcut tapped on the Commands screen; [notice] is the string resource of the short message after it. */
    fun shortcut(keys: List<String>, @StringRes notice: Int) = launchCommand(null) {
        client.shortcut(*keys.toTypedArray())
        emitNotice(Notice(UiText.Res(notice)))
    }

    /** Starts one of the apps the PC offers (`capabilities.apps`). */
    fun launchApp(app: RemoteApp) = launchCommand(null) {
        client.launchApp(app.id)
        emitNotice(Notice(UiText.Res(R.string.notice_app_starting, listOf(app.name))))
    }

    fun clipboard(text: String) = launchCommand("Sending clipboard text...") { client.clipboard(text) }

    fun refreshCapabilities() {
        launchCommand("Refreshing capabilities...") {
            val capabilities = client.capabilities()
            _state.update { it.copy(capabilities = capabilities, status = "Capabilities refreshed") }
        }
    }

    fun loadScreenSources() {
        launchCommand("Loading screen sources...") {
            _state.update { it.copy(screenSourcesLoading = true) }
            val sources = try {
                client.listScreenSources()
            } finally {
                _state.update { it.copy(screenSourcesLoading = false) }
            }
            _state.update { current ->
                // No choice is made here: without one the PC streams the screen it remembers.
                val selected = current.selectedScreenSourceId?.takeIf { id -> sources.any { it.id == id } }
                current.copy(
                    screenSources = sources,
                    selectedScreenSourceId = selected,
                    screenStatus = if (sources.isEmpty()) "No screen sources available" else "Found ${sources.size} screen source(s)",
                    screenError = null,
                )
            }
        }
    }

    /** The screen the next stream shows; null leaves the choice to the PC (the one it remembers). */
    fun selectScreenSource(sourceId: String?) {
        _state.update {
            it.copy(
                selectedScreenSourceId = sourceId,
                screenStatus = if (sourceId == null) {
                    "Source chosen by the PC"
                } else {
                    "Selected ${it.screenSources.firstOrNull { source -> source.id == sourceId }?.label ?: sourceId}"
                },
            )
        }
    }

    fun startScreenStream() {
        val host = _state.value.connectedHost ?: run {
            _state.update { it.copy(screenError = "Connect to a host before starting a screen stream.") }
            return
        }
        val existingSessionId = _state.value.screenStreamInfo?.sessionId
        screenStreamDesired = true
        screenStreamJob?.cancel()
        screenStreamJob = viewModelScope.launch {
            if (existingSessionId != null) {
                stopScreenStreamOnHost(existingSessionId, "before_restart")
            }
            _state.update {
                it.copy(
                    screenStreaming = true,
                    videoWidth = null,
                videoHeight = null,
                    screenStreamStats = ScreenStreamStats(),
                    screenStatus = "Starting screen stream...",
                    screenConnectionState = transitionScreenSession(RemoteScreenSessionEvent.Start),
                    screenError = null,
                )
            }
            val selected = _state.value.selectedScreenSourceId
            val settings = _state.value.streamingSettings
            streamShownAtMs = 0L

            Log.i(
                TAG,
                "stream_settings_apply preset=${settings.preset} rung=${_state.value.autoQualityRung} fps=${settings.maxFps} quality=${settings.jpegQuality} max=${settings.maxDimension}",
            )
            var attempt = 0
            while (isActive && screenStreamDesired && attempt < SCREEN_STREAM_MAX_RETRIES) {
                attempt += 1
                var info: ScreenStreamInfo? = null
                val result = runCatching {
                    if (attempt > 1) {
                        _state.update {
                            it.copy(
                                screenConnectionState = transitionScreenSession(RemoteScreenSessionEvent.Retry),
                                screenStatus = "Reconnecting screen stream ($attempt/$SCREEN_STREAM_MAX_RETRIES)...",
                            )
                        }
                        delay(500L * attempt)
                    }
                    val streamInfo = client.startScreenStream(
                        sourceId = selected,
                        maxFps = settings.maxFps,
                        jpegQuality = settings.jpegQuality,
                        maxWidth = settings.maxWidth,
                        maxHeight = settings.maxHeight,
                    )
                    info = streamInfo
                    Log.i(TAG, "screen_stream_start attempt=$attempt session=${streamInfo.sessionId} source=${streamInfo.source.id} port=${streamInfo.streamPort} actualFps=${streamInfo.actualFps} actualQuality=${streamInfo.actualQuality}")
                    _state.update {
                        it.copy(
                            screenStreamInfo = streamInfo,
                            selectedScreenSourceId = streamInfo.source.id,
                            screenConnectionState = transitionScreenSession(RemoteScreenSessionEvent.Negotiated),
                            screenStatus = "Connecting ${streamInfo.source.backend} stream...",
                            screenError = null,
                            screenStreamStats = ScreenStreamStats(
                                targetFps = settings.maxFps,
                                actualFps = streamInfo.actualFps,
                                backend = streamInfo.source.backend,
                            ),
                        )
                    }
                    videoSession.updateSessionInfo(
                        targetFps = settings.maxFps,
                        actualFps = streamInfo.actualFps,
                        backend = streamInfo.source.backend,
                    )
                    // Suspends until the stream ends or throws; frames go straight from the socket
                    // to MediaCodec and never pass through this state holder.
                    videoSession.collect(
                        host = host.address,
                        port = streamInfo.streamPort,
                        token = streamInfo.token,
                        transport = streamInfo.transport,
                    )
                }
                info?.sessionId?.let { sessionId ->
                    stopScreenStreamOnHost(sessionId, "cleanup")
                }
                result.onFailure { throwable ->
                    if (throwable is kotlinx.coroutines.CancellationException) throw throwable
                    if (!screenStreamDesired || !isActive) {
                        Log.i(TAG, "screen_stream_failure_ignored_after_stop message=${throwable.message}")
                        return@onFailure
                    }
                    Log.w(TAG, "screen_stream_failed attempt=$attempt desired=$screenStreamDesired", throwable)
                    val message = throwable.message ?: throwable::class.java.simpleName
                    val failedState = transitionScreenSession(RemoteScreenSessionEvent.Fail)
                    val willRetry = attempt < SCREEN_STREAM_MAX_RETRIES && screenStreamDesired
                    if (willRetry) {
                        viewModelScope.launch {
                            if (reconnectCurrentHost()) {
                                Log.i(TAG, "screen_stream_reconnect_succeeded before retry")
                            } else {
                                Log.w(TAG, "screen_stream_reconnect_failed before retry")
                            }
                        }
                    }
                    _state.update {
                        it.copy(
                            screenStreaming = willRetry,
                            screenStreamInfo = null,
                            screenConnectionState = failedState,
                            screenStatus = if (willRetry) {
                                "Screen stream dropped; retrying..."
                            } else {
                                "Screen stream stopped"
                            },
                            screenError = message,
                        )
                    }
                }.onSuccess {
                    Log.i(TAG, "screen_stream_closed attempt=$attempt desired=$screenStreamDesired")
                    if (screenStreamDesired && isActive) {
                        _state.update {
                            it.copy(
                                screenStreaming = false,
                                screenStreamInfo = null,
                                screenConnectionState = transitionScreenSession(RemoteScreenSessionEvent.Retry),
                                screenStatus = "Screen stream closed; reconnecting...",
                            )
                        }
                    }
                }
            }
            if (!screenStreamDesired) {
                _state.update {
                    it.copy(
                        screenStreaming = false,
                        screenStreamInfo = null,
                        screenConnectionState = transitionScreenSession(RemoteScreenSessionEvent.Close),
                        screenStatus = "Screen stream idle",
                    )
                }
            } else if (attempt >= SCREEN_STREAM_MAX_RETRIES) {
                // Given up: no stream is wanted any more, so the next reconnect may start one again.
                screenStreamDesired = false
                _state.update {
                    it.copy(
                        screenStreaming = false,
                        screenStreamInfo = null,
                        screenConnectionState = RemoteScreenConnectionState.Failed,
                        screenStatus = "Screen stream failed after retries",
                    )
                }
            }
        }
    }

    fun stopScreenStream() {
        screenStreamDesired = false
        val sessionId = _state.value.screenStreamInfo?.sessionId
        screenStreamJob?.cancel()
        screenStreamJob = null
        if (sessionId != null) {
            viewModelScope.launch {
                stopScreenStreamOnHost(sessionId, "user_stop")
            }
        }
        _state.update {
            it.copy(
                screenStreaming = false,
                screenStreamInfo = null,
                screenConnectionState = transitionScreenSession(RemoteScreenSessionEvent.Close),
                screenStatus = "Screen stream idle",
                screenStreamStats = ScreenStreamStats(),
            )
        }
    }

    private suspend fun stopScreenStreamOnHost(sessionId: String, reason: String) {
        withContext(NonCancellable) {
            runCatching { client.stopScreenStream(sessionId) }
                .onSuccess { Log.i(TAG, "screen_stream_stop_sent reason=$reason session=$sessionId") }
                .onFailure { Log.w(TAG, "screen_stream_stop_failed reason=$reason session=$sessionId", it) }
        }
    }

    private fun transitionScreenSession(event: RemoteScreenSessionEvent): RemoteScreenConnectionState {
        val next = screenSessionMachine.transition(event)
        Log.d(TAG, "screen_session_state event=$event state=$next")
        return next
    }

    fun remoteScreenPointerMove(x: Float, y: Float) {
        if (!x.isFinite() || !y.isFinite()) return
        enqueueInput(RemoteInputCommand.PointerMoveAbsolute(activeScreenSourceId(), x, y))
    }

    fun remoteScreenClick(x: Float, y: Float, button: PointerButton = PointerButton.Left) {
        remoteScreenPointerMove(x, y)
        pointerButton(button, ButtonState.Pressed)
        pointerButton(button, ButtonState.Released)
    }

    /**
     * Mirrors the decoder session into UI state.
     *
     * Statistics and resolution are produced by the video pipeline itself, so the ViewModel only
     * republishes them. The first decoded frame is what moves the session out of `Negotiating`.
     */
    private fun observeVideoSession() {
        videoSession.onFirstFrame = {
            _state.update {
                it.copy(
                    screenStreaming = true,
                    screenConnectionState = transitionScreenSession(RemoteScreenSessionEvent.FirstFrame),
                    screenError = null,
                )
            }
            // Called on the stream's thread; the Automatic quality lives on the main one.
            viewModelScope.launch {
                streamShownAtMs = monotonicMs()
                autoQuality.onStreamRestarted()
                streamDelay.reset()
            }
        }
        viewModelScope.launch {
            videoSession.stats.collect { stats ->
                _state.update { current ->
                    current.copy(
                        screenStreamStats = stats,
                        screenStatus = if (current.screenStreaming) {
                            streamStatusFor(current.videoWidth, current.videoHeight, stats)
                        } else {
                            current.screenStatus
                        },
                    )
                }
                judgeAutoQuality(stats)
            }
        }
        viewModelScope.launch {
            // Deliberately the source size, not the encoded size: the stream is usually
            // downscaled, and absolute pointer commands are in desktop pixels.
            videoSession.sourceSize.collect { size ->
                val width = size.width.takeIf { it > 0 }
                val height = size.height.takeIf { it > 0 }
                _state.update { it.copy(videoWidth = width, videoHeight = height) }
            }
        }
        val audio = videoSession.renderer.audio
        audio.attachAudioFocus(getApplication())
        audio.onMuteChanged = { muted ->
            // Playback is already silent locally; this only tells the host to stop sending.
            val sessionId = _state.value.screenStreamInfo?.sessionId
            if (sessionId != null) {
                viewModelScope.launch {
                    runCatching { client.setDesktopAudioMute(sessionId, muted) }
                        .onFailure { Log.w(TAG, "desktop_audio_mute_failed muted=$muted", it) }
                }
            }
        }
        viewModelScope.launch {
            audio.muted.collect { muted -> _state.update { it.copy(audioMuted = muted) } }
        }
        viewModelScope.launch {
            videoSession.renderError.collect { error ->
                if (error != null) {
                    Log.w(TAG, "screen_stream_render_error message=$error")
                    _state.update { it.copy(screenError = error) }
                }
            }
        }
    }

    /**
     * The Automatic quality, fed with the frame rate of the stream on screen once it has warmed up.
     * The PC sends a frame only when its screen changes, so a frame rate below the target counts
     * only while the frames also arrive late; on time, the network is keeping up, whatever the
     * rate. On a step down the stream starts again with the next rung.
     */
    private fun judgeAutoQuality(stats: ScreenStreamStats) {
        val s = _state.value
        if (s.streamSettings.preset != QualityPreset.Auto || stats.targetFps <= 0) return
        if (s.screenConnectionState != RemoteScreenConnectionState.Streaming || streamShownAtMs == 0L) return
        val now = monotonicMs()
        // The quickest frame is looked for from the start: the first ones, before any queue
        // builds up, are the best reference.
        val late = streamDelay.isLate(stats.lastFrameAgeMs)
        if (now - streamShownAtMs < AUTO_QUALITY_WARMUP_MS) return
        val delivered = if (late) stats.deliveredFps else stats.targetFps.toDouble()
        val rung = autoQuality.onStats(delivered, stats.targetFps, now) ?: return
        Log.i(TAG, "auto_quality_lowered rung=$rung delivered=${stats.deliveredFps.formatFps()} target=${stats.targetFps}")
        _state.update { it.copy(autoQualityRung = rung) }
        emitNotice(Notice(UiText.Res(R.string.notice_quality_lowered)))
        startScreenStream()
    }

    private fun monotonicMs(): Long = System.nanoTime() / 1_000_000L

    private fun streamStatusFor(width: Int?, height: Int?, stats: ScreenStreamStats): String {
        val backend = stats.backend.takeIf { it.isNotBlank() } ?: "unknown"
        val resolution = if (width != null && height != null) "${width}x$height" else "connecting"
        val codec = stats.codec.takeIf { it.isNotBlank() }
        return if (_state.value.streamSettings.showStats) {
            val fpsLabel = if (stats.actualFps > 0 && stats.actualFps != stats.targetFps) {
                "${stats.deliveredFps.formatFps()}/${stats.actualFps} fps (target ${stats.targetFps})"
            } else {
                "${stats.deliveredFps.formatFps()}/${stats.targetFps} fps"
            }
            val parts = listOfNotNull(
                "Live $resolution",
                codec,
                backend,
                fpsLabel,
                "${stats.kilobitsPerSecond} kbps",
            )
            parts.joinToString(" | ")
        } else {
            listOfNotNull("Live $resolution", codec, backend).joinToString(" · ")
        }
    }

    private fun activeScreenSourceId(): String? =
        _state.value.screenStreamInfo?.source?.id ?: _state.value.selectedScreenSourceId

    private fun launchCommand(status: String?, block: suspend () -> Unit) {
        if (status != null) _state.update { it.copy(status = status, error = null) }
        viewModelScope.launch {
            val result = runCatching { block() }
                .recoverCatching { throwable ->
                    if (throwable is CancellationException) throw throwable
                    // Re-run only if the request never reached the PC; after a read failure it may
                    // already have run.
                    val canRetry = throwable.isTransportFailure() && !throwable.mayHaveRun()
                    if (canRetry && reconnectCurrentHost()) block() else throw throwable
                }
            if (status != null) _state.update { it.copy(status = "Ready") }
            result.onFailure { reportCommandFailure(it) }
        }
    }

    private fun launchQuiet(block: suspend () -> Unit) {
        viewModelScope.launch { runQuiet(block) }
    }

    private suspend fun runQuiet(block: suspend () -> Unit) {
        runCatching { block() }
            .onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                if (throwable.isTransportFailure()) {
                    Log.w(TAG, "transport_failure quiet_command reconnecting=true", throwable)
                    reconnectCurrentHost()
                } else {
                    reportCommandFailure(throwable)
                }
            }
    }

    /** A failed command never touches the connection state: the user just gets a message. */
    private fun reportCommandFailure(throwable: Throwable) {
        if (throwable is CancellationException) return
        Log.w(TAG, "command_failed", throwable)
        if (ErrorTranslator.isInputApprovalMissing(throwable)) {
            // The PC forgot the approval (its daemon restarted): the touchpad asks for it again.
            _state.value.connectedHost?.fingerprint?.let { setInputApproved(it, false) }
        }
        emitNotice(Notice(ErrorTranslator.translate(throwable, ErrorContext.Command, currentPort())))
    }

    private fun currentPort(): Int = _state.value.connectedHost?.port ?: 47771

    /** Emits a notice, skipping an identical one that was just shown (see [shouldEmitNotice]). */
    private fun emitNotice(notice: Notice) {
        val now = System.currentTimeMillis()
        if (!shouldEmitNotice(lastNotice, lastNoticeAtMs, notice, now)) return
        lastNotice = notice
        lastNoticeAtMs = now
        _notices.trySend(notice)
    }

    /**
     * Re-establishes the session after a transport failure, silently, within the policy's budget.
     * Concurrent callers share one attempt. Returns true when the session is back.
     */
    private suspend fun reconnectCurrentHost(): Boolean {
        if (_state.value.connectedHost == null) return false
        val job = reconnectJob?.takeIf { it.isActive }
            ?: viewModelScope.async { runReconnectLoop() }.also { reconnectJob = it }
        return try {
            job.await()
        } catch (e: CancellationException) {
            currentCoroutineContext().ensureActive()
            false
        }
    }

    private suspend fun runReconnectLoop(): Boolean {
        val host = _state.value.connectedHost ?: return false
        Log.i(TAG, "connection_state=reconnecting host=${host.hostName}")
        _state.update { it.copy(reconnecting = true) }
        val startedAt = System.nanoTime()
        var attempt = 0
        var lastError: Throwable? = null
        while (true) {
            val elapsed = (System.nanoTime() - startedAt) / 1_000_000
            val wait = reconnectPolicy.nextDelay(attempt, elapsed) ?: break
            if (wait > 0) delay(wait)
            val result = runCatching { client.connect(host) }
            val connected = result.getOrNull()
            if (connected != null) {
                val capabilities = connected.capabilities
                Log.i(TAG, "connection_state=reconnected host=${host.hostName} input_backend=${capabilities.inputBackend}")
                savedStateHandle["connectedHostId"] = host.id
                val updated = rememberConnection(host, connected, touch = false)
                val stored = updated !== host
                _state.update {
                    it.copy(
                        trustedHosts = if (stored) store.load() else it.trustedHosts,
                        // Only a stored change replaces the session's PC: it may have been renamed meanwhile.
                        connectedHost = if (stored && it.connectedHost?.id == updated.id) updated else it.connectedHost,
                        capabilities = capabilities,
                        connectionState = ConnectionState.Connected,
                        status = "Reconnected to ${host.hostName}",
                        error = null,
                        reconnecting = false,
                    )
                }
                publishCurrentExternalDevices()
                autoStartScreenStream()
                emitNotice(Notice(UiText.Res(R.string.notice_reconnected)))
                return true
            }
            val failure = result.exceptionOrNull()!!
            if (failure is CancellationException) throw failure
            lastError = failure
            Log.w(TAG, "reconnect_attempt_failed attempt=$attempt", failure)
            if (!failure.isTransportFailure()) break
            attempt++
        }
        // Gave up: back to the list, with a message that says why.
        val text = daemonVerdict(lastError, host.port)
            ?: UiText.Res(R.string.error_connection_lost, listOf(host.label))
        performDisconnect(cancelReconnect = false)
        emitNotice(Notice(text, NoticeAction.Retry))
        return false
    }

    /**
     * The daemon's own reason when it turned the connection down (session expired, identity
     * changed...); null when the PC simply did not answer.
     */
    private fun daemonVerdict(error: Throwable?, port: Int): UiText? = error
        ?.takeIf { it is GestoApiException || it.message?.contains("fingerprint", ignoreCase = true) == true }
        ?.let { ErrorTranslator.translate(it, ErrorContext.Connecting, port) }

    private fun connectFailureText(host: TrustedHost, error: Throwable?): UiText =
        daemonVerdict(error, host.port) ?: UiText.Res(R.string.notice_pc_not_answering, listOf(host.label))

    /** Pings the PC every few seconds while connected and idle; two misses in a row mean a dead link. */
    private suspend fun runHealthPing() {
        var failures = 0
        while (true) {
            delay(HEALTH_PING_INTERVAL_MS)
            val s = _state.value
            val idle = System.currentTimeMillis() - lastInputAtMs >= HEALTH_PING_IDLE_MS
            if (s.connectionState != ConnectionState.Connected || s.connectedHost == null || s.reconnecting || !idle) {
                failures = 0
                continue
            }
            val startedAt = System.nanoTime()
            val result = runCatching { client.ping() }
            if (result.exceptionOrNull() is CancellationException) throw result.exceptionOrNull()!!
            if (result.isSuccess) {
                failures = 0
                val ms = ((System.nanoTime() - startedAt) / 1_000_000).toInt()
                _state.update { it.copy(latencyMs = ms) }
            } else if (++failures >= HEALTH_PING_MAX_FAILURES) {
                Log.w(TAG, "health_ping_failed count=$failures", result.exceptionOrNull())
                failures = 0
                _state.update { it.copy(latencyMs = null) }
                reconnectCurrentHost()
            }
        }
    }

    /** Queues [command] for the PC; false when it was dropped. */
    private fun enqueueInput(command: RemoteInputCommand): Boolean {
        lastInputAtMs = System.currentTimeMillis()
        val current = _state.value
        if (current.connectedHost == null || current.connectionState == ConnectionState.Disconnected) {
            Log.d(TAG, "input_queue_drop_disconnected command=$command")
            return false
        }
        val queued = queuedInputCommands.get()
        if (command.isRealtime && queued >= REALTIME_INPUT_BACKLOG_HIGH_WATERMARK) {
            Log.w(TAG, "input_queue_drop_stale_realtime depth=$queued command=$command")
            return false
        }
        val result = inputCommands.trySend(command)
        if (result.isSuccess) {
            val depth = queuedInputCommands.incrementAndGet()
            updateInputBacklog(depth)
            return true
        } else if (command.isTerminal) {
            Log.w(TAG, "input_queue_backpressure command=$command preserving_terminal=true")
            viewModelScope.launch {
                runCatching { inputCommands.send(command) }
                    .onSuccess {
                        val depth = queuedInputCommands.incrementAndGet()
                        updateInputBacklog(depth)
                    }
                    .onFailure {
                        Log.w(TAG, "input_queue_rejected command=$command cause=${it.message}")
                    }
            }
            return true
        } else {
            Log.w(TAG, "input_queue_drop_backpressure command=$command cause=${result.exceptionOrNull()?.message}")
            return false
        }
    }

    /**
     * Drops whatever input is still waiting: gestures, and keys and text from the keyboard sheet.
     * Called when the session ends or moves to another PC, so nothing typed during an outage is
     * typed into a PC after the link comes back.
     */
    private fun clearQueuedInput() {
        while (inputCommands.tryReceive().isSuccess) {
            // Drain stale gesture commands after explicit disconnect so they cannot reconnect or
            // mutate UI state after the user has left the remote session.
        }
        var droppedKeys = 0
        while (keyboardQueue.tryReceive().isSuccess) droppedKeys++
        if (droppedKeys > 0) Log.i(TAG, "keyboard_queue_dropped count=$droppedKeys")
        queuedInputCommands.set(0)
        updateInputBacklog(0)
    }

    private fun startInteractionKeepAlive() {
        if (interactionKeepAliveJob?.isActive == true) return
        interactionKeepAliveJob = viewModelScope.launch {
            delay(INTERACTION_KEEPALIVE_INITIAL_MS)
            while (isActive && _state.value.remoteInputSessionActive) {
                enqueueInput(RemoteInputCommand.Ping)
                delay(INTERACTION_KEEPALIVE_INTERVAL_MS)
            }
        }
    }

    private suspend fun drainInputCommands() {
        val pending = ArrayDeque<RemoteInputCommand>()
        while (true) {
            val command = if (pending.isNotEmpty()) {
                pending.removeFirst()
            } else {
                inputCommands.receiveCatching().getOrNull()?.also { markInputDequeued() } ?: break
            }
            if (_state.value.connectedHost == null ||
                _state.value.connectionState == ConnectionState.Disconnected ||
                _state.value.connectionState == ConnectionState.Error
            ) {
                Log.d(TAG, "input_queue_skip_disconnected command=$command")
                continue
            }
            handleInputCommand(command, pending)
        }
    }

    private suspend fun handleInputCommand(
        command: RemoteInputCommand,
        pending: ArrayDeque<RemoteInputCommand>,
    ) {
        when (command) {
            is RemoteInputCommand.PointerMove -> {
                var dx = command.dx
                var dy = command.dy
                while (true) {
                    val next = pollInputCommand() ?: break
                    if (next is RemoteInputCommand.PointerMove) {
                        dx += next.dx
                        dy += next.dy
                    } else {
                        pending.addFirst(next)
                        break
                    }
                }
                val (wholeX, wholeY) = pointerCarry.take(dx, dy)
                if (wholeX != 0 || wholeY != 0) {
                    Log.v(TAG, "transport_send type=pointer_move dx=$wholeX dy=$wholeY")
                    sendInput("pointer_move") { client.pointerMove(wholeX.toFloat(), wholeY.toFloat()) }
                }
            }
            is RemoteInputCommand.PointerMoveAbsolute -> {
                var latest: RemoteInputCommand.PointerMoveAbsolute = command
                while (true) {
                    val next = pollInputCommand() ?: break
                    if (next is RemoteInputCommand.PointerMoveAbsolute && next.sourceId == latest.sourceId) {
                        latest = next
                    } else {
                        pending.addFirst(next)
                        break
                    }
                }
                Log.v(TAG, "transport_send type=pointer_move_absolute source=${latest.sourceId} x=${latest.x} y=${latest.y}")
                sendInput("pointer_move_absolute") {
                    client.pointerMoveAbsolute(latest.sourceId, latest.x, latest.y)
                }
            }
            is RemoteInputCommand.Scroll -> {
                var dx = command.dx
                var dy = command.dy
                while (true) {
                    val next = pollInputCommand() ?: break
                    if (next is RemoteInputCommand.Scroll) {
                        dx += next.dx
                        dy += next.dy
                    } else {
                        pending.addFirst(next)
                        break
                    }
                }
                if (abs(dx) + abs(dy) > 0.01f) {
                    Log.v(TAG, "transport_send type=scroll dx=$dx dy=$dy")
                    sendInput("scroll") { client.scroll(dx, dy, finish = false) }
                }
            }
            RemoteInputCommand.ScrollFinish -> {
                Log.d(TAG, "transport_send type=scroll_finish")
                sendInput("scroll_finish") { client.scroll(0f, 0f, finish = true) }
            }
            is RemoteInputCommand.PointerButton -> {
                Log.d(TAG, "transport_send type=pointer_button button=${command.button.wireName} state=${command.state.wireName}")
                sendInput("pointer_button") { client.pointerButton(command.button, command.state) }
            }
            RemoteInputCommand.ReleasePointerButtons -> {
                Log.d(TAG, "transport_send type=release_pointer_buttons")
                // Queued at the end of every touchpad gesture: what is left of a pixel is not carried into the next one.
                pointerCarry.reset()
                sendInput("release_pointer_buttons") {
                    client.pointerButton(PointerButton.Left, ButtonState.Released)
                    client.pointerButton(PointerButton.Right, ButtonState.Released)
                    client.pointerButton(PointerButton.Middle, ButtonState.Released)
                }
            }
            RemoteInputCommand.Ping -> {
                Log.v(TAG, "transport_send type=ping")
                sendInput("ping") { client.ping() }
            }
            is RemoteInputCommand.External -> {
                when (val external = command.event) {
                    is ExternalInputEvent.PointerMove -> {
                        var dx = external.dx
                        var dy = external.dy
                        while (true) {
                            val next = pollInputCommand() ?: break
                            val nextEvent = (next as? RemoteInputCommand.External)?.event
                            if (nextEvent is ExternalInputEvent.PointerMove &&
                                nextEvent.deviceId == external.deviceId
                            ) {
                                dx += nextEvent.dx
                                dy += nextEvent.dy
                            } else {
                                pending.addFirst(next)
                                break
                            }
                        }
                        val coalesced = external.copy(dx = dx, dy = dy)
                        Log.v(TAG, "transport_send type=external_pointer_move device=${coalesced.deviceId} dx=$dx dy=$dy")
                        sendInput("external_pointer_move") { client.externalInput(coalesced) }
                    }
                    is ExternalInputEvent.PointerScroll -> {
                        var dx = external.dx
                        var dy = external.dy
                        var finish = external.finish
                        while (true) {
                            val next = pollInputCommand() ?: break
                            val nextEvent = (next as? RemoteInputCommand.External)?.event
                            if (nextEvent is ExternalInputEvent.PointerScroll &&
                                nextEvent.deviceId == external.deviceId
                            ) {
                                dx += nextEvent.dx
                                dy += nextEvent.dy
                                finish = finish || nextEvent.finish
                            } else {
                                pending.addFirst(next)
                                break
                            }
                        }
                        val coalesced = external.copy(dx = dx, dy = dy, finish = finish)
                        Log.v(TAG, "transport_send type=external_pointer_scroll device=${coalesced.deviceId} dx=$dx dy=$dy finish=$finish")
                        sendInput("external_pointer_scroll") { client.externalInput(coalesced) }
                    }
                    is ExternalInputEvent.ControllerAxis -> {
                        val latestByAxis = linkedMapOf(external.axis to external)
                        while (true) {
                            val next = pollInputCommand() ?: break
                            val nextEvent = (next as? RemoteInputCommand.External)?.event
                            if (nextEvent is ExternalInputEvent.ControllerAxis &&
                                nextEvent.deviceId == external.deviceId
                            ) {
                                latestByAxis[nextEvent.axis] = nextEvent
                            } else {
                                pending.addFirst(next)
                                break
                            }
                        }
                        if (latestByAxis.size > 1) {
                            Log.d(
                                TAG,
                                "input_queue_coalesce_controller_axes device=${external.deviceId} axes=${latestByAxis.keys} count=${latestByAxis.size}",
                            )
                        }
                        latestByAxis.values.forEach { latest ->
                            Log.v(TAG, "transport_send type=external_controller_axis device=${latest.deviceId} axis=${latest.axis} value=${latest.value}")
                            sendInput("external_controller_axis") { client.externalInput(latest) }
                        }
                    }
                    else -> {
                        Log.d(TAG, "transport_send type=external_input event=${external::class.simpleName} device=${external.deviceId}")
                        sendInput("external_input") { client.externalInput(external) }
                    }
                }
            }
        }
    }

    private fun pollInputCommand(): RemoteInputCommand? {
        val command = inputCommands.tryReceive().getOrNull() ?: return null
        markInputDequeued()
        return command
    }

    private fun markInputDequeued() {
        val depth = queuedInputCommands.updateAndGet { (it - 1).coerceAtLeast(0) }
        updateInputBacklog(depth)
    }

    private fun updateInputBacklog(depth: Int) {
        val current = _state.value.remoteInputBacklog
        if (depth == 0 || abs(depth - current) >= 8) {
            _state.update { it.copy(remoteInputBacklog = depth) }
        }
    }

    private suspend fun sendInput(label: String, block: suspend () -> Unit) {
        lastInputAtMs = System.currentTimeMillis()
        runCatching { block() }
            .onFailure { throwable ->
                if (throwable is CancellationException) throw throwable
                Log.w(TAG, "transport_send_failed type=$label", throwable)
                when {
                    !throwable.isTransportFailure() -> reportCommandFailure(throwable)
                    // The request may already have run on the PC: bring the link back, but never
                    // send it twice (a doubled click or a doubled letter).
                    throwable.mayHaveRun() -> reconnectCurrentHost()
                    reconnectCurrentHost() -> runCatching { block() }.onFailure { reportCommandFailure(it) }
                }
            }
    }

    /** Flips desktop audio on the phone, and tells the host to stop sending while it is off. */
    fun toggleAudioMute() {
        videoSession.renderer.audio.toggleMute()
    }

    override fun onCleared() {
        inputCommands.close()
        interactionKeepAliveJob?.cancel()
        interactionEndJob?.cancel()
        screenStreamJob?.cancel()
        videoSession.release()
        // A launch connect may still be blocked on its socket: stop waiting for it, and make the
        // client drop whatever channel it opens from now on.
        liveLaunch = 0L
        launchTimeoutJob?.cancel()
        presenceJob?.cancel()
        addPcScanJob?.cancel()
        livePairing = 0L
        client.onOneWayError = null
        client.shutdown()
        super.onCleared()
    }
}

private fun shouldCaptureExternalInput(state: GestoUiState): Boolean =
    state.connectionState == ConnectionState.Connected &&
        (state.screen == Screen.Touchpad || state.screen == Screen.Display)

private fun shouldForwardExternalDeviceConnected(
    state: GestoUiState,
    device: ExternalInputDeviceSummary,
): Boolean =
    if (device.classes.any { it == ExternalInputDeviceClass.Gamepad || it == ExternalInputDeviceClass.Joystick }) {
        shouldForwardControllerInput(state)
    } else {
        shouldCaptureExternalInput(state)
    }

private fun shouldForwardExternalInput(state: GestoUiState, event: ExternalInputEvent): Boolean =
    shouldCaptureExternalInput(state) &&
        (!event.isControllerLike() || shouldForwardControllerInput(state))

private fun shouldForwardControllerInput(state: GestoUiState): Boolean =
    state.connectionState == ConnectionState.Connected && state.remoteScreenFullscreen

private fun shouldConsumeControllerLocally(state: GestoUiState, event: ExternalInputEvent): Boolean =
    state.connectionState == ConnectionState.Connected && event.isControllerLike()

private fun ExternalInputEvent.isControllerLike(): Boolean =
    when (this) {
        is ExternalInputEvent.ControllerButton,
        is ExternalInputEvent.ControllerAxis -> true
        is ExternalInputEvent.DeviceConnected,
        is ExternalInputEvent.DeviceDisconnected -> deviceType == ExternalInputDeviceClass.Gamepad ||
            deviceType == ExternalInputDeviceClass.Joystick
        else -> deviceType == ExternalInputDeviceClass.Gamepad ||
            deviceType == ExternalInputDeviceClass.Joystick
    }

private fun isExternalInputSupported(state: GestoUiState, event: ExternalInputEvent): Boolean =
    when (event) {
        is ExternalInputEvent.DeviceConnected,
        is ExternalInputEvent.DeviceDisconnected -> true
        is ExternalInputEvent.PointerMove,
        is ExternalInputEvent.PointerButton,
        is ExternalInputEvent.PointerScroll -> state.capabilities.externalPointerSupported
        is ExternalInputEvent.KeyboardKey -> state.capabilities.externalKeyboardSupported
        is ExternalInputEvent.ControllerButton,
        is ExternalInputEvent.ControllerAxis -> state.capabilities.externalControllerSupported
    }

private fun ExternalInputDeviceClass.unsupportedLabel(): String = when (this) {
    ExternalInputDeviceClass.Mouse,
    ExternalInputDeviceClass.Touchpad -> "external pointer"
    ExternalInputDeviceClass.Keyboard -> "external keyboard"
    ExternalInputDeviceClass.Gamepad,
    ExternalInputDeviceClass.Joystick -> "controller"
    ExternalInputDeviceClass.Unknown -> "external input"
}

private fun Set<ExternalInputDeviceClass>.primaryTypeForStatus(): ExternalInputDeviceClass =
    when {
        ExternalInputDeviceClass.Mouse in this -> ExternalInputDeviceClass.Mouse
        ExternalInputDeviceClass.Touchpad in this -> ExternalInputDeviceClass.Touchpad
        ExternalInputDeviceClass.Gamepad in this -> ExternalInputDeviceClass.Gamepad
        ExternalInputDeviceClass.Joystick in this -> ExternalInputDeviceClass.Joystick
        ExternalInputDeviceClass.Keyboard in this -> ExternalInputDeviceClass.Keyboard
        else -> ExternalInputDeviceClass.Unknown
    }

private fun externalInputSummary(devices: List<ExternalInputDeviceSummary>): String =
    if (devices.isEmpty()) {
        "No external input devices detected"
    } else {
        "External devices: " + devices.joinToString { "${it.name} (${it.displayClasses})" }
    }

private sealed interface RemoteInputCommand {
    val isRealtime: Boolean
        get() = this is RemoteInputCommand.PointerMove ||
            this is RemoteInputCommand.PointerMoveAbsolute ||
            this is RemoteInputCommand.Scroll ||
            this is RemoteInputCommand.Ping ||
            (this is RemoteInputCommand.External && this.event.highFrequency)

    val isTerminal: Boolean
        get() = !isRealtime

    data class PointerMove(val dx: Float, val dy: Float) : RemoteInputCommand
    data class PointerMoveAbsolute(val sourceId: String?, val x: Float, val y: Float) : RemoteInputCommand
    data class Scroll(val dx: Float, val dy: Float) : RemoteInputCommand
    data class PointerButton(val button: it.frumorn.gesto.core.model.PointerButton, val state: ButtonState) : RemoteInputCommand
    data class External(val event: ExternalInputEvent) : RemoteInputCommand
    data object ScrollFinish : RemoteInputCommand
    data object ReleasePointerButtons : RemoteInputCommand
    data object Ping : RemoteInputCommand
}

/**
 * True when the link itself looks broken (reset, closed, timed out) as opposed to the daemon
 * answering with an error. Any answer from the daemon proves the transport is alive.
 */
internal fun Throwable.mayHaveRun(): Boolean =
    generateSequence(this) { it.cause }.take(8).any { it is RequestMayHaveRunException }

internal fun Throwable.isTransportFailure(): Boolean {
    val chain = generateSequence(this) { it.cause }.take(8).toList()
    if (chain.any { it is GestoApiException }) return false
    if (chain.any { it is java.io.IOException }) return true
    val text = chain.mapNotNull { it.message }.joinToString(" ").lowercase()
    return "broken pipe" in text ||
        "connection reset" in text ||
        "connection closed" in text ||
        "socket closed" in text ||
        "not connected" in text
}

/** Scanning for hosts never changes the state of a session that is held: only an idle app moves. */
internal fun GestoUiState.stateDuringDiscovery(idle: ConnectionState): ConnectionState =
    if (connectedHost != null) connectionState else idle
