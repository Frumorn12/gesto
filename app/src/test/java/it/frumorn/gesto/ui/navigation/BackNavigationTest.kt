package it.frumorn.gesto.ui.navigation

import it.frumorn.gesto.AddPcStep
import it.frumorn.gesto.Screen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class BackNavigationTest {
    private fun back(
        screen: Screen,
        keyboardOpen: Boolean = false,
        quickMenuOpen: Boolean = false,
        addPcStep: AddPcStep = AddPcStep.Search,
        previousScreen: Screen? = null,
    ) = backAction(screen, keyboardOpen, quickMenuOpen, addPcStep, previousScreen)

    @Test fun open_keyboard_closes_first() {
        assertEquals(BackAction.CloseKeyboard, back(Screen.Touchpad, keyboardOpen = true))
    }

    @Test fun open_quick_menu_closes() {
        assertEquals(BackAction.CloseQuickMenu, back(Screen.Touchpad, quickMenuOpen = true))
    }

    @Test fun keyboard_wins_over_quick_menu() {
        assertEquals(
            BackAction.CloseKeyboard,
            back(Screen.Display, keyboardOpen = true, quickMenuOpen = true),
        )
    }

    @Test fun session_screens_open_the_quick_menu() {
        assertEquals(BackAction.OpenQuickMenu, back(Screen.Touchpad))
        assertEquals(BackAction.OpenQuickMenu, back(Screen.Display))
        assertEquals(BackAction.OpenQuickMenu, back(Screen.Commands))
    }

    @Test fun add_pc_inner_steps_go_back_to_search() {
        for (step in listOf(AddPcStep.Approval, AddPcStep.Code, AddPcStep.Address, AddPcStep.Qr)) {
            assertEquals(
                "step $step",
                BackAction.AddPcStepTo(AddPcStep.Search),
                back(Screen.AddPc, addPcStep = step),
            )
        }
    }

    @Test fun add_pc_search_goes_home() {
        assertEquals(BackAction.GoTo(Screen.Home), back(Screen.AddPc, addPcStep = AddPcStep.Search))
    }

    @Test fun settings_returns_where_it_was_opened() {
        assertEquals(BackAction.GoTo(Screen.Display), back(Screen.Settings, previousScreen = Screen.Display))
    }

    @Test fun settings_without_previous_goes_home() {
        assertEquals(BackAction.GoTo(Screen.Home), back(Screen.Settings, previousScreen = null))
    }

    @Test fun advanced_and_diagnostics_return_to_settings() {
        assertEquals(BackAction.GoTo(Screen.Settings), back(Screen.Advanced))
        assertEquals(BackAction.GoTo(Screen.Settings), back(Screen.Diagnostics))
    }

    @Test fun home_and_launch_exit_the_app() {
        assertEquals(BackAction.ExitApp, back(Screen.Home))
        assertEquals(BackAction.ExitApp, back(Screen.Launch))
    }

    @Test fun no_intermediate_state_ever_exits() {
        for (screen in Screen.entries) {
            for (step in AddPcStep.entries) {
                for (keyboard in listOf(false, true)) {
                    for (menu in listOf(false, true)) {
                        val action = back(screen, keyboard, menu, step, previousScreen = null)
                        val root = (screen == Screen.Home || screen == Screen.Launch) && !keyboard && !menu
                        if (!root) {
                            assertNotEquals("$screen $step keyboard=$keyboard menu=$menu", BackAction.ExitApp, action)
                        }
                    }
                }
            }
        }
    }

    @Test fun session_screens_need_a_connected_pc() {
        assertEquals(Screen.Home, reachableScreen(Screen.Display, connected = false))
        assertEquals(Screen.Home, reachableScreen(Screen.Touchpad, connected = false))
        assertEquals(Screen.Commands, reachableScreen(Screen.Commands, connected = true))
    }

    @Test fun other_screens_are_always_reachable() {
        for (screen in listOf(Screen.Launch, Screen.Home, Screen.AddPc, Screen.Settings, Screen.Advanced, Screen.Diagnostics)) {
            assertEquals(screen, reachableScreen(screen, connected = false))
        }
    }
}
