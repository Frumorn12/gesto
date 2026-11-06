package it.frumorn.gesto.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import it.frumorn.gesto.R

val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.Normal),
    Font(R.font.manrope_medium, FontWeight.Medium),
    Font(R.font.manrope_semibold, FontWeight.SemiBold),
    Font(R.font.manrope_bold, FontWeight.Bold),
)

private fun style(
    size: Int,
    line: Int,
    weight: FontWeight,
    tracking: TextUnit = 0.sp,
) = TextStyle(
    fontFamily = Manrope,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = line.sp,
    letterSpacing = tracking,
)

/** Titles Regular with negative tracking, buttons Medium, emphasis SemiBold. */
val GestoTypography: Typography = Typography(
    displayLarge = style(44, 52, FontWeight.Normal, (-0.025).em),
    displayMedium = style(40, 48, FontWeight.Normal, (-0.025).em),
    displaySmall = style(36, 44, FontWeight.Normal, (-0.025).em),
    headlineLarge = style(32, 40, FontWeight.Normal, (-0.025).em),
    headlineMedium = style(28, 36, FontWeight.Normal, (-0.02).em),
    headlineSmall = style(24, 32, FontWeight.Normal, (-0.02).em),
    titleLarge = style(22, 28, FontWeight.SemiBold, (-0.015).em),
    titleMedium = style(16, 24, FontWeight.SemiBold, (-0.01).em),
    titleSmall = style(14, 20, FontWeight.SemiBold),
    bodyLarge = style(16, 24, FontWeight.Normal),
    bodyMedium = style(14, 20, FontWeight.Normal),
    bodySmall = style(12, 16, FontWeight.Normal),
    labelLarge = style(15, 20, FontWeight.Medium),
    labelMedium = style(12, 16, FontWeight.SemiBold),
    labelSmall = style(11, 16, FontWeight.SemiBold, 0.1.em),
)

/** Type roles that do not exist in the Material 3 scale but that Gesto needs. */
@Immutable
data class GestoAccentTypography(
    /** Wordmark shown in the top app bar. */
    val brand: TextStyle = style(18, 24, FontWeight.SemiBold, (-0.01).em),
    /** Host key fingerprints: must be monospaced so digits line up. */
    val fingerprint: TextStyle = style(12, 16, FontWeight.Medium).copy(fontFamily = FontFamily.Monospace),
    /** Read-outs in the diagnostics HUDs. */
    val telemetry: TextStyle = style(11, 16, FontWeight.Medium).copy(fontFamily = FontFamily.Monospace),
    /** One digit of the pairing PIN. */
    val pinDigit: TextStyle = style(30, 36, FontWeight.SemiBold),
    /** A command to type on the PC (`gesto pair`). */
    val command: TextStyle = style(14, 20, FontWeight.Medium).copy(fontFamily = FontFamily.Monospace),
)

val LocalGestoAccentTypography = staticCompositionLocalOf { GestoAccentTypography() }
