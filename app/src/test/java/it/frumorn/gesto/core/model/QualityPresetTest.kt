package it.frumorn.gesto.core.model

import org.junit.Assert.assertEquals
import org.junit.Test

class QualityPresetTest {
    /** Long side, fps and quality of [settings], in the order the plan lists them. */
    private fun values(settings: StreamSettings) = Triple(settings.maxDimension, settings.maxFps, settings.jpegQuality)

    @Test
    fun autoStartsAt1080p60() {
        assertEquals(Triple(1920, 60, 70), values(QualityPreset.Auto.settings(showStats = true)))
    }

    @Test
    fun autoSecondRung() {
        assertEquals(Triple(1600, 60, 62), values(QualityPreset.Auto.settings(rung = 1, showStats = true)))
    }

    @Test
    fun autoThirdRung() {
        assertEquals(Triple(1280, 60, 58), values(QualityPreset.Auto.settings(rung = 2, showStats = true)))
    }

    @Test
    fun autoLastRung() {
        assertEquals(Triple(1280, 30, 52), values(QualityPreset.Auto.settings(rung = 3, showStats = true)))
    }

    @Test
    fun autoRungsOutsideTheLadderStayOnIt() {
        assertEquals(values(QualityPreset.Auto.settings(rung = 3, showStats = true)), values(QualityPreset.Auto.settings(rung = 9, showStats = true)))
        assertEquals(values(QualityPreset.Auto.settings(rung = 0, showStats = true)), values(QualityPreset.Auto.settings(rung = -1, showStats = true)))
    }

    @Test
    fun smooth() {
        assertEquals(Triple(1280, 60, 58), values(QualityPreset.Smooth.settings(showStats = true)))
    }

    @Test
    fun sharp() {
        assertEquals(Triple(2400, 30, 86), values(QualityPreset.Sharp.settings(showStats = true)))
    }

    @Test
    fun saver() {
        assertEquals(Triple(1280, 30, 52), values(QualityPreset.Saver.settings(showStats = true)))
    }

    @Test
    fun theRungOnlyMattersToAuto() {
        assertEquals(QualityPreset.Sharp.settings(showStats = true), QualityPreset.Sharp.settings(rung = 2, showStats = true))
    }

    @Test
    fun customUsesTheSavedValues() {
        val saved = StreamSettings(QualityPreset.Custom, maxFps = 45, jpegQuality = 80, maxDimension = 2560, showStats = true)

        val settings = QualityPreset.Custom.settings(custom = saved, showStats = false)

        assertEquals(Triple(2560, 45, 80), values(settings))
        assertEquals(QualityPreset.Custom, settings.preset)
        assertEquals(false, settings.showStats)
    }

    @Test
    fun customKeepsItsValuesToTheOffers() {
        val odd = StreamSettings(QualityPreset.Custom, maxFps = 50, jpegQuality = 99, maxDimension = 2400, showStats = true)

        // Fps 30/45/60, long side 1280/1600/1920/2560/3840, quality 35 to 92.
        assertEquals(Triple(2560, 45, 92), values(QualityPreset.Custom.settings(custom = odd, showStats = true)))
        assertEquals(35, QualityPreset.Custom.settings(custom = odd.copy(jpegQuality = 10), showStats = true).jpegQuality)
    }

    @Test
    fun customWithNothingSavedStartsFromAuto() {
        assertEquals(Triple(1920, 60, 70), values(QualityPreset.Custom.settings(showStats = true)))
    }

    @Test
    fun everyPresetCarriesItsNameAndTheStatsSwitch() {
        for (preset in QualityPreset.entries) {
            val settings = preset.settings(showStats = false)
            assertEquals(preset, settings.preset)
            assertEquals(false, settings.showStats)
        }
    }

    @Test
    fun theDefaultSettingsAreAuto() {
        assertEquals(QualityPreset.Auto.settings(showStats = true), StreamSettings())
    }

    @Test
    fun autoStreamsAtItsRungOthersAsChosen() {
        val auto = QualityPreset.Auto.settings(showStats = false)
        val sharp = QualityPreset.Sharp.settings(showStats = false)

        assertEquals(QualityPreset.Auto.settings(rung = 2, showStats = false), auto.atRung(2))
        assertEquals(sharp, sharp.atRung(2))
    }

    @Test
    fun storedPresetsAreReadBack() {
        assertEquals(QualityPreset.Sharp.settings(showStats = false), storedStreamSettings("Sharp", null, null, null, showStats = false))
        assertEquals(
            QualityPreset.Custom.settings(custom = StreamSettings(QualityPreset.Custom, 30, 40, 1600, true), showStats = true),
            storedStreamSettings("Custom", customFps = 30, customDimension = 1600, customQuality = 40, showStats = true),
        )
    }

    @Test
    fun unknownOrOldStoredNamesAreAuto() {
        // The names of the old stream profiles, and anything else, fall back to Auto.
        for (name in listOf(null, "", "Balanced", "Quality", "LowLatency", "Game", "nonsense")) {
            assertEquals(QualityPreset.Auto.settings(showStats = true), storedStreamSettings(name, 30, 1280, 52, showStats = true))
        }
    }

    @Test
    fun theQuickMenuCyclesAutoSmoothSharpSaver() {
        assertEquals(QualityPreset.Smooth, nextQuality(QualityPreset.Auto))
        assertEquals(QualityPreset.Sharp, nextQuality(QualityPreset.Smooth))
        assertEquals(QualityPreset.Saver, nextQuality(QualityPreset.Sharp))
        assertEquals(QualityPreset.Auto, nextQuality(QualityPreset.Saver))
    }

    @Test
    fun customIsLeftForAuto() {
        assertEquals(QualityPreset.Auto, nextQuality(QualityPreset.Custom))
    }

    @Test
    fun theLinesOfAPictureAreThoseOfA16By9Screen() {
        assertEquals(1080, StreamSettings(maxDimension = 1920).videoLines)
        assertEquals(720, StreamSettings(maxDimension = 1280).videoLines)
        assertEquals(1440, StreamSettings(maxDimension = 2560).videoLines)
    }
}
