package it.frumorn.gesto.ui.navigation

import it.frumorn.gesto.AddPcStep
import it.frumorn.gesto.Screen

/** What the system back button does in a given state. */
sealed interface BackAction {
    data object CloseKeyboard : BackAction
    data object CloseQuickMenu : BackAction
    data object OpenQuickMenu : BackAction
    data class GoTo(val screen: Screen) : BackAction
    data class AddPcStepTo(val step: AddPcStep) : BackAction
    data object ExitApp : BackAction
}

/**
 * The back button never drops the user out of the app from a state in between: it closes what is
 * open first, opens the quick menu inside a session, steps back through Add a PC and Settings, and
 * only leaves from the PCs list or the launch screen. The rules apply in this order.
 */
fun backAction(
    screen: Screen,
    keyboardOpen: Boolean,
    quickMenuOpen: Boolean,
    addPcStep: AddPcStep,
    previousScreen: Screen?,
): BackAction = when {
    keyboardOpen -> BackAction.CloseKeyboard
    quickMenuOpen -> BackAction.CloseQuickMenu
    else -> when (screen) {
        Screen.Touchpad, Screen.Display, Screen.Commands -> BackAction.OpenQuickMenu
        Screen.AddPc -> when (addPcStep) {
            AddPcStep.Search -> BackAction.GoTo(Screen.Home)
            AddPcStep.Approval, AddPcStep.Code, AddPcStep.Address, AddPcStep.Qr ->
                BackAction.AddPcStepTo(AddPcStep.Search)
        }
        Screen.Settings -> BackAction.GoTo(previousScreen ?: Screen.Home)
        Screen.Advanced, Screen.Diagnostics -> BackAction.GoTo(Screen.Settings)
        Screen.Home, Screen.Launch -> BackAction.ExitApp
    }
}
