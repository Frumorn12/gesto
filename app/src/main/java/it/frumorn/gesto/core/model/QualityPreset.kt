package it.frumorn.gesto.core.model

/**
 * How the remote screen is streamed, as the Settings offer it.
 *
 * - [Auto] starts at 1080p 60 fps and steps down a rung at a time while the network can't keep
 *   up (see `AutoQualityController`); it never steps back up in the same session.
 * - [Smooth] 60 fps at 720p, for games (Game mode streams with it).
 * - [Sharp] readable text at 30 fps, for work.
 * - [Saver] 720p 30 fps, for a slow network or the battery.
 * - [Custom] the user's own values, set in Advanced.
 */
enum class QualityPreset { Auto, Smooth, Sharp, Saver, Custom }

/** Long side in pixels, frames per second and encoder quality of one preset or rung. */
private data class QualityValues(val maxDimension: Int, val maxFps: Int, val jpegQuality: Int)

/** The rungs of [QualityPreset.Auto], best first. */
private val AutoRungs = listOf(
    QualityValues(1920, 60, 70),
    QualityValues(1600, 60, 62),
    QualityValues(1280, 60, 58),
    QualityValues(1280, 30, 52),
)

/** The lowest rung [QualityPreset.Auto] steps down to. */
val AUTO_LAST_RUNG: Int = AutoRungs.lastIndex

/** What Advanced offers for a custom quality. */
val CustomFpsOptions: List<Int> = listOf(30, 45, 60)
val CustomDimensionOptions: List<Int> = listOf(1280, 1600, 1920, 2560, 3840)
val CustomQualityRange: IntRange = 35..92

/**
 * The settings to stream with: for [QualityPreset.Auto] those of [rung] (0 = best), for
 * [QualityPreset.Custom] the values of [custom] kept to what Advanced offers (Auto's first rung
 * when none were saved), for the others their fixed values.
 */
fun QualityPreset.settings(rung: Int = 0, custom: StreamSettings? = null, showStats: Boolean): StreamSettings {
    val values = when (this) {
        QualityPreset.Auto -> AutoRungs[rung.coerceIn(0, AUTO_LAST_RUNG)]
        QualityPreset.Smooth -> QualityValues(1280, 60, 58)
        QualityPreset.Sharp -> QualityValues(2400, 30, 86)
        QualityPreset.Saver -> QualityValues(1280, 30, 52)
        QualityPreset.Custom -> custom?.let {
            QualityValues(
                maxDimension = nearest(CustomDimensionOptions, it.maxDimension),
                maxFps = nearest(CustomFpsOptions, it.maxFps),
                jpegQuality = it.jpegQuality.coerceIn(CustomQualityRange),
            )
        } ?: AutoRungs.first()
    }
    return StreamSettings(
        preset = this,
        maxFps = values.maxFps,
        jpegQuality = values.jpegQuality,
        maxDimension = values.maxDimension,
        showStats = showStats,
    )
}

/** The offer closest to [value]; the lower one on a tie. */
private fun nearest(offers: List<Int>, value: Int): Int = offers.minBy { kotlin.math.abs(it - value) }

/** What these settings stream with when [QualityPreset.Auto] is on [rung]; other presets are unchanged. */
fun StreamSettings.atRung(rung: Int): StreamSettings =
    if (preset == QualityPreset.Auto) QualityPreset.Auto.settings(rung, showStats = showStats) else this

/**
 * The stream settings kept in the settings store: the preset by name and, for Custom, its values.
 * A missing or unknown name (the old stream profiles included) is [QualityPreset.Auto].
 */
fun storedStreamSettings(
    presetName: String?,
    customFps: Int?,
    customDimension: Int?,
    customQuality: Int?,
    showStats: Boolean,
): StreamSettings {
    val preset = QualityPreset.entries.find { it.name == presetName } ?: QualityPreset.Auto
    val first = AutoRungs.first()
    val custom = StreamSettings(
        preset = QualityPreset.Custom,
        maxFps = customFps ?: first.maxFps,
        jpegQuality = customQuality ?: first.jpegQuality,
        maxDimension = customDimension ?: first.maxDimension,
        showStats = showStats,
    )
    return preset.settings(custom = custom, showStats = showStats)
}

/**
 * The presets the quick menu cycles through, in order. [QualityPreset.Custom] is not one of them:
 * from it the next is Auto.
 */
val QualityCycle: List<QualityPreset> = listOf(
    QualityPreset.Auto,
    QualityPreset.Smooth,
    QualityPreset.Sharp,
    QualityPreset.Saver,
)

/** The preset after [preset] in [QualityCycle]. */
fun nextQuality(preset: QualityPreset): QualityPreset =
    QualityCycle[(QualityCycle.indexOf(preset) + 1) % QualityCycle.size]

/** The "1080" of 1080p: the short side of a 16:9 picture as long as [StreamSettings.maxDimension]. */
val StreamSettings.videoLines: Int
    get() = maxDimension * 9 / 16
