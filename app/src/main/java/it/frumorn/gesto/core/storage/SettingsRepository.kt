package it.frumorn.gesto.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.frumorn.gesto.SessionMode
import it.frumorn.gesto.core.model.StreamProfile
import it.frumorn.gesto.core.model.StreamSettings
import it.frumorn.gesto.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "waypad_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        val STREAM_PROFILE = stringPreferencesKey("stream_profile")
        val STREAM_MAX_FPS = intPreferencesKey("stream_max_fps")
        val STREAM_JPEG_QUALITY = intPreferencesKey("stream_jpeg_quality")
        val STREAM_MAX_DIMENSION = intPreferencesKey("stream_max_dimension")
        val SHOW_STATS = booleanPreferencesKey("show_stats")
        val HAPTICS = booleanPreferencesKey("haptics")
        val GAME_MODE = booleanPreferencesKey("game_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ROUNDED_CORNERS = booleanPreferencesKey("rounded_corners")
        val FIRST_RUN = booleanPreferencesKey("first_run")
        val LAST_MODE = stringPreferencesKey("last_mode")
    }

    val streamSettings: Flow<StreamSettings> = context.dataStore.data.map { prefs ->
        val isFirstRun = prefs[Keys.FIRST_RUN] != false
        val profileName = prefs[Keys.STREAM_PROFILE]
        val profile = StreamProfile.entries.find { it.name == profileName }

        if (isFirstRun || profile == null) {
            // First run or missing data: default to Game mode (60 fps) for best controller experience
            StreamProfile.Game.toStreamSettings(showStats = prefs[Keys.SHOW_STATS] ?: true)
        } else {
            StreamSettings(
                profile = profile,
                maxFps = prefs[Keys.STREAM_MAX_FPS] ?: profile.defaultFps,
                jpegQuality = prefs[Keys.STREAM_JPEG_QUALITY] ?: profile.defaultQuality,
                maxDimension = prefs[Keys.STREAM_MAX_DIMENSION] ?: profile.defaultMaxDimension,
                showStats = prefs[Keys.SHOW_STATS] ?: true,
            )
        }
    }

    val haptics: Flow<Boolean> = context.dataStore.data.map { it[Keys.HAPTICS] ?: true }
    val gameMode: Flow<Boolean> = context.dataStore.data.map { it[Keys.GAME_MODE] ?: false }

    val themeMode: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        ThemeMode.entries.find { it.name == prefs[Keys.THEME_MODE] } ?: ThemeMode.System
    }
    val roundedCorners: Flow<Boolean> = context.dataStore.data.map { it[Keys.ROUNDED_CORNERS] ?: false }

    /** The session mode the app reopens on; Touchpad until one is used, or for an unknown value. */
    val lastMode: Flow<SessionMode> = context.dataStore.data.map { prefs ->
        SessionMode.entries.find { it.name == prefs[Keys.LAST_MODE] } ?: SessionMode.Touchpad
    }

    suspend fun saveLastMode(mode: SessionMode) {
        context.dataStore.edit { prefs -> prefs[Keys.LAST_MODE] = mode.name }
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.dataStore.edit { prefs -> prefs[Keys.THEME_MODE] = mode.name }
    }

    suspend fun saveRoundedCorners(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.ROUNDED_CORNERS] = enabled }
    }

    suspend fun saveStreamSettings(settings: StreamSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.STREAM_PROFILE] = settings.profile.name
            prefs[Keys.STREAM_MAX_FPS] = settings.maxFps
            prefs[Keys.STREAM_JPEG_QUALITY] = settings.jpegQuality
            prefs[Keys.STREAM_MAX_DIMENSION] = settings.maxDimension
            prefs[Keys.SHOW_STATS] = settings.showStats
            prefs[Keys.FIRST_RUN] = false
        }
    }

    suspend fun saveHaptics(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAPTICS] = enabled
        }
    }

    suspend fun saveGameMode(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GAME_MODE] = enabled
            prefs[Keys.FIRST_RUN] = false
        }
    }
}
