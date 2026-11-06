package it.frumorn.gesto.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import it.frumorn.gesto.ui.theme.GestoPreviewSurface

/**
 * Gesto line icons: 24 x 24, 1.75 stroke, square caps and mitred joins, drawn like the prototype.
 * Material's outlined set has rounded ends, which clash with the 2 dp corners.
 */
object GestoIcons {
    val Back: ImageVector by lazy { lineIcon("Back", "M15 5l-7 7 7 7") }
    val Settings: ImageVector by lazy {
        lineIcon(
            "Settings",
            "M4 6h5M13 6h7M4 12h11M19 12h1M4 18h3M11 18h9",
            rect(9, 4, 4, 4), rect(15, 10, 4, 4), rect(7, 16, 4, 4),
        )
    }
    val Plus: ImageVector by lazy { lineIcon("Plus", "M12 5v14M5 12h14") }
    val Touchpad: ImageVector by lazy { lineIcon("Touchpad", rect(3, 3, 18, 13), "M3 20h7M14 20h7") }
    val Screen: ImageVector by lazy { lineIcon("Screen", rect(3, 4, 18, 12), "M8 20h8M12 16v4") }
    val Keyboard: ImageVector by lazy {
        lineIcon("Keyboard", rect(2, 6, 20, 12), "M6 10h1M10 10h1M14 10h1M18 10h1M7 14h10")
    }
    val Commands: ImageVector by lazy {
        lineIcon("Commands", rect(4, 4, 6, 6), rect(14, 4, 6, 6), rect(4, 14, 6, 6), rect(14, 14, 6, 6))
    }
    val Close: ImageVector by lazy { lineIcon("Close", "M6 6l12 12M18 6L6 18") }
    val RightClick: ImageVector by lazy {
        lineIcon("RightClick", rect(6, 3, 12, 18), "M12 3v7M6 10h12", rect(12, 3, 6, 7))
    }
    val Volume: ImageVector by lazy {
        lineIcon("Volume", "M4 9h4l5-4v14l-5-4H4z", "M16 9c1.5 1.5 1.5 4.5 0 6M19 6c3 3 3 9 0 12")
    }
    val Touch: ImageVector by lazy { lineIcon("Touch", rect(9, 3, 6, 6), "M12 9v12M8 15h8") }
    val Quality: ImageVector by lazy {
        lineIcon("Quality", rect(8, 8, 8, 8), "M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M5 19l2-2M17 7l2-2")
    }
    val Game: ImageVector by lazy { lineIcon("Game", rect(2, 7, 20, 11), "M7 10v5M4.5 12.5h5M15 11h1M18 14h1") }
    val Exit: ImageVector by lazy { lineIcon("Exit", "M10 4H4v16h6M14 8l4 4-4 4M18 12H9") }
}

private fun rect(x: Int, y: Int, w: Int, h: Int) = "M$x ${y}h${w}v${h}h-${w}z"

private fun lineIcon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = "Gesto.$name",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        paths.forEach { data ->
            addPath(
                pathData = addPathNodes(data),
                fill = null,
                // Tinted by Icon; the colour here only has to be opaque.
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 1.75f,
                strokeLineCap = StrokeCap.Square,
                strokeLineJoin = StrokeJoin.Miter,
            )
        }
    }.build()

@Preview(name = "Gesto icons")
@Composable
private fun GestoIconsPreview() = GestoPreviewSurface(darkTheme = false) {
    Column {
        Row {
            listOf(GestoIcons.Back, GestoIcons.Settings, GestoIcons.Plus, GestoIcons.Touchpad, GestoIcons.Screen)
                .forEach { Icon(it, contentDescription = null) }
        }
        Row {
            listOf(GestoIcons.Keyboard, GestoIcons.Commands, GestoIcons.Close, GestoIcons.RightClick, GestoIcons.Volume)
                .forEach { Icon(it, contentDescription = null) }
        }
        Row {
            listOf(GestoIcons.Touch, GestoIcons.Quality, GestoIcons.Game, GestoIcons.Exit)
                .forEach { Icon(it, contentDescription = null) }
        }
    }
}
