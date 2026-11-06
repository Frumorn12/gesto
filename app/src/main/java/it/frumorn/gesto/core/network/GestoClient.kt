package it.frumorn.gesto.core.network

import it.frumorn.gesto.core.externalinput.ExternalInputDeviceClass
import it.frumorn.gesto.core.externalinput.ExternalInputEvent
import it.frumorn.gesto.core.model.ButtonState
import it.frumorn.gesto.core.model.CapabilitySummary
import it.frumorn.gesto.core.model.PointerButton
import it.frumorn.gesto.core.model.ScreenSource
import it.frumorn.gesto.core.model.ScreenStreamInfo
import it.frumorn.gesto.core.model.TrustedHost
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class GestoClient {
    private companion object {
        const val MAX_ONE_WAY_POINTER_RESPONSES = 32
        const val RESPONSE_TIMEOUT_MS = 10_000
        const val PING_TIMEOUT_MS = 3_000
        /** For a request the user answers on the PC (approval pairing). */
        const val APPROVAL_TIMEOUT_MS = 130_000
        const val APP_VERSION = "0.2.0"
    }

    private val transportMutex = Mutex()
    private val oneWayResponseIds = ArrayDeque<String>()
    private var channel: SecureChannel? = null
    /** Guards [channel] replacement against [shutdown] from another thread. */
    private val channelLock = Any()
    private var shutDown = false
    /** The pairing being waited for; guarded by [channelLock]. */
    private val pairingAttempts = PairingAttempts<SecureChannel>()

    /** Pairs with the 6-digit code `gesto pair` (or `waypad-daemon pair-code`) printed on the PC. */
    suspend fun pair(
        address: String,
        port: Int,
        deviceName: String,
        pairingCode: String,
        expectedFingerprint: String? = null,
    ): Pair<TrustedHost, CapabilitySummary> = pairOver(address, port, expectedFingerprint, approval = false) { id ->
        JSONObject()
            .put("type", "pair_request")
            .put("request_id", id)
            .put("device_name", deviceName)
            .put("pairing_code", pairingCode)
            .put("app_version", "0.1.0")
    }

    /**
     * Pairs by approval: the PC shows a notification with [pin] and the person there allows it. The
     * answer can take up to two minutes; [cancelPairing] stops waiting and makes the PC withdraw the
     * notification. A daemon that does not know this request fails with
     * [ApprovalNotSupportedException].
     */
    suspend fun pairWithApproval(
        address: String,
        port: Int,
        deviceName: String,
        pin: String,
        expectedFingerprint: String?,
    ): Pair<TrustedHost, CapabilitySummary> = pairOver(address, port, expectedFingerprint, approval = true) { id ->
        buildApprovalPairRequest(id, deviceName, pin, APP_VERSION)
    }

    /**
     * Stops the pairing that is running, if any: its connection is closed (for an approval the PC then
     * withdraws its notification) and it fails with [PairingCancelledException]. A pairing that has
     * already been answered is not undone, and its channel, now the session's, stays open. Returns
     * whether there was a pairing to stop.
     */
    fun cancelPairing(): Boolean = synchronized(channelLock) { pairingAttempts.cancel() }

    /**
     * One pairing exchange on its own connection. The wait happens outside [transportMutex], so a
     * session already open keeps working while the PC decides; the mutex is taken only to swap the
     * new channel in. The live channel stays open until then, so a failed attempt leaves it intact.
     */
    private suspend fun pairOver(
        address: String,
        port: Int,
        expectedFingerprint: String?,
        approval: Boolean,
        request: (requestId: String) -> JSONObject,
    ): Pair<TrustedHost, CapabilitySummary> {
        // Registered before the first suspension, so a cancel that follows the call always finds it.
        val attempt = beginPairing()
        return withContext(Dispatchers.IO) {
            try {
                val ch = SecureChannel.connect(address, port, expectedFingerprint)
                val registered = synchronized(channelLock) { !shutDown && pairingAttempts.connected(attempt, ch) }
                if (!registered) {
                    ch.close()
                    throw PairingCancelledException()
                }
                // Everything that can fail happens before the live channel is touched; the new one
                // is closed on any failure.
                val paired = try {
                    val id = requestId()
                    ch.send(request(id))
                    val sentAt = System.nanoTime()
                    try {
                        val timeout = if (approval) APPROVAL_TIMEOUT_MS else RESPONSE_TIMEOUT_MS
                        // A fresh channel has no one-way requests outstanding.
                        val data = matchResponse(id, mutableListOf()) { ch.receive(timeout) }.getJSONObject("data")
                        pairedHost(data, address, port)
                    } catch (e: Throwable) {
                        if (synchronized(channelLock) { attempt.cancelled }) throw PairingCancelledException()
                        throw if (approval) classifyApprovalFailure(e, (System.nanoTime() - sentAt) / 1_000_000) else e
                    }
                } catch (e: Throwable) {
                    ch.close()
                    throw e
                }
                transportMutex.withLock {
                    synchronized(channelLock) {
                        if (attempt.cancelled) {
                            ch.close()
                            throw PairingCancelledException()
                        }
                        adopt(ch)
                        // From here a late cancel must not close what is now the session's channel.
                        pairingAttempts.adopted(attempt)
                    }
                }
                paired
            } finally {
                synchronized(channelLock) { pairingAttempts.finished(attempt) }
            }
        }
    }

    /** Starts tracking a new pairing; one that was still running is cancelled. */
    private fun beginPairing(): PairingAttempts.Attempt<SecureChannel> = synchronized(channelLock) { pairingAttempts.begin() }

    private fun pairedHost(data: JSONObject, address: String, port: Int): Pair<TrustedHost, CapabilitySummary> =
        TrustedHost(
            id = "${address}:${port}",
            hostName = data.optString("host_name", address),
            address = address,
            port = port,
            fingerprint = data.getString("host_fingerprint"),
            deviceId = data.getString("device_id"),
            sessionToken = data.getString("session_token"),
            lastConnectedAt = System.currentTimeMillis(),
            macAddresses = data.macAddresses(),
        ) to data.optJSONObject("capabilities").toCapabilitySummary()

    /** Opens an authenticated session with a paired PC. */
    suspend fun connect(host: TrustedHost): ConnectResult = withContext(Dispatchers.IO) { transportMutex.withLock {
        val ch = SecureChannel.connect(host.address, host.port, host.fingerprint)
        val result = try {
            val id = requestId()
            ch.send(
                JSONObject()
                    .put("type", "auth_request")
                    .put("request_id", id)
                    .put("device_id", host.deviceId)
                    .put("session_token", host.sessionToken)
                    .put("app_version", "0.1.0")
            )
            val data = ch.receiveResponse(id).getJSONObject("data")
            ConnectResult(data.optJSONObject("capabilities").toCapabilitySummary(), data.macAddresses())
        } catch (e: Throwable) {
            ch.close()
            throw e
        }
        adopt(ch)
        result
    } }

    suspend fun prepareInput() {
        command("prepare_input", timeoutMs = APPROVAL_TIMEOUT_MS)
    }

    suspend fun ping() {
        withContext(Dispatchers.IO) { transportMutex.withLock {
            val ch = channel ?: error("Not connected")
            val id = requestId()
            ch.send(JSONObject().put("type", "ping").put("request_id", id))
            ch.receiveResponse(id, PING_TIMEOUT_MS)
        } }
    }

    suspend fun pointerMove(dx: Float, dy: Float) {
        commandOneWay("pointer_move", JSONObject().put("dx", dx.toDouble()).put("dy", dy.toDouble()))
    }

    suspend fun pointerMoveAbsolute(sourceId: String?, x: Float, y: Float) {
        val body = JSONObject()
            .put("x", x.toDouble())
            .put("y", y.toDouble())
        if (!sourceId.isNullOrBlank()) body.put("source_id", sourceId)
        commandOneWay("pointer_move_absolute", body)
    }

    suspend fun pointerButton(button: PointerButton, state: ButtonState) {
        commandOneWay(
            "pointer_button",
            JSONObject().put("button", button.wireName).put("state", state.wireName),
        )
    }

    suspend fun scroll(dx: Float, dy: Float, finish: Boolean = false) {
        val body = JSONObject().put("dx", dx.toDouble()).put("dy", dy.toDouble()).put("finish", finish)
        if (finish) {
            command("scroll", body)
        } else {
            commandOneWay("scroll", body)
        }
    }

    suspend fun text(text: String) {
        command("text", JSONObject().put("text", text))
    }

    suspend fun shortcut(vararg keys: String) {
        command("shortcut", JSONObject().put("keys", JSONArray(keys.toList())))
    }

    suspend fun media(action: String) {
        command("media", JSONObject().put("action", action))
    }

    suspend fun volume(action: String) {
        command("volume", JSONObject().put("action", action))
    }

    suspend fun brightness(action: String) {
        command("brightness", JSONObject().put("action", action))
    }

    suspend fun system(action: String) {
        command("system", JSONObject().put("action", action))
    }

    suspend fun clipboard(text: String) {
        command("clipboard_set", JSONObject().put("text", text))
    }

    suspend fun externalInput(event: ExternalInputEvent) {
        val body = event.toJson()
        if (event.highFrequency) {
            commandFireForget("external_input", body)
        } else {
            command("external_input", body)
        }
    }

    suspend fun capabilities(): CapabilitySummary = withContext(Dispatchers.IO) {
        command("get_capabilities")?.toCapabilitySummary() ?: CapabilitySummary()
    }

    suspend fun listScreenSources(): List<ScreenSource> = withContext(Dispatchers.IO) {
        val data = command("list_screen_sources") ?: return@withContext emptyList()
        val sources = data.optJSONArray("sources") ?: return@withContext emptyList()
        (0 until sources.length()).mapNotNull { index -> sources.optJSONObject(index)?.toScreenSource() }
    }

    suspend fun startScreenStream(
        sourceId: String?,
        maxFps: Int = 12,
        jpegQuality: Int = 70,
        maxWidth: Int? = null,
        maxHeight: Int? = null,
    ): ScreenStreamInfo = withContext(Dispatchers.IO) {
        val body = JSONObject()
            .put("max_fps", maxFps)
            .put("jpeg_quality", jpegQuality)
        maxWidth?.let { body.put("max_width", it) }
        maxHeight?.let { body.put("max_height", it) }
        if (!sourceId.isNullOrBlank()) body.put("source_id", sourceId)
        command("start_screen_stream", body)?.toScreenStreamInfo()
            ?: error("Daemon returned no screen stream information")
    }

    suspend fun stopScreenStream(sessionId: String) {
        command("stop_screen_stream", JSONObject().put("session_id", sessionId))
    }

    /**
     * Mutes desktop audio at the source.
     *
     * The phone silences playback on its own the moment the user asks, so this is not what makes
     * it quiet — it is what stops the host burning bandwidth on audio nobody is listening to.
     */
    suspend fun setDesktopAudioMute(sessionId: String, muted: Boolean) {
        command(
            "set_desktop_audio_mute",
            JSONObject().put("session_id", sessionId).put("muted", muted),
        )
    }

    /**
     * Makes [ch] the live channel, closing the previous one. After [shutdown] a connect or pair
     * that was still running on its socket closes its channel instead of keeping it open.
     */
    private fun adopt(ch: SecureChannel) = synchronized(channelLock) {
        if (shutDown) {
            ch.close()
            throw IllegalStateException("Gesto client shut down")
        }
        channel?.close()
        oneWayResponseIds.clear()
        channel = ch
    }

    /** Closes the client for good (the ViewModel is going away). */
    fun shutdown() = synchronized(channelLock) {
        shutDown = true
        pairingAttempts.cancel()
        close()
    }

    fun close() {
        channel?.close()
        channel = null
        oneWayResponseIds.clear()
    }

    private suspend fun command(
        name: String,
        body: JSONObject = JSONObject(),
        timeoutMs: Int = RESPONSE_TIMEOUT_MS,
    ): JSONObject? = withContext(Dispatchers.IO) { transportMutex.withLock {
        val ch = channel ?: error("Not connected")
        val id = requestId()
        val command = JSONObject(body.toString()).put("name", name)
        ch.send(JSONObject().put("type", "command").put("request_id", id).put("command", command))
        try {
            ch.receiveResponse(id, timeoutMs).optJSONObject("data")
        } catch (e: GestoApiException) {
            throw e
        } catch (e: java.io.IOException) {
            // The request was already written: it may have run on the PC.
            throw RequestMayHaveRunException(e)
        }
    } }

    private suspend fun commandOneWay(name: String, body: JSONObject = JSONObject()) = withContext(Dispatchers.IO) { transportMutex.withLock {
        val ch = channel ?: error("Not connected")
        val id = requestId()
        val command = JSONObject(body.toString()).put("name", name)
        ch.send(JSONObject().put("type", "command").put("request_id", id).put("command", command))
        oneWayResponseIds.addLast(id)
        while (oneWayResponseIds.size > MAX_ONE_WAY_POINTER_RESPONSES) {
            ch.receiveResponse(oneWayResponseIds.removeFirst())
        }
    } }

    private suspend fun commandFireForget(name: String, body: JSONObject = JSONObject()) = withContext(Dispatchers.IO) {
        runCatching { transportMutex.withLock {
            val ch = channel ?: error("Not connected")
            val id = requestId()
            val command = JSONObject(body.toString()).put("name", name)
            ch.send(JSONObject().put("type", "command").put("request_id", id).put("command", command))
        } }
    }

    private fun SecureChannel.receiveResponse(id: String, timeoutMs: Int = RESPONSE_TIMEOUT_MS): JSONObject =
        try {
            matchResponse(id, oneWayResponseIds) { receive(timeoutMs) }
        } catch (e: java.net.SocketTimeoutException) {
            // A reply that arrives late would poison every later read: drop the whole channel so
            // the caller's reconnect starts from a clean stream.
            close()
            if (channel === this) channel = null
            throw e
        }
}

/**
 * What a PC says about itself when the app connects: what it can do, and the MAC addresses of its
 * network cards (empty from a daemon that does not report them).
 */
data class ConnectResult(
    val capabilities: CapabilitySummary,
    val macAddresses: List<String> = emptyList(),
)

/** The `approval_pair_request` message: the PC shows [pin] in a notification and asks whether to let [deviceName] in. */
fun buildApprovalPairRequest(requestId: String, deviceName: String, pin: String, appVersion: String): JSONObject =
    JSONObject()
        .put("type", "approval_pair_request")
        .put("request_id", requestId)
        .put("device_name", deviceName)
        .put("pin", pin)
        .put("app_version", appVersion)

/** How soon after the request an old daemon gives itself away. */
internal const val OLD_DAEMON_WINDOW_MS = 3_000L

/** Messages of a connection that ended, or of an answer the app cannot read. */
private val UNREADABLE_ANSWERS = setOf("Connection closed", "Unexpected server message", "Response ID mismatch")

/**
 * A daemon from before approval pairing does not know the request and drops the connection at once
 * (or answers something the app cannot read). Such a failure within [OLD_DAEMON_WINDOW_MS] of
 * sending becomes [ApprovalNotSupportedException]; anything else, or later, is returned as it is.
 */
internal fun classifyApprovalFailure(error: Throwable, elapsedMs: Long): Throwable {
    if (elapsedMs > OLD_DAEMON_WINDOW_MS) return error
    val chain = generateSequence(error) { it.cause }.take(8).toList()
    if (chain.any { it is GestoApiException || it is java.net.SocketTimeoutException }) return error
    val oldDaemon = chain.any {
        it is java.io.EOFException ||
            it is java.net.SocketException ||
            it is org.json.JSONException ||
            (it is IllegalStateException && it.message in UNREADABLE_ANSWERS)
    }
    return if (oldDaemon) ApprovalNotSupportedException(error) else error
}

/** The PC runs a daemon that cannot ask for approval: pair it with a code instead. */
class ApprovalNotSupportedException(cause: Throwable) :
    java.io.EOFException("The PC cannot approve pairing requests") {
    init {
        initCause(cause)
    }
}

/** The pairing was stopped by [GestoClient.cancelPairing] (or a newer pairing). */
class PairingCancelledException : java.io.IOException("Pairing cancelled")

/** `data.mac_addresses` of a pair or auth response, keeping only well-formed addresses. */
internal fun JSONObject.macAddresses(): List<String> {
    val array = optJSONArray("mac_addresses") ?: return emptyList()
    return (0 until array.length())
        .mapNotNull { index -> array.opt(index) as? String }
        .map { it.trim().lowercase() }
        .filter { mac -> runCatching { WakeOnLan.magicPacket(mac) }.isSuccess }
        .distinct()
}

/** The daemon answered, so the link is fine, but the command may or may not have run. */
class RequestMayHaveRunException(cause: Throwable) : java.io.IOException(cause.message, cause)

/**
 * Reads responses from [next] until the one for [id]. Responses for ids in [pendingIds] (earlier
 * requests whose answer was never consumed) are skipped, though an error among them is thrown.
 * Any other id is a protocol error.
 */
internal fun matchResponse(id: String, pendingIds: MutableCollection<String>, next: () -> JSONObject): JSONObject {
    while (true) {
        val response = next()
        check(response.getString("type") == "response") { "Unexpected server message" }
        val responseId = response.getString("request_id")
        if (responseId != id) {
            if (pendingIds.remove(responseId)) {
                if (!response.getBoolean("ok")) throw response.getJSONObject("error").toApiException()
                continue
            }
            error("Response ID mismatch")
        }
        if (!response.getBoolean("ok")) throw response.getJSONObject("error").toApiException()
        pendingIds.remove(responseId)
        return response
    }
}

private fun JSONObject.toApiException() = GestoApiException(
    code = optString("code").ifBlank { "unknown" },
    message = optString("message"),
    retryable = optBoolean("retryable", false),
)

private fun JSONObject?.toCapabilitySummary(): CapabilitySummary {
    if (this == null) return CapabilitySummary()
    val input = optJSONObject("input")
    val external = optJSONObject("external_input")
    val connectivity = optJSONObject("connectivity")
    val capture = optJSONObject("capture")
    val system = optJSONObject("system")
    val audio = optJSONObject("audio_capture")
    return CapabilitySummary(
        inputSupported = input?.optBoolean("supported") ?: false,
        inputReason = input?.optString("reason", "No input capability data") ?: "No input capability data",
        inputBackend = input?.optString("backend", "unknown") ?: "unknown",
        externalPointerSupported = external?.optBoolean("pointer") ?: (input?.optBoolean("supported") ?: false),
        externalKeyboardSupported = external?.optBoolean("keyboard") ?: (input?.optBoolean("supported") ?: false),
        externalControllerSupported = external?.optBoolean("controller") ?: false,
        externalInputReason = external?.optString("reason", input?.optString("reason", "No external input data") ?: "No external input data")
            ?: "No external input data",
        routeBackend = connectivity?.optString("backend", "unknown") ?: "unknown",
        lanDirectSupported = connectivity?.optBoolean("lan_direct") ?: false,
        publicDirectSupported = connectivity?.optBoolean("public_direct") ?: false,
        publicPairingAllowed = connectivity?.optBoolean("public_pairing_allowed") ?: false,
        relaySupported = connectivity?.optBoolean("relay") ?: false,
        connectivityReason = connectivity?.optString("reason", "No connectivity data") ?: "No connectivity data",
        captureSupported = capture?.optBoolean("supported") ?: false,
        captureReason = capture?.optString("reason", "No capture capability data") ?: "No capture capability data",
        captureBackend = capture?.optString("backend", "unknown") ?: "unknown",
        captureRequiresApproval = capture?.optBoolean("requires_user_approval") ?: false,
        audioSupported = audio?.optBoolean("supported") ?: false,
        volume = system?.optBoolean("volume") ?: false,
        media = system?.optBoolean("media") ?: false,
        brightness = system?.optBoolean("brightness") ?: false,
        clipboard = system?.optBoolean("clipboard") ?: false,
        lock = system?.optBoolean("lock") ?: false,
        suspend = system?.optBoolean("suspend") ?: false,
    )
}

private fun JSONObject.toScreenSource(): ScreenSource = ScreenSource(
    id = getString("id"),
    label = optString("label", getString("id")),
    kind = optString("kind", "monitor"),
    backend = optString("backend", "unknown"),
    width = optInt("width"),
    height = optInt("height"),
    x = optInt("x"),
    y = optInt("y"),
    scale = optDouble("scale", 1.0),
    focused = optBoolean("focused"),
)

private fun ExternalInputEvent.toJson(): JSONObject {
    val body = JSONObject()
        .put("device_id", deviceId)
        .put("device_type", deviceType.wireName)
    val event = when (this) {
        is ExternalInputEvent.DeviceConnected -> JSONObject()
            .put("type", "device_connected")
            .put("name", name)
            .put("classes", JSONArray(classes.map(ExternalInputDeviceClass::wireName)))
        is ExternalInputEvent.DeviceDisconnected -> JSONObject()
            .put("type", "device_disconnected")
        is ExternalInputEvent.PointerMove -> JSONObject()
            .put("type", "pointer_move")
            .put("dx", dx.toDouble())
            .put("dy", dy.toDouble())
        is ExternalInputEvent.PointerButton -> JSONObject()
            .put("type", "pointer_button")
            .put("button", button.wireName)
            .put("state", state.wireName)
        is ExternalInputEvent.PointerScroll -> JSONObject()
            .put("type", "pointer_scroll")
            .put("dx", dx.toDouble())
            .put("dy", dy.toDouble())
            .put("finish", finish)
        is ExternalInputEvent.KeyboardKey -> JSONObject()
            .put("type", "keyboard_key")
            .put("keysym", keysym)
            .put("state", state.wireName)
            .put("repeat", repeat)
        is ExternalInputEvent.ControllerButton -> JSONObject()
            .put("type", "controller_button")
            .put("button", button)
            .put("state", state.wireName)
        is ExternalInputEvent.ControllerAxis -> JSONObject()
            .put("type", "controller_axis")
            .put("axis", axis)
            .put("value", value.toDouble())
    }
    return body.put("event", event)
}

private fun JSONObject.toScreenStreamInfo(): ScreenStreamInfo = ScreenStreamInfo(
    sessionId = getString("session_id"),
    streamPort = getInt("stream_port"),
    token = getString("token"),
    codec = optString("codec", "jpeg"),
    transport = optString("transport", "waypad-frame-stream-v1"),
    source = getJSONObject("source").toScreenSource(),
    actualFps = optInt("actual_fps", 0),
    actualQuality = optInt("actual_quality", 0),
)
