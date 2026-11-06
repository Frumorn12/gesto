package it.frumorn.gesto.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Single source of truth for every gap, inset and touch-target size in the Gesto UI.
 *
 * No composable is allowed to hardcode a `.dp` spacing value: it must resolve one of these tokens
 * through [GestoTheme.spacing].
 */
@Immutable
data class GestoSpacing(
    /** Hairline gap, used between tightly coupled labels. */
    val hairline: Dp = 2.dp,
    /** 4dp - caption to value. */
    val xs: Dp = 4.dp,
    /** 6dp - icon to label inside a button. */
    val sm: Dp = 6.dp,
    /** 8dp - default gap inside a row of controls. */
    val md: Dp = 8.dp,
    /** 10dp - overlay padding. */
    val lg: Dp = 10.dp,
    /** 12dp - list gap. */
    val xl: Dp = 12.dp,
    /** 14dp - generous list gap. */
    val xxl: Dp = 14.dp,
    /** 18dp - card inner padding and screen gutter. */
    val gutter: Dp = 18.dp,
    /** 22dp - hero block padding. */
    val hero: Dp = 22.dp,
    /** 28dp - separation between hero sections. */
    val section: Dp = 28.dp,
) {
    /** No spacing at all - use instead of a literal `0.dp`. */
    val none: Dp = 0.dp

    /** Hairline outline used on cards and the touchpad slab. */
    val borderWidth: Dp = 1.dp

    /** Movement below which a pointer gesture still counts as a tap. */
    val tapSlop: Dp = 8.dp

    /** Height of a primary "click" target on the touchpad screen. */
    val clickButtonHeight: Dp = 56.dp

    /** Size of the brand mark shown in the top app bar. */
    val brandMarkSize: Dp = 38.dp

    /** Size of the large decorative icon at the centre of empty states. */
    val emptyStateIconSize: Dp = 42.dp

    /** Width of the drag handle revealed in game-mode fullscreen. */
    val handleWidth: Dp = 44.dp

    /** Height of the drag handle revealed in game-mode fullscreen. */
    val handleHeight: Dp = 3.dp

    /** Height of the invisible strip that reveals the fullscreen controls. */
    val revealStripHeight: Dp = 34.dp

    /** Width reserved for the compact "Full" action in the stream setup card. */
    val compactActionWidth: Dp = 120.dp

    /** Line icons in the session bars. */
    val barIconSize: Dp = 22.dp

    /** The g written on the launch screen. */
    val launchMarkSize: Dp = 104.dp

    /** Indeterminate loading bar. */
    val loadingBarWidth: Dp = 180.dp
    val loadingBarHeight: Dp = 3.dp

    /** Floating quick menu panel and its icons. */
    val quickMenuWidth: Dp = 240.dp
    val menuIconSize: Dp = 20.dp

    /** Tallest the keyboard sheet grows before its content scrolls. */
    val keyboardSheetMaxHeight: Dp = 420.dp

    /** The square dot before a PC's status line. */
    val statusDotSize: Dp = 8.dp

    /** The Gesto mark on the empty PCs list. */
    val emptyMarkSize: Dp = 72.dp
}

val LocalGestoSpacing = staticCompositionLocalOf { GestoSpacing() }
