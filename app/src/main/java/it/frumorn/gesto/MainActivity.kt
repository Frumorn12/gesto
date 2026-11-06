package it.frumorn.gesto

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import it.frumorn.gesto.core.externalinput.AndroidExternalInputClassifier
import it.frumorn.gesto.core.externalinput.ExternalInputDeviceClass
import it.frumorn.gesto.core.externalinput.AndroidExternalInputMapper
import it.frumorn.gesto.core.externalinput.ExternalInputDeviceMonitor
import it.frumorn.gesto.core.externalinput.ExternalInputEvent
import it.frumorn.gesto.ui.GestoApp

class MainActivity : ComponentActivity() {
    private companion object {
        const val TAG = "GestoExternalInput"
    }

    private val viewModel: GestoViewModel by viewModels()
    private val externalInputMapper = AndroidExternalInputMapper()
    private lateinit var externalInputMonitor: ExternalInputDeviceMonitor
    private val pressedControllerKeys = mutableSetOf<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        externalInputMonitor = ExternalInputDeviceMonitor(
            context = this,
            mapper = externalInputMapper,
            onDevicesChanged = viewModel::updateExternalInputDevices,
        )
        window.decorView.setOnCapturedPointerListener { _, event ->
            routeExternalMotionEvent(event)
        }
        setContent {
            GestoApp(viewModel)
        }
        // After process death the system hands back the same intent: it was handled already.
        if (savedInstanceState == null) handleLinkIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleLinkIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        externalInputMonitor.start()
    }

    override fun onStop() {
        externalInputMonitor.stop()
        super.onStop()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (handleControllerMenuShortcut(event)) return true
        val external = externalInputMapper.keyEventToExternal(event)
        if (external != null && routeExternalInputEvent(external)) return true
        return super.dispatchKeyEvent(event)
    }

    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (routeExternalMotionEvent(event)) return true
        return super.dispatchGenericMotionEvent(event)
    }

    private fun routeExternalMotionEvent(event: MotionEvent): Boolean {
        val events = externalInputMapper.motionEventToExternal(event)
        if (events.isEmpty()) return false
        var handled = false
        for (external in events) {
            handled = viewModel.handleExternalInputEvent(external) || handled
        }
        if (handled) {
            Log.v(TAG, "external_motion_handled action=${event.actionMasked} count=${events.size}")
        }
        return handled
    }

    private fun routeExternalInputEvent(event: ExternalInputEvent): Boolean {
        val handled = viewModel.handleExternalInputEvent(event)
        if (handled) Log.v(TAG, "external_key_handled type=${event.deviceType.wireName}")
        return handled
    }

    /**
     * `gesto://invite` (and the old `waypad://invite`) opens the invite's confirmation in Add a PC;
     * `gesto://connect?host=` opens a PC.
     */
    private fun handleLinkIntent(intent: Intent?) {
        val data = intent?.dataString ?: return
        when {
            data.startsWith("gesto://invite") || data.startsWith("waypad://invite") -> {
                Log.i(TAG, "qr_invite_intent")
                viewModel.applyInvite(data)
            }
            data.startsWith("gesto://connect") -> {
                val hostId = intent.data?.getQueryParameter("host")
                Log.i(TAG, "connect_intent known=${hostId != null}")
                hostId?.let(viewModel::connectById)
            }
            else -> return
        }
        // Handled once: a rotation recreates the activity with the same intent.
        intent.data = null
        intent.removeExtra(EXTRA_CONNECT_HOST)
    }

    /** MODE, or SELECT and START together, on a controller open (or close) the quick menu. */
    private fun handleControllerMenuShortcut(event: KeyEvent): Boolean {
        val device = event.device ?: return false
        if (!device.isExternal) return false
        val classes = AndroidExternalInputClassifier.classify(device.sources, device.keyboardType)
        val isController = ExternalInputDeviceClass.Gamepad in classes ||
            ExternalInputDeviceClass.Joystick in classes
        if (!isController) return false

        when (event.action) {
            KeyEvent.ACTION_DOWN -> pressedControllerKeys += event.keyCode
            KeyEvent.ACTION_UP -> pressedControllerKeys -= event.keyCode
        }

        if (event.keyCode == KeyEvent.KEYCODE_BUTTON_MODE) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                viewModel.toggleQuickMenuFromController("controller_mode")
            }
            return true
        }

        val selectStartCombo =
            KeyEvent.KEYCODE_BUTTON_SELECT in pressedControllerKeys &&
                KeyEvent.KEYCODE_BUTTON_START in pressedControllerKeys
        if (selectStartCombo) {
            if (event.action == KeyEvent.ACTION_DOWN && event.repeatCount == 0) {
                viewModel.toggleQuickMenuFromController("controller_start_select")
            }
            return true
        }
        return false
    }
}
