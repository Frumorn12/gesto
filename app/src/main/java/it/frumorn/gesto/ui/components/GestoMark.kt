package it.frumorn.gesto.ui.components

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.animation.core.CubicBezierEasing
import it.frumorn.gesto.R
import it.frumorn.gesto.ui.theme.GestoMotion

/** The handwritten g, in a 108 x 108 viewport. Must match every `pathData` copy in `res/drawable`. */
const val GESTO_G_PATH =
    "M59,37 C53,30 40,31 37,42 C34,52 42,57 49,53 C55,49 58,41 59,33 C59,46 58,62 55,72 C52,82 41,84 39,78 C37,72 51,66 74,56"

private const val VIEWPORT = 108f
private const val STROKE_WIDTH = 6.5f
private const val CORNER_RADIUS = 26f
private const val WRITE_DELAY_MS = 150
private val GradientStart = Color(0xFF7A5CF6)
private val GradientEnd = Color(0xFF4E31D6)
private val WriteEasing = CubicBezierEasing(0.45f, 0.05f, 0.25f, 1f)

/** The app mark: purple rounded square with the white handwritten g. */
@Composable
fun GestoMark(modifier: Modifier = Modifier, size: Dp = 28.dp) {
    Image(
        painter = painterResource(R.drawable.ic_gesto_mark),
        contentDescription = "Gesto",
        modifier = modifier.size(size),
    )
}

/** The mark whose g is written in one stroke; static when [animate] is false or system animations are off. */
@Composable
fun WritingGestoMark(modifier: Modifier = Modifier, size: Dp = 104.dp, animate: Boolean = true) {
    val resolver = LocalContext.current.contentResolver
    val animated = animate &&
        Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    val progress = remember { Animatable(if (animated) 0f else 1f) }
    LaunchedEffect(animated) {
        if (animated) {
            progress.snapTo(0f)
            progress.animateTo(
                1f,
                tween(GestoMotion.WRITE_MS, delayMillis = WRITE_DELAY_MS, easing = WriteEasing),
            )
        } else {
            progress.snapTo(1f)
        }
    }
    val glyph = remember { PathParser().parsePathString(GESTO_G_PATH).toPath() }
    val measure = remember { PathMeasure().apply { setPath(glyph, false) } }
    Canvas(modifier.size(size)) {
        val factor = this.size.minDimension / VIEWPORT
        scale(factor, factor, pivot = Offset.Zero) {
            drawRoundRect(
                brush = Brush.linearGradient(
                    listOf(GradientStart, GradientEnd),
                    start = Offset.Zero,
                    end = Offset(VIEWPORT, VIEWPORT),
                ),
                size = Size(VIEWPORT, VIEWPORT),
                cornerRadius = CornerRadius(CORNER_RADIUS),
            )
            val partial = Path()
            measure.getSegment(0f, measure.length * progress.value, partial, true)
            drawPath(
                partial,
                Color.White,
                style = Stroke(STROKE_WIDTH, cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }
    }
}
