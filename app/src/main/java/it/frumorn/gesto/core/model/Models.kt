package it.frumorn.gesto.core.model

data class DiscoveredHost(
    val hostName: String,
    val address: String,
    val port: Int,
    val fingerprint: String,
    val inputSupported: Boolean,
    val inputBackend: String,
    val captureSupported: Boolean = false,
    val captureBackend: String = "unknown",
    val approvalPairing: Boolean = false,
)

data class TrustedHost(
    val id: String,
    val hostName: String,
    val address: String,
    val port: Int,
    val fingerprint: String,
    val deviceId: String,
    val sessionToken: String,
    val lastConnectedAt: Long,
    /** The name the user gave this PC; null shows the PC's own [hostName]. */
    val displayName: String? = null,
    /** MAC addresses of the PC's network cards, for Wake-on-LAN; reported by the daemon. */
    val macAddresses: List<String> = emptyList(),
)

/** Longest name the user can give a PC. */
const val MAX_PC_NAME_LENGTH = 40

/** What the app calls this PC. */
val TrustedHost.label: String
    get() = displayName ?: hostName

/** An app or script the PC offers to start, by the id set in Gesto's configuration there. */
data class RemoteApp(val id: String, val name: String)

data class CapabilitySummary(
    val inputSupported: Boolean = false,
    val inputReason: String = "Not connected",
    val inputBackend: String = "unknown",
    val externalPointerSupported: Boolean = false,
    val externalKeyboardSupported: Boolean = false,
    val externalControllerSupported: Boolean = false,
    val externalInputReason: String = "Not connected",
    val routeBackend: String = "unknown",
    val lanDirectSupported: Boolean = false,
    val publicDirectSupported: Boolean = false,
    val publicPairingAllowed: Boolean = false,
    val relaySupported: Boolean = false,
    val connectivityReason: String = "Not connected",
    val captureSupported: Boolean = false,
    val captureReason: String = "Not connected",
    val captureBackend: String = "unknown",
    val captureRequiresApproval: Boolean = false,
    /** The PC can send its sound along with the screen. */
    val audioSupported: Boolean = false,
    val volume: Boolean = false,
    val media: Boolean = false,
    val brightness: Boolean = false,
    val clipboard: Boolean = false,
    val lock: Boolean = false,
    val suspend: Boolean = false,
    /** What the PC lets the phone launch; empty when nothing is configured. */
    val apps: List<RemoteApp> = emptyList(),
)

data class ScreenSource(
    val id: String,
    val label: String,
    val kind: String,
    val backend: String,
    val width: Int,
    val height: Int,
    val x: Int,
    val y: Int,
    val scale: Double,
    val focused: Boolean,
)

data class ScreenStreamInfo(
    val sessionId: String,
    val streamPort: Int,
    val token: String,
    val codec: String,
    val transport: String,
    val source: ScreenSource,
    val actualFps: Int = 0,
    val actualQuality: Int = 0,
)

enum class StreamProfile(
    val label: String,
    val defaultFps: Int,
    val defaultQuality: Int,
    val defaultMaxDimension: Int,
) {
    Balanced("Balanced", 30, 70, 1600),
    Quality("Quality", 30, 86, 2400),
    LowLatency("Ultra low latency", 60, 58, 1280),
    Game("Game Mode", 60, 52, 1280);

    fun toStreamSettings(showStats: Boolean): StreamSettings =
        StreamSettings(
            profile = this,
            maxFps = defaultFps,
            jpegQuality = defaultQuality,
            maxDimension = defaultMaxDimension,
            showStats = showStats,
        )
}

/**
 * How a finger on the remote screen drives the PC: like the touchpad (it moves the pointer from
 * where it is) or directly (a tap clicks where it touches).
 */
enum class TouchMode { Touchpad, Direct }

data class StreamSettings(
    val profile: StreamProfile = StreamProfile.Balanced,
    val maxFps: Int = StreamProfile.Balanced.defaultFps,
    val jpegQuality: Int = StreamProfile.Balanced.defaultQuality,
    val maxDimension: Int = StreamProfile.Balanced.defaultMaxDimension,
    val showStats: Boolean = true,
) {
    val maxWidth: Int
        get() = maxDimension
    val maxHeight: Int
        get() = maxDimension
}

data class ScreenStreamStats(
    val estimatedFps: Double = 0.0,
    val averageKib: Int = 0,
    val lastFrameAgeMs: Long = 0,
    val droppedStaleFrames: Long = 0,
    val receivedFrames: Long = 0,
    val deliveredFps: Double = 0.0,
    val targetFps: Int = 0,
    val actualFps: Int = 0,
    val backend: String = "unknown",
    val bytesPerSecond: Long = 0,
    val codec: String = "",
) {
    val kilobitsPerSecond: Int
        get() = ((bytesPerSecond * 8L) / 1000L).toInt()
}

enum class RemoteScreenConnectionState {
    Idle,
    Connecting,
    Negotiating,
    Streaming,
    Reconnecting,
    Failed,
    Closed,
}

enum class ConnectionState {
    Disconnected,
    Discovering,
    Pairing,
    Connecting,
    Connected,
    Error,
}

enum class PointerButton(val wireName: String) {
    Left("left"),
    Middle("middle"),
    Right("right"),
}

fun Double.formatFps(): String = if (this >= 10.0) {
    "${this.toInt()}"
} else {
    "%.1f".format(this)
}

enum class ButtonState(val wireName: String) {
    Pressed("pressed"),
    Released("released"),
}
