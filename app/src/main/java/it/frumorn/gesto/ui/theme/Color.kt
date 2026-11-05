package it.frumorn.gesto.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/** Ecomic palette, light theme. Same family as Tratto. */
val GestoLightColors: ColorScheme = lightColorScheme(
    primary = Color(0xFF6546F3),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEFEAFE),
    onPrimaryContainer = Color(0xFF271E52),
    inversePrimary = Color(0xFFA491FA),
    secondary = Color(0xFF493990),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFEFEAFE),
    onSecondaryContainer = Color(0xFF271E52),
    tertiary = Color(0xFFA491FA),
    onTertiary = Color(0xFF1F1747),
    tertiaryContainer = Color(0xFFEFEAFE),
    onTertiaryContainer = Color(0xFF271E52),
    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1E1E1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1E1E1E),
    surfaceVariant = Color(0xFFF7F7F7),
    onSurfaceVariant = Color(0xFF4D4D4D),
    surfaceTint = Color(0xFF6546F3),
    inverseSurface = Color(0xFF1E1E1E),
    inverseOnSurface = Color(0xFFFFFFFF),
    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),
    outline = Color(0xFF999999),
    outlineVariant = Color(0xFFE0E0E0),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceDim = Color(0xFFEDEDED),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF7F7F7),
    surfaceContainerHigh = Color(0xFFF1F1F1),
    surfaceContainerHighest = Color(0xFFEBEBEB),
)

/** Ecomic palette, dark theme (deep violet ground). */
val GestoDarkColors: ColorScheme = darkColorScheme(
    primary = Color(0xFFA491FA),
    onPrimary = Color(0xFF1F1747),
    primaryContainer = Color(0xFF342A72),
    onPrimaryContainer = Color(0xFFEFEAFE),
    inversePrimary = Color(0xFF6546F3),
    secondary = Color(0xFFC9C2EC),
    onSecondary = Color(0xFF1F1747),
    secondaryContainer = Color(0xFF342A72),
    onSecondaryContainer = Color(0xFFEFEAFE),
    tertiary = Color(0xFFA491FA),
    onTertiary = Color(0xFF1F1747),
    tertiaryContainer = Color(0xFF342A72),
    onTertiaryContainer = Color(0xFFEFEAFE),
    background = Color(0xFF1F1747),
    onBackground = Color(0xFFF2F2F2),
    surface = Color(0xFF1F1747),
    onSurface = Color(0xFFF2F2F2),
    surfaceVariant = Color(0xFF271E52),
    onSurfaceVariant = Color(0xFFC9C2EC),
    surfaceTint = Color(0xFFA491FA),
    inverseSurface = Color(0xFFF2F2F2),
    inverseOnSurface = Color(0xFF1F1747),
    error = Color(0xFFFF8A80),
    onError = Color(0xFF410E0B),
    errorContainer = Color(0xFF5C1A15),
    onErrorContainer = Color(0xFFF9DEDC),
    outline = Color(0xFF8E86B8),
    outlineVariant = Color(0xFF3A3170),
    scrim = Color(0xFF000000),
    surfaceBright = Color(0xFF3A3170),
    surfaceDim = Color(0xFF1F1747),
    surfaceContainerLowest = Color(0xFF18123A),
    surfaceContainerLow = Color(0xFF231B4D),
    surfaceContainer = Color(0xFF271E52),
    surfaceContainerHigh = Color(0xFF2E2560),
    surfaceContainerHighest = Color(0xFF362C6C),
)

/** Colours Material 3 has no role for. */
@Immutable
data class GestoExtraColors(
    val success: Color,
    /** Letterbox behind the remote video; the same in both themes. */
    val video: Color,
    val onVideo: Color,
    val onVideoMuted: Color,
)

val GestoLightExtraColors = GestoExtraColors(
    success = Color(0xFF167D5A),
    video = Color(0xFF0E0B1F),
    onVideo = Color(0xFFE8E6F2),
    onVideoMuted = Color(0xFFA9A5C4),
)

val GestoDarkExtraColors = GestoExtraColors(
    success = Color(0xFF5FD3A5),
    video = Color(0xFF0E0B1F),
    onVideo = Color(0xFFE8E6F2),
    onVideoMuted = Color(0xFFA9A5C4),
)

/** Theme independent video colours, for code that has no composition (kept for existing call sites). */
val GestoVideoLetterbox: Color = GestoLightExtraColors.video
val GestoOnVideoLetterbox: Color = GestoLightExtraColors.onVideo
val GestoOnVideoLetterboxMuted: Color = GestoLightExtraColors.onVideoMuted
