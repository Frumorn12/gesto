package it.frumorn.gesto.core.storage

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import it.frumorn.gesto.SessionMode
import it.frumorn.gesto.core.input.PointerAcceleration
import it.frumorn.gesto.core.model.QualityPreset
import it.frumorn.gesto.core.model.StreamSettings
import it.frumorn.gesto.core.model.TouchMode
import it.frumorn.gesto.core.model.decodeStreamSettings
import it.frumorn.gesto.core.model.encodeStreamSettings
import it.frumorn.gesto.core.model.storedStreamSettings
import it.frumorn.gesto.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "waypad_settings")

class SettingsRepository(private val context: Context) {
    private object Keys {
        // The old "stream_profile" and "stream_*" keys are no longer read: an update starts on Auto.
        val QUALITY_PRESET = stringPreferencesKey("quality_preset")
        val CUSTOM_FPS = intPreferencesKey("custom_fps")
        val CUSTOM_DIMENSION = intPreferencesKey("custom_dimension")
        val CUSTOM_QUALITY = intPreferencesKey("custom_quality")
        val SHOW_STATS = booleanPreferencesKey("show_stats")
        val HAPTICS = booleanPreferencesKey("haptics")
        val GAME_MODE = booleanPreferencesKey("game_mode")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ROUNDED_CORNERS = booleanPreferencesKey("rounded_corners")
        val LAST_MODE = stringPreferencesKey("last_mode")
        val POINTER_SPEED = floatPreferencesKey("pointer_speed")
        val POINTER_ACCELERATION = booleanPreferencesKey("pointer_acceleration")
        val TWO_FINGER_RIGHT_CLICK = booleanPreferencesKey("two_finger_right_click")
        val TOUCHPAD_HINT_SESSIONS = intPreferencesKey("touchpad_hint_sessions")
        val TOUCH_MODE = stringPreferencesKey("touch_mode")
        val HUD_X = floatPreferencesKey("hud_x")
        val HUD_Y = floatPreferencesKey("hud_y")
        val HUD_HINT_SESSIONS = intPreferencesKey("hud_hint_sessions")
        val SETTINGS_BEFORE_GAME = stringPreferencesKey("settings_before_game")
    }

    /** The chosen quality preset (Auto until one is chosen, or for a name it doesn't know) and the stats switch. */
    val streamSettings: Flow<StreamSettings> = context.dataStore.data.map { prefs ->
        storedStreamSettings(
            presetName = prefs[Keys.QUALITY_PRESET],
            customFps = prefs[Keys.CUSTOM_FPS],
            customDimension = prefs[Keys.CUSTOM_DIMENSION],
            customQuality = prefs[Keys.CUSTOM_QUALITY],
            showStats = prefs[Keys.SHOW_STATS] ?: true,
        )
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

    /** Touchpad pointer speed, 0.5 to 3.0; 1.0 until it is changed. */
    val pointerSpeed: Flow<Float> = context.dataStore.data.map { prefs ->
        PointerAcceleration.clampSpeed(prefs[Keys.POINTER_SPEED] ?: PointerAcceleration.DEFAULT_SPEED)
    }
    val pointerAcceleration: Flow<Boolean> = context.dataStore.data.map { it[Keys.POINTER_ACCELERATION] ?: true }
    val twoFingerRightClick: Flow<Boolean> = context.dataStore.data.map { it[Keys.TWO_FINGER_RIGHT_CLICK] ?: true }

    /** How many sessions have shown the touchpad's gesture hints. */
    val touchpadHintSessions: Flow<Int> = context.dataStore.data.map { it[Keys.TOUCHPAD_HINT_SESSIONS] ?: 0 }

    /** How a finger drives the PC on the remote screen; Touchpad until it is changed. */
    val touchMode: Flow<TouchMode> = context.dataStore.data.map { prefs ->
        TouchMode.entries.find { it.name == prefs[Keys.TOUCH_MODE] } ?: TouchMode.Touchpad
    }

    /**
     * Where the quick-menu button sits, as fractions (0..1) of the room it can move in; the
     * bottom-right corner until it is dragged.
     */
    val hudPosition: Flow<Pair<Float, Float>> = context.dataStore.data.map { prefs ->
        (prefs[Keys.HUD_X] ?: 1f).coerceIn(0f, 1f) to (prefs[Keys.HUD_Y] ?: 1f).coerceIn(0f, 1f)
    }

    /** How many sessions have pulsed the quick-menu button to call attention to it. */
    val hudHintSessions: Flow<Int> = context.dataStore.data.map { it[Keys.HUD_HINT_SESSIONS] ?: 0 }

    /** The stream settings Game mode replaced, to put back when it goes off; null when it is off. */
    val settingsBeforeGame: Flow<StreamSettings?> = context.dataStore.data.map { prefs ->
        prefs[Keys.SETTINGS_BEFORE_GAME]?.let(::decodeStreamSettings)
    }

    suspend fun saveTouchMode(mode: TouchMode) {
        context.dataStore.edit { prefs -> prefs[Keys.TOUCH_MODE] = mode.name }
    }

    suspend fun saveHudPosition(x: Float, y: Float) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HUD_X] = x.coerceIn(0f, 1f)
            prefs[Keys.HUD_Y] = y.coerceIn(0f, 1f)
        }
    }

    suspend fun saveHudHintSessions(count: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.HUD_HINT_SESSIONS] = count }
    }

    suspend fun savePointerSpeed(speed: Float) {
        context.dataStore.edit { prefs -> prefs[Keys.POINTER_SPEED] = PointerAcceleration.clampSpeed(speed) }
    }

    suspend fun savePointerAcceleration(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.POINTER_ACCELERATION] = enabled }
    }

    suspend fun saveTwoFingerRightClick(enabled: Boolean) {
        context.dataStore.edit { prefs -> prefs[Keys.TWO_FINGER_RIGHT_CLICK] = enabled }
    }

    suspend fun saveTouchpadHintSessions(count: Int) {
        context.dataStore.edit { prefs -> prefs[Keys.TOUCHPAD_HINT_SESSIONS] = count }
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
        context.dataStore.edit { prefs -> prefs.putStreamSettings(settings) }
    }

    /** The preset by name and the stats switch; the values only for Custom, the others have fixed ones. */
    private fun MutablePreferences.putStreamSettings(settings: StreamSettings) {
        this[Keys.QUALITY_PRESET] = settings.preset.name
        this[Keys.SHOW_STATS] = settings.showStats
        if (settings.preset == QualityPreset.Custom) {
            this[Keys.CUSTOM_FPS] = settings.maxFps
            this[Keys.CUSTOM_DIMENSION] = settings.maxDimension
            this[Keys.CUSTOM_QUALITY] = settings.jpegQuality
        }
    }

    suspend fun saveHaptics(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAPTICS] = enabled
        }
    }

    /**
     * Game mode on or off in one write, with the stream settings it streams with ([settings]) and
     * those to restore afterwards ([before], null to forget them): a half-written switch would
     * leave the quality it replaced lost.
     */
    suspend fun saveGameMode(enabled: Boolean, settings: StreamSettings, before: StreamSettings?) {
        context.dataStore.edit { prefs ->
            prefs[Keys.GAME_MODE] = enabled
            prefs.putStreamSettings(settings)
            if (before != null) {
                prefs[Keys.SETTINGS_BEFORE_GAME] = encodeStreamSettings(before)
            } else {
                prefs.remove(Keys.SETTINGS_BEFORE_GAME)
            }
        }
    }
}
