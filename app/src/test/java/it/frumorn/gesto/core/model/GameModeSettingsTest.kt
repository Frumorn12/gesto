package it.frumorn.gesto.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameModeSettingsTest {
    private val sharp = QualityPreset.Sharp.settings(showStats = false)

    @Test
    fun turningOnUsesTheSmoothPresetAndRemembersTheCurrentSettings() {
        val (next, saved) = gameModeChange(enable = true, current = sharp, saved = null)

        assertEquals(GameModePreset.copy(showStats = false), next)
        assertEquals(sharp, saved)
    }

    @Test
    fun gameModeStreamsWithTheSmoothPreset() {
        assertEquals(QualityPreset.Smooth, GameModePreset.preset)
        assertEquals(1280, GameModePreset.maxDimension)
        assertEquals(60, GameModePreset.maxFps)
        assertEquals(58, GameModePreset.jpegQuality)
    }

    @Test
    fun turningOffRestoresWhatWasThereBefore() {
        val (next, saved) = gameModeChange(enable = false, current = GameModePreset, saved = sharp)

        assertEquals(sharp.copy(showStats = GameModePreset.showStats), next)
        assertNull(saved)
    }

    @Test
    fun turningOffRestoresACustomQualityWithItsValues() {
        val custom = StreamSettings(QualityPreset.Custom, maxFps = 45, jpegQuality = 80, maxDimension = 2560, showStats = true)

        val (next, _) = gameModeChange(enable = false, current = GameModePreset, saved = custom)

        assertEquals(custom, next)
    }

    @Test
    fun turningOffWithNothingRememberedKeepsTheCurrentSettings() {
        val current = QualityPreset.Auto.settings(showStats = true)

        val (next, saved) = gameModeChange(enable = false, current = current, saved = null)

        assertEquals(current, next)
        assertNull(saved)
    }

    @Test
    fun turningOffAfterChoosingAnotherQualityKeepsThatChoice() {
        val chosen = QualityPreset.Saver.settings(showStats = true)

        val (next, saved) = gameModeChange(enable = false, current = chosen, saved = sharp)

        assertEquals(chosen, next)
        assertNull(saved)
    }

    @Test
    fun turningOnAgainKeepsTheFirstRememberedSettings() {
        val (_, saved) = gameModeChange(enable = true, current = GameModePreset, saved = sharp)

        assertEquals(sharp, saved)
    }

    @Test
    fun rememberedSettingsSurviveBeingStored() {
        val custom = StreamSettings(QualityPreset.Custom, maxFps = 45, jpegQuality = 80, maxDimension = 1920, showStats = false)

        assertEquals(custom, decodeStreamSettings(encodeStreamSettings(custom)))
        assertEquals(sharp, decodeStreamSettings(encodeStreamSettings(sharp)))
    }

    @Test
    fun anOldProfileRememberedBeforeTheUpdateComesBackAsAuto() {
        assertEquals(QualityPreset.Auto.settings(showStats = true), decodeStreamSettings("Quality;30;86;2400;true"))
    }

    @Test
    fun brokenStoredSettingsAreIgnored() {
        assertNull(decodeStreamSettings("nonsense"))
        assertNull(decodeStreamSettings("Auto;thirty;70;1600;true"))
        assertNull(decodeStreamSettings("Auto;30;70;1600;maybe"))
        assertNull(decodeStreamSettings(""))
    }
}
