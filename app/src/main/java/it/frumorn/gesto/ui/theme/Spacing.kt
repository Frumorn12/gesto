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

    /** Height of the Left and Right buttons (and the scroll strip) under the touchpad. */
    val clickButtonHeight: Dp = 58.dp

    /** Height of a key and of the text field on the keyboard sheet. */
    val keyHeight: Dp = 48.dp

    /** The narrow scroll strip between the touchpad's Left and Right buttons. */
    val scrollStripWidth: Dp = 54.dp

    /** One line of the scroll strip every this many dp. */
    val scrollStripLineGap: Dp = 8.dp

    /** The touchpad's dot grid: one dot every [padDotGap], [padDotRadius] across. */
    val padDotGap: Dp = 22.dp
    val padDotRadius: Dp = 1.dp

    /** The square drawn under each finger on the touchpad. */
    val fingerMarkSize: Dp = 34.dp

    /** The mini PC screen in the touchpad's corner, and the cursor square on it. */
    val miniScreenWidth: Dp = 76.dp
    val miniScreenHeight: Dp = 48.dp
    val miniCursorSize: Dp = 6.dp

    /** Size of the brand mark shown in the top app bar. */
    val brandMarkSize: Dp = 38.dp

    /** Size of the large decorative icon at the centre of empty states. */
    val emptyStateIconSize: Dp = 42.dp

    /** The purple quick-menu button, and how far its call-to-action pulse spreads. */
    val quickButtonSize: Dp = 46.dp
    val quickPulseSpread: Dp = 12.dp

    /** Shadows of the floating panels: the button (0 2 10) and the quick menu (0 10 30). */
    val quickButtonShadowBlur: Dp = 10.dp
    val quickButtonShadowY: Dp = 2.dp
    val menuShadowBlur: Dp = 30.dp
    val menuShadowY: Dp = 10.dp

    /** The three quick tiles under the remote screen (Right click, Keyboard, Audio). */
    val quickTileHeight: Dp = 52.dp

    /** A shortcut key on the Commands screen, and the hold-to-suspend button. */
    val commandKeyHeight: Dp = 62.dp

    /** The phone icon of the "turn the phone" hint. */
    val rotateIconSize: Dp = 26.dp

    /**
     * Two fingers on the remote screen: a change of their distance past this, before their centre
     * moves as far, is a pinch; otherwise they scroll.
     */
    val multiTouchSlop: Dp = 24.dp

    /** A second tap this close to the first, in the Direct touch mode, clicks on the first (about 60 px). */
    val doubleTapSlop: Dp = 24.dp

    /** Line icons in the session bars. */
    val barIconSize: Dp = 22.dp

    /** The g written on the launch screen. */
    val launchMarkSize: Dp = 104.dp

    /** Indeterminate loading bar. */
    val loadingBarWidth: Dp = 180.dp
    val loadingBarHeight: Dp = 3.dp

    /** Floating quick menu panel and its icons. */
    val quickMenuWidth: Dp = 220.dp
    val menuIconSize: Dp = 20.dp

    /** Tallest the keyboard sheet grows before its content scrolls. */
    val keyboardSheetMaxHeight: Dp = 420.dp

    /** The square dot before a PC's status line. */
    val statusDotSize: Dp = 8.dp

    /** The Gesto mark on the empty PCs list. */
    val emptyMarkSize: Dp = 72.dp

    /** The searching squares of Add a PC, and the square at their centre. */
    val radarSize: Dp = 150.dp
    val radarCoreSize: Dp = 26.dp

    /** The tinted square holding the icon of a found PC. */
    val foundIconBox: Dp = 40.dp

    /** One box of the PIN the PC shows. */
    val pinBoxWidth: Dp = 52.dp
    val pinBoxHeight: Dp = 64.dp

    /** One bar of the step indicator of Add a PC. */
    val stepBarWidth: Dp = 22.dp
    val stepBarHeight: Dp = 3.dp

    /** Room the page bar keeps opposite the back arrow, so its title stays centred. */
    val barSideSlot: Dp = 48.dp

    /** One segment of a segmented control (Quality, Theme…). */
    val segmentHeight: Dp = 40.dp

    /** The square switch, its knob and the gap around the knob. */
    val switchWidth: Dp = 42.dp
    val switchHeight: Dp = 24.dp
    val switchKnob: Dp = 16.dp
    val switchInset: Dp = 3.dp

    /** A slider's square thumb, the ring of page colour around it, and its track. */
    val sliderThumb: Dp = 20.dp
    val sliderThumbRing: Dp = 3.dp
    val sliderTrack: Dp = 4.dp

    /** The chevron at the end of a row that opens another page. */
    val chevronSize: Dp = 18.dp
}

val LocalGestoSpacing = staticCompositionLocalOf { GestoSpacing() }
