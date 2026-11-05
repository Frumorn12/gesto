package it.frumorn.gesto.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Gesto's Material 3 Expressive theme.
 *
 * * Colour comes from the wallpaper on Android 12+ ([dynamicLightColorScheme] /
 *   [dynamicDarkColorScheme]); older devices fall back to [GestoLightColorScheme] /
 *   [GestoDarkColorScheme].
 * * Light and dark both follow the system setting by default.
 * * `MaterialExpressiveTheme` and `MotionScheme` are `internal` in material3 1.4.0 (the version the
 *   Compose BOM 2026.02.01 resolves), so the expressive layer is applied through the public
 *   `MaterialTheme` entry point plus Gesto's own expressive tokens: [GestoShapes] for the rounder
 *   shape scale, [GestoTypography] for the weight contrast and [GestoMotion] for the expressive
 *   spring scheme.
 */
@Composable
fun GestoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme: ColorScheme = remember(darkTheme, dynamicColor, context) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

            darkTheme -> GestoDarkColorScheme
            else -> GestoLightColorScheme
        }
    }
    CompositionLocalProvider(
        LocalGestoSpacing provides GestoSpacing(),
        LocalGestoExtraShapes provides GestoExtraShapes(),
        LocalGestoAccentTypography provides GestoAccentTypography(),
        LocalGestoMotion provides GestoMotion(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = GestoShapes,
            typography = GestoTypography,
            content = content,
        )
    }
}

/** Accessors for the Gesto design tokens that Material 3 does not model publicly. */
object GestoTheme {
    val spacing: GestoSpacing
        @Composable @ReadOnlyComposable get() = LocalGestoSpacing.current

    val shapes: GestoExtraShapes
        @Composable @ReadOnlyComposable get() = LocalGestoExtraShapes.current

    val accentTypography: GestoAccentTypography
        @Composable @ReadOnlyComposable get() = LocalGestoAccentTypography.current

    val motion: GestoMotion
        @Composable @ReadOnlyComposable get() = LocalGestoMotion.current
}
