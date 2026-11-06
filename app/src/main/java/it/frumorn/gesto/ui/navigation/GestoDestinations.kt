package it.frumorn.gesto.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import it.frumorn.gesto.R
import it.frumorn.gesto.Screen
import it.frumorn.gesto.SessionMode
import it.frumorn.gesto.ui.components.GestoIcons

/**
 * Declarative description of Gesto's navigation.
 *
 * Gesto deliberately does **not** use `androidx.navigation:navigation-compose`: `GestoViewModel`
 * already owns the current [Screen] inside its UI state, persists it through `SavedStateHandle`, and
 * attaches side effects to every transition (`go()` stops the screen stream, resets fullscreen,
 * remembers the last session mode and lazily loads capture sources). A `NavHost` would introduce a
 * second, competing source of truth for the same value. The back button is a pure function of the
 * same state, see [backAction].
 */

/** One entry of the session's bottom bar: a session mode, or (with no mode) the keyboard sheet. */
@Immutable
data class DockItem(@StringRes val label: Int, val icon: ImageVector, val mode: SessionMode?)

val SessionDockItems: List<DockItem> = listOf(
    DockItem(R.string.dock_touchpad, GestoIcons.Touchpad, SessionMode.Touchpad),
    DockItem(R.string.dock_screen, GestoIcons.Screen, SessionMode.Display),
    DockItem(R.string.dock_keyboard, GestoIcons.Keyboard, mode = null),
    DockItem(R.string.dock_commands, GestoIcons.Commands, SessionMode.Commands),
)

/** Screens that forward external pointer devices to the host while they are on top. */
val ScreensCapturingExternalPointer: Set<Screen> = setOf(Screen.Touchpad, Screen.Display)
