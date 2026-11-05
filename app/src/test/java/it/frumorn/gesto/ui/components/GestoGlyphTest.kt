package it.frumorn.gesto.ui.components

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/** Keeps every copy of the handwritten g in sync with [GESTO_G_PATH]. */
class GestoGlyphTest {
    private fun glyphIn(name: String): String {
        val text = File("src/main/res/drawable/$name").readText()
        val paths = Regex("""android:pathData="([^"]+)"""").findAll(text).map { it.groupValues[1] }
        return paths.single { it.startsWith("M59,37") }
    }

    @Test fun foregroundMatches() = assertEquals(GESTO_G_PATH, glyphIn("ic_launcher_foreground.xml"))
    @Test fun monochromeMatches() = assertEquals(GESTO_G_PATH, glyphIn("ic_launcher_monochrome.xml"))
    @Test fun markMatches() = assertEquals(GESTO_G_PATH, glyphIn("ic_gesto_mark.xml"))
}
