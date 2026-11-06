package it.frumorn.gesto

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import it.frumorn.gesto.core.model.GameModePreset
import it.frumorn.gesto.core.model.StreamProfile
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
    fun firstRunDefaultsToGameMode60Fps() = runBlocking {
        val settings = repo.streamSettings.first()
        assertEquals(StreamProfile.Game, settings.profile)
        assertEquals(60, settings.maxFps)
        assertEquals(1280, settings.maxDimension)
    }

    @Test
    fun saveAndRestoreStreamSettings() = runBlocking {
        val custom = StreamProfile.LowLatency.toStreamSettings(showStats = false)
        repo.saveStreamSettings(custom)
        val loaded = repo.streamSettings.first()
        assertEquals(StreamProfile.LowLatency, loaded.profile)
        assertEquals(60, loaded.maxFps)
        assertEquals(false, loaded.showStats)
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
        val before = StreamProfile.Quality.toStreamSettings(showStats = false)
        repo.saveGameMode(true, GameModePreset, before)
        assertEquals(true, repo.gameMode.first())
        assertEquals(before, repo.settingsBeforeGame.first())
        assertEquals(GameModePreset.profile, repo.streamSettings.first().profile)

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
