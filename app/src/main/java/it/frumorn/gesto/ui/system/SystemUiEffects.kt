package it.frumorn.gesto.ui.system

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.os.Build
import android.util.Log
import android.view.WindowInsets
import android.view.WindowInsetsController
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView

/**
 * Window-level side effects owned by the app shell.
 *
 * Moved out of `GestoApp.kt` unchanged: the logging tags and the exact ordering of the window flag
 * writes are relied upon by the field diagnostics.
 */

// Same scrims enableEdgeToEdge() uses by default, so three-button navigation stays readable.
private val LightNavigationScrim = android.graphics.Color.argb(0xE6, 0xFF, 0xFF, 0xFF)
private val DarkNavigationScrim = android.graphics.Color.argb(0x80, 0x1B, 0x1B, 0x1B)

/**
 * Status and navigation bar icons follow the app theme (Light/Dark/System), not the system night
 * mode: a light app on a dark phone would otherwise get white icons on a white bar.
 */
@Composable
fun SystemBarsThemeEffect(dark: Boolean) {
    val view = LocalView.current
    DisposableEffect(dark, view) {
        val activity = view.context.findActivity() as? ComponentActivity
        activity?.enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(
                android.graphics.Color.TRANSPARENT,
                android.graphics.Color.TRANSPARENT,
            ) { dark },
            navigationBarStyle = SystemBarStyle.auto(LightNavigationScrim, DarkNavigationScrim) { dark },
        )
        onDispose {}
    }
}

/** Grabs the physical mouse/touchpad so its deltas can be forwarded to the host. */
@Composable
fun ExternalPointerCaptureEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled, view) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (enabled) {
                Log.i("GestoExternalInput", "pointer_capture_request")
                view.requestPointerCapture()
            } else if (view.hasPointerCapture()) {
                Log.i("GestoExternalInput", "pointer_capture_release")
                view.releasePointerCapture()
            }
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && view.hasPointerCapture()) {
                view.releasePointerCapture()
            }
        }
    }
}

/**
 * Lets the remote screen turn with the phone (on its side it goes full screen), keeps the rest of
 * the app upright, and restores the previous policy on exit.
 */
@Composable
fun RemoteScreenOrientationEffect(onRemoteDisplay: Boolean, gameMode: Boolean) {
    val view = LocalView.current
    DisposableEffect(onRemoteDisplay, gameMode, view) {
        val activity = view.context.findActivity()
        val previous = activity?.requestedOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        val policy = when {
            // Game mode is the full screen on purpose: a desktop is wide, so landscape is asked for
            // outright - either way up, so the phone can still be turned around.
            onRemoteDisplay && gameMode -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            // Upright shows the screen with its actions under it; on its side, only the screen.
            onRemoteDisplay -> ActivityInfo.SCREEN_ORIENTATION_FULL_SENSOR
            else -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        Log.i("GestoRemoteScreen", "orientation_policy=$policy game_mode=$gameMode")
        activity?.requestedOrientation = policy
        onDispose {
            activity?.requestedOrientation = previous
        }
    }
}

/** Hides the system bars while the remote screen is full screen (on its side, or in Game mode). */
@Composable
fun FullscreenSystemUiEffect(enabled: Boolean) {
    val view = LocalView.current
    DisposableEffect(enabled, view) {
        val window = view.context.findActivity()?.window
        if (enabled) {
            Log.i("GestoRemoteScreen", "fullscreen_system_ui_hide")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window?.insetsController?.systemBarsBehavior =
                    WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                window?.insetsController?.hide(WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                window?.decorView?.systemUiVisibility =
                    android.view.View.SYSTEM_UI_FLAG_FULLSCREEN or
                        android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                        android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        } else {
            Log.i("GestoRemoteScreen", "fullscreen_system_ui_show")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window?.insetsController?.show(WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                window?.decorView?.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }
        onDispose {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                window?.insetsController?.show(WindowInsets.Type.systemBars())
            } else {
                @Suppress("DEPRECATION")
                window?.decorView?.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            }
        }
    }
}

internal tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
