package it.frumorn.gesto.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring

/**
 * Gesto motion: Ecomic easings and durations, plus the spring helpers older screens still use
 * (spring constants follow the Material 3 expressive motion scheme, which is internal in 1.4.0).
 */
object GestoMotion {
    val Enter: Easing = CubicBezierEasing(0.2f, 0.7f, 0.2f, 1f)
    val Exit: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

    const val ENTER_MS = 320
    const val EXIT_MS = 190
    const val MENU_MS = 320
    const val SHEET_MS = 350
    const val PULSE_MS = 1800
    const val WRITE_MS = 1300
    const val ENTER_OFFSET_DP = 10

    fun <T> defaultSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 380f)
    fun <T> fastSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.6f, stiffness = 800f)
    fun <T> slowSpatial(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.8f, stiffness = 200f)
    fun <T> defaultEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 1600f)
    fun <T> fastEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 3800f)
    fun <T> slowEffects(): FiniteAnimationSpec<T> = spring(dampingRatio = 1f, stiffness = 800f)
}
