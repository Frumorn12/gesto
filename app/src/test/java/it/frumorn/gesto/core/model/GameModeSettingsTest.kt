package it.frumorn.gesto.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GameModeSettingsTest {
    private val sharp = StreamProfile.Quality.toStreamSettings(showStats = false)

    @Test
    fun turningOnUsesTheSmoothPresetAndRemembersTheCurrentSettings() {
        val (next, saved) = gameModeChange(enable = true, current = sharp, saved = null)

        assertEquals(GameModePreset.copy(showStats = false), next)
        assertEquals(sharp, saved)
    }

    @Test
    fun theSmoothPresetIsTheLowLatencyProfile() {
        assertEquals(StreamProfile.LowLatency, GameModePreset.profile)
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
    fun turningOffWithNothingRememberedKeepsTheCurrentSettings() {
        val current = StreamProfile.Balanced.toStreamSettings(showStats = true)

        val (next, saved) = gameModeChange(enable = false, current = current, saved = null)

        assertEquals(current, next)
        assertNull(saved)
    }

    @Test
    fun turningOffAfterChoosingAnotherQualityKeepsThatChoice() {
        val chosen = StreamProfile.Balanced.toStreamSettings(showStats = true)

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
    fun qualityCyclesAutomaticSmoothSharp() {
        assertEquals(StreamProfile.LowLatency, nextQuality(StreamProfile.Balanced))
        assertEquals(StreamProfile.Quality, nextQuality(StreamProfile.LowLatency))
        assertEquals(StreamProfile.Balanced, nextQuality(StreamProfile.Quality))
    }

    @Test
    fun theGameProfileCountsAsSmooth() {
        assertEquals(StreamProfile.LowLatency, shownQuality(StreamProfile.Game))
        assertEquals(StreamProfile.Quality, nextQuality(StreamProfile.Game))
    }

    @Test
    fun rememberedSettingsSurviveBeingStored() {
        val settings = StreamSettings(StreamProfile.Quality, maxFps = 45, jpegQuality = 80, maxDimension = 1920, showStats = false)

        assertEquals(settings, decodeStreamSettings(encodeStreamSettings(settings)))
    }

    @Test
    fun brokenStoredSettingsAreIgnored() {
        assertNull(decodeStreamSettings("nonsense"))
        assertNull(decodeStreamSettings("Unknown;30;70;1600;true"))
        assertNull(decodeStreamSettings(""))
    }
}
