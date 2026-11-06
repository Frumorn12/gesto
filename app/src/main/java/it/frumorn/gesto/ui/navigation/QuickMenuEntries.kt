package it.frumorn.gesto.ui.navigation

import it.frumorn.gesto.SessionMode

/** One line of the quick menu. */
enum class QuickMenuEntry {
    Keyboard,
    RightClick,
    /** PC audio on or off; it comes with the screen, so only the Screen has it. */
    Audio,
    /** Touchpad or Direct, for the remote screen. */
    TouchMode,
    /** The stream quality, one step at a time. */
    Quality,
    GameMode,
    BackToTouchpad,
    BackToPcs,
}

/**
 * The quick menu of a session mode. It is the same menu everywhere, without what makes no sense
 * there: the touch mode, the quality and the PC audio belong to the Screen, and Game mode needs a
 * PC that shares its screen ([captureSupported]). The last line is the way out: back to the
 * touchpad, or to the PCs from the touchpad itself.
 */
fun quickMenuEntries(mode: SessionMode, captureSupported: Boolean): List<QuickMenuEntry> = buildList {
    add(QuickMenuEntry.Keyboard)
    add(QuickMenuEntry.RightClick)
    if (mode == SessionMode.Display) {
        add(QuickMenuEntry.Audio)
        add(QuickMenuEntry.TouchMode)
        add(QuickMenuEntry.Quality)
    }
    if (captureSupported) add(QuickMenuEntry.GameMode)
    add(if (mode == SessionMode.Touchpad) QuickMenuEntry.BackToPcs else QuickMenuEntry.BackToTouchpad)
}
