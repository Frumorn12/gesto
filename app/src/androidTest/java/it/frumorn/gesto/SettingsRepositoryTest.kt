package it.frumorn.gesto

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import it.frumorn.gesto.core.model.GameModePreset
import it.frumorn.gesto.core.model.QualityPreset
import it.frumorn.gesto.core.model.StreamSettings
import it.frumorn.gesto.core.model.settings
import it.frumorn.gesto.core.model.TouchMode
import it.frumorn.gesto.core.storage.SettingsRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {
    private lateinit var context: Context
    private lateinit var repo: SettingsRepository

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Clear DataStore before each test
        context.filesDir.resolve("datastore/waypad_settings.preferences_pb").delete()
        repo = SettingsRepository(context)
    }

    @Test
    fun firstRunStartsOnAutomaticQuality() = runBlocking {
        assertEquals(QualityPreset.Auto.settings(showStats = true), repo.streamSettings.first())
    }

    @Test
    fun saveAndRestoreStreamSettings() = runBlocking {
        val smooth = QualityPreset.Smooth.settings(showStats = false)
        repo.saveStreamSettings(smooth)
        assertEquals(smooth, repo.streamSettings.first())
    }

    @Test
    fun customValuesAreKept() = runBlocking {
        val custom = StreamSettings(QualityPreset.Custom, maxFps = 45, jpegQuality = 80, maxDimension = 2560, showStats = true)
        repo.saveStreamSettings(custom)
        assertEquals(custom, repo.streamSettings.first())
    }

    @Test
    fun saveAndRestoreHaptics() = runBlocking {
        repo.saveHaptics(false)
        val loaded = repo.haptics.first()
        assertEquals(false, loaded)
    }

    @Test
    fun touchpadDefaults() = runBlocking {
        assertEquals(1f, repo.pointerSpeed.first())
        assertEquals(true, repo.pointerAcceleration.first())
        assertEquals(true, repo.twoFingerRightClick.first())
        assertEquals(0, repo.touchpadHintSessions.first())
    }

    @Test
    fun pointerSpeedIsKeptWithinItsRange() = runBlocking {
        repo.savePointerSpeed(9f)
        assertEquals(3f, repo.pointerSpeed.first())
        repo.savePointerSpeed(0.75f)
        assertEquals(0.75f, repo.pointerSpeed.first())
    }

    @Test
    fun saveAndRestoreGameMode() = runBlocking {
        val before = QualityPreset.Sharp.settings(showStats = false)
        repo.saveGameMode(true, GameModePreset, before)
        assertEquals(true, repo.gameMode.first())
        assertEquals(before, repo.settingsBeforeGame.first())
        assertEquals(GameModePreset.preset, repo.streamSettings.first().preset)

        repo.saveGameMode(false, before, null)
        assertEquals(false, repo.gameMode.first())
        assertEquals(null, repo.settingsBeforeGame.first())
    }

    @Test
    fun touchModeAndButtonPositionAreKept() = runBlocking {
        repo.saveTouchMode(TouchMode.Direct)
        assertEquals(TouchMode.Direct, repo.touchMode.first())
        repo.saveHudPosition(0.25f, 2f)
        assertEquals(0.25f to 1f, repo.hudPosition.first())
    }
}
