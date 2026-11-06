package it.frumorn.gesto.core.model

/** The stream settings Game mode switches to: the Smooth preset (1280 px, 60 fps, quality 58). */
val GameModePreset: StreamSettings = QualityPreset.Smooth.settings(showStats = true)

/**
 * Turning Game mode on or off. Returns the settings to stream with and the settings to remember
 * for when Game mode goes off again.
 *
 * - On: the Smooth preset, remembering [current] (or what was already remembered, if it was on).
 * - Off, still on the Smooth preset: back to [saved], the preset (and its values) from before.
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
    preset == other.preset &&
        maxFps == other.maxFps &&
        jpegQuality == other.jpegQuality &&
        maxDimension == other.maxDimension

private const val FIELD_SEPARATOR = ';'

/** One line for the settings store: `preset;fps;quality;dimension;stats`. */
fun encodeStreamSettings(settings: StreamSettings): String = listOf(
    settings.preset.name,
    settings.maxFps,
    settings.jpegQuality,
    settings.maxDimension,
    settings.showStats,
).joinToString(FIELD_SEPARATOR.toString())

/**
 * The settings [encodeStreamSettings] wrote, or null when [value] is not such a line. A preset
 * name it doesn't know (an old stream profile) reads as Auto, like the stored quality does; the
 * values are those of the preset, except for Custom, which keeps its own.
 */
fun decodeStreamSettings(value: String): StreamSettings? {
    val fields = value.split(FIELD_SEPARATOR)
    if (fields.size != 5) return null
    val fps = fields[1].toIntOrNull() ?: return null
    val quality = fields[2].toIntOrNull() ?: return null
    val dimension = fields[3].toIntOrNull() ?: return null
    val showStats = fields[4].toBooleanStrictOrNull() ?: return null
    return storedStreamSettings(fields[0], fps, dimension, quality, showStats)
}
