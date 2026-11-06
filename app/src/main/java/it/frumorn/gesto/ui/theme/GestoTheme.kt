package it.frumorn.gesto.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

enum class ThemeMode { System, Light, Dark }

val LocalGestoExtraColors = staticCompositionLocalOf { GestoLightExtraColors }

/**
 * Gesto theme: Ecomic palette, Manrope, 2 dp corners by default (or the "arrotondato" preset).
 * No dynamic color.
 */
@Composable
fun GestoTheme(
    mode: ThemeMode = ThemeMode.System,
    roundedCorners: Boolean = false,
    content: @Composable () -> Unit,
) {
    val dark = mode.isDark()
    CompositionLocalProvider(
        LocalGestoSpacing provides GestoSpacing(),
        LocalGestoExtraShapes provides
            (if (roundedCorners) GestoExtraShapesArrotondato else GestoExtraShapesSpigolo),
        LocalGestoAccentTypography provides GestoAccentTypography(),
        LocalGestoExtraColors provides (if (dark) GestoDarkExtraColors else GestoLightExtraColors),
    ) {
        MaterialTheme(
            colorScheme = if (dark) GestoDarkColors else GestoLightColors,
            shapes = if (roundedCorners) ShapesArrotondato else ShapesSpigolo,
            typography = GestoTypography,
            content = content,
        )
    }
}

/** Whether this mode resolves to the dark palette right now. */
@Composable
fun ThemeMode.isDark(): Boolean = when (this) {
    ThemeMode.System -> isSystemInDarkTheme()
    ThemeMode.Light -> false
    ThemeMode.Dark -> true
}

/** Accessors for the Gesto design tokens that Material 3 does not model publicly. */
object GestoTheme {
    val colors: GestoExtraColors
        @Composable @ReadOnlyComposable get() = LocalGestoExtraColors.current

    val spacing: GestoSpacing
        @Composable @ReadOnlyComposable get() = LocalGestoSpacing.current

    val shapes: GestoExtraShapes
        @Composable @ReadOnlyComposable get() = LocalGestoExtraShapes.current

    val accentTypography: GestoAccentTypography
        @Composable @ReadOnlyComposable get() = LocalGestoAccentTypography.current

    val motion: GestoMotion
        @Composable @ReadOnlyComposable get() = GestoMotion
}
