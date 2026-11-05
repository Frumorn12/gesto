package it.frumorn.gesto.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.pow

/** WCAG 2.x relative luminance of an sRGB colour (alpha ignored). */
private fun relativeLuminance(c: Color): Double {
    fun channel(v: Float): Double {
        val s = v.toDouble()
        return if (s <= 0.03928) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
    }
    return 0.2126 * channel(c.red) + 0.7152 * channel(c.green) + 0.0722 * channel(c.blue)
}

/** WCAG contrast ratio between two colours, from 1.0 (identical) to 21.0 (black on white). */
fun contrastRatio(a: Color, b: Color): Double {
    val la = relativeLuminance(a)
    val lb = relativeLuminance(b)
    return (maxOf(la, lb) + 0.05) / (minOf(la, lb) + 0.05)
}
