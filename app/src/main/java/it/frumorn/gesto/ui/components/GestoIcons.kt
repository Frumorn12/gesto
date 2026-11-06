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
    /** The speaker with a cross: PC audio off. */
    val Mute: ImageVector by lazy { lineIcon("Mute", "M4 9h4l5-4v14l-5-4H4z", "M17 9l5 6M22 9l-5 6") }
    /** A phone on its side with an arrow: turn the phone. */
    val Rotate: ImageVector by lazy { lineIcon("Rotate", rect(4, 7, 16, 10), "M8 3h8M16 3l-2-2M16 3l-2 2") }
    /** Three small squares: the "⋯" of a card's menu. */
    val More: ImageVector by lazy {
        lineIcon("More", "M4.5 11.5h1v1h-1z", "M11.5 11.5h1v1h-1z", "M18.5 11.5h1v1h-1z")
    }
    /** The power symbol, for waking a PC. */
    val Wake: ImageVector by lazy { lineIcon("Wake", "M12 3v8", "M7.5 6.5a7 7 0 1 0 9 0") }
    /** A monitor on its stand: a PC found on the network. */
    val Pc: ImageVector by lazy { lineIcon("Pc", rect(3, 4, 18, 12), "M9 20h6M12 16v4") }
    /** Three finder squares and a dot: scan a QR. */
    val Qr: ImageVector by lazy {
        lineIcon("Qr", rect(4, 4, 6, 6), rect(14, 4, 6, 6), rect(4, 14, 6, 6), "M14 14h2v2h-2zM18 18h2v2h-2zM14 20h2M20 14v2")
    }
    /** A pencil: type something in. */
    val Edit: ImageVector by lazy { lineIcon("Edit", "M4 20h4L19 9l-4-4L4 16z", "M13 7l4 4") }
    val Chevron: ImageVector by lazy { lineIcon("Chevron", "M9 5l7 7-7 7") }
    val Refresh: ImageVector by lazy { lineIcon("Refresh", "M20 12a8 8 0 1 1-2.34-5.66", "M20 4v5h-5") }
    val Play: ImageVector by lazy { lineIcon("Play", "M8 5l11 7-11 7z", "M5 5v14") }
    val Previous: ImageVector by lazy { lineIcon("Previous", "M18 5L8 12l10 7z", "M6 5v14") }
    val Next: ImageVector by lazy { lineIcon("Next", "M6 5l10 7-10 7z", "M18 5v14") }
    val Minus: ImageVector by lazy { lineIcon("Minus", "M5 12h14") }
    val Brightness: ImageVector by lazy {
        lineIcon("Brightness", rect(8, 8, 8, 8), "M12 2v3M12 19v3M2 12h3M19 12h3M5 5l2 2M17 17l2 2M5 19l2-2M17 7l2-2")
    }
    val Copy: ImageVector by lazy { lineIcon("Copy", rect(8, 8, 12, 12), "M16 8V4H4v12h4") }
    val Paste: ImageVector by lazy { lineIcon("Paste", rect(5, 5, 14, 16), rect(9, 3, 6, 4)) }
    val Undo: ImageVector by lazy { lineIcon("Undo", "M9 6L4 11l5 5", "M4 11h10a5 5 0 0 1 0 10h-3") }
    val Save: ImageVector by lazy { lineIcon("Save", "M4 4h13l3 3v13H4z", rect(8, 4, 8, 5), rect(7, 13, 10, 7)) }
    val Terminal: ImageVector by lazy { lineIcon("Terminal", rect(3, 4, 18, 16), "M7 9l3 3-3 3M13 15h4") }
    val Launcher: ImageVector by lazy { lineIcon("Launcher", "M11 4a7 7 0 1 0 0 14 7 7 0 0 0 0-14z", "M16 16l5 5") }
    val Escape: ImageVector by lazy { lineIcon("Escape", rect(3, 6, 18, 12), "M8 10l2 2-2 2M13 14h3") }
    val Lock: ImageVector by lazy { lineIcon("Lock", rect(5, 11, 14, 9), "M8 11V7a4 4 0 0 1 8 0v4") }
    val Moon: ImageVector by lazy { lineIcon("Moon", "M20 14A8 8 0 1 1 10 4a6 6 0 0 0 10 10z") }
    val Apps: ImageVector by lazy { lineIcon("Apps", rect(4, 4, 6, 6), rect(14, 4, 6, 6), rect(4, 14, 6, 6), "M14 17h6M17 14v6") }
    /** An arrow up and an arrow down: drag here to scroll. */
    val ScrollVertical: ImageVector by lazy { lineIcon("ScrollVertical", "M12 4v16", "M8 8l4-4 4 4", "M8 16l4 4 4-4") }
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
            listOf(GestoIcons.Touch, GestoIcons.Quality, GestoIcons.Game, GestoIcons.Exit, GestoIcons.More, GestoIcons.Wake)
                .forEach { Icon(it, contentDescription = null) }
        }
        Row {
            listOf(GestoIcons.Pc, GestoIcons.Qr, GestoIcons.Edit, GestoIcons.Chevron, GestoIcons.Mute, GestoIcons.Rotate)
                .forEach { Icon(it, contentDescription = null) }
        }
    }
}
