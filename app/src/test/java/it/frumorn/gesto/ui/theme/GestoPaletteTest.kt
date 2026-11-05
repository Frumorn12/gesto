package it.frumorn.gesto.ui.theme

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GestoPaletteTest {
    @Test
    fun lightPaletteMatchesEcomic() {
        assertEquals(Color(0xFF6546F3), GestoLightColors.primary)
        assertEquals(Color(0xFF1E1E1E), GestoLightColors.onBackground)
    }

    @Test
    fun darkPaletteMatchesEcomic() {
        assertEquals(Color(0xFFA491FA), GestoDarkColors.primary)
        assertEquals(Color(0xFF1F1747), GestoDarkColors.background)
    }

    @Test
    fun primaryReadableOnBackgroundInBothThemes() {
        assertTrue(contrastRatio(GestoLightColors.primary, GestoLightColors.background) >= 4.5)
        assertTrue(contrastRatio(GestoDarkColors.primary, GestoDarkColors.background) >= 4.5)
    }

    @Test
    fun onPrimaryReadableOnPrimaryInBothThemes() {
        assertTrue(contrastRatio(GestoLightColors.onPrimary, GestoLightColors.primary) >= 4.5)
        assertTrue(contrastRatio(GestoDarkColors.onPrimary, GestoDarkColors.primary) >= 4.5)
    }

    @Test
    fun contrastRatioMatchesWcagReferenceValues() {
        assertEquals(21.0, contrastRatio(Color.Black, Color.White), 0.01)
        assertEquals(1.0, contrastRatio(Color.White, Color.White), 0.001)
    }
}
