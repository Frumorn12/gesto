package it.frumorn.gesto.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/** Preset "spigolo" (default): every corner 2 dp. */
val ShapesSpigolo = Shapes(
    extraSmall = RoundedCornerShape(2.dp),
    small = RoundedCornerShape(2.dp),
    medium = RoundedCornerShape(2.dp),
    large = RoundedCornerShape(2.dp),
    extraLarge = RoundedCornerShape(2.dp),
)

/** Preset "arrotondato", the same scale as Tratto. */
val ShapesArrotondato = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

/** Shape tokens that have no slot in the standard Material 3 [Shapes] scale; they follow the preset. */
@Immutable
data class GestoExtraShapes(
    val pill: Shape,
    val slab: Shape,
    val viewport: Shape,
    /** Remote display when fullscreen: edge to edge, no corner. */
    val viewportFullscreen: Shape = RoundedCornerShape(0.dp),
    val overlay: Shape,
    val overlayCompact: Shape,
    val handle: Shape = RoundedCornerShape(2.dp),
)

val GestoExtraShapesSpigolo = GestoExtraShapes(
    pill = RoundedCornerShape(2.dp),
    slab = RoundedCornerShape(2.dp),
    viewport = RoundedCornerShape(2.dp),
    overlay = RoundedCornerShape(2.dp),
    overlayCompact = RoundedCornerShape(2.dp),
)

val GestoExtraShapesArrotondato = GestoExtraShapes(
    pill = RoundedCornerShape(percent = 50),
    slab = RoundedCornerShape(28.dp),
    viewport = RoundedCornerShape(14.dp),
    overlay = RoundedCornerShape(14.dp),
    overlayCompact = RoundedCornerShape(10.dp),
)

val LocalGestoExtraShapes = staticCompositionLocalOf { GestoExtraShapesSpigolo }
