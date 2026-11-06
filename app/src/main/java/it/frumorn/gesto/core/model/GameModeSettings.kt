package it.frumorn.gesto.core.model

/**
 * The stream settings Game mode switches to: the "Smooth" quality (1280 px, 60 fps, quality 58).
 *
 * Until quality presets replace [StreamProfile], Smooth is the [StreamProfile.LowLatency] profile,
 * which has exactly those values.
 */
val GameModePreset: StreamSettings = StreamProfile.LowLatency.toStreamSettings(showStats = true)

/**
 * Turning Game mode on or off. Returns the settings to stream with and the settings to remember
 * for when Game mode goes off again.
 *
 * - On: the Smooth preset, remembering [current] (or what was already remembered, if it was on).
 * - Off, still on the Smooth preset: back to [saved], the quality from before.
 * - Off after another quality was chosen meanwhile, or with nothing remembered: that quality stays.
 *
 * The "show stats" switch is not part of the quality and always keeps its current value.
 */
fun gameModeChange(
    enable: Boolean,
    current: StreamSettings,
    saved: StreamSettings?,
): Pair<StreamSettings, StreamSettings?> = when {
    enable -> GameModePreset.copy(showStats = current.showStats) to (saved ?: current)
    saved != null && current.sameQualityAs(GameModePreset) -> saved.copy(showStats = current.showStats) to null
    else -> current to null
}

private fun StreamSettings.sameQualityAs(other: StreamSettings): Boolean =
    profile == other.profile &&
        maxFps == other.maxFps &&
        jpegQuality == other.jpegQuality &&
        maxDimension == other.maxDimension

/**
 * The qualities the quick menu cycles through, in order: Automatic, Smooth, Sharp.
 * [StreamProfile.Game] is not one of them: it is what Game mode used to stream with.
 */
val QualityCycle: List<StreamProfile> = listOf(StreamProfile.Balanced, StreamProfile.LowLatency, StreamProfile.Quality)

/** The quality [profile] is shown as; the old Game profile is close enough to Smooth to be called that. */
fun shownQuality(profile: StreamProfile): StreamProfile =
    if (profile == StreamProfile.Game) StreamProfile.LowLatency else profile

/** The quality after [profile] in [QualityCycle]. */
fun nextQuality(profile: StreamProfile): StreamProfile {
    val index = QualityCycle.indexOf(shownQuality(profile))
    return QualityCycle[(index + 1) % QualityCycle.size]
}

private const val FIELD_SEPARATOR = ';'

/** One line for the settings store: `profile;fps;quality;dimension;stats`. */
fun encodeStreamSettings(settings: StreamSettings): String = listOf(
    settings.profile.name,
    settings.maxFps,
    settings.jpegQuality,
    settings.maxDimension,
    settings.showStats,
).joinToString(FIELD_SEPARATOR.toString())

/** The settings [encodeStreamSettings] wrote, or null when [value] is not such a line. */
fun decodeStreamSettings(value: String): StreamSettings? {
    val fields = value.split(FIELD_SEPARATOR)
    if (fields.size != 5) return null
    val profile = StreamProfile.entries.find { it.name == fields[0] } ?: return null
    return StreamSettings(
        profile = profile,
        maxFps = fields[1].toIntOrNull() ?: return null,
        jpegQuality = fields[2].toIntOrNull() ?: return null,
        maxDimension = fields[3].toIntOrNull() ?: return null,
        showStats = fields[4].toBooleanStrictOrNull() ?: return null,
    )
}
