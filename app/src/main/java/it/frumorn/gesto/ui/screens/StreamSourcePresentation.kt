package it.frumorn.gesto.ui.screens

import androidx.annotation.StringRes
import it.frumorn.gesto.R
import it.frumorn.gesto.core.model.ScreenSource

/**
 * How capture sources are grouped in Advanced.
 *
 * The daemon offers the same two monitors through three backends that differ by an order of
 * magnitude in speed, and used to present them as one flat list labelled with backend names. That
 * is a trap: picking the wrong row silently drops the stream from 60 fps of hardware H.264 to six
 * frames a second of JPEG, with nothing on screen to explain why. Grouping by speed puts the
 * consequence in front of the choice.
 */
enum class SourceGroup(@StringRes val title: Int, @StringRes val caption: Int) {
    Recommended(R.string.source_group_recommended, R.string.source_group_recommended_text),
    Fallback(R.string.source_group_fallback, R.string.source_group_fallback_text),
}

/** How one capture backend is described to the user. */
private data class BackendDescriptor(
    val group: SourceGroup,
    /** Appended to the monitor name; `null` when the source is already uniquely named. */
    val tag: String?,
    /** What it costs, in words; `null` for a backend the app doesn't know (its name is shown). */
    @StringRes val quality: Int?,
    val fast: Boolean,
)

/** A capture source as Advanced shows it. */
data class SourcePresentation(
    val id: String,
    val group: SourceGroup,
    val title: String,
    /** What the backend costs, in words; `null` when unknown, then [backend] is all there is. */
    @StringRes val quality: Int?,
    val backend: String,
    /** "1920 × 1080" when the daemon knows the size. */
    val resolution: String?,
    val fast: Boolean,
)

/** Human-readable summary of one capture source. */
fun presentSource(source: ScreenSource): SourcePresentation {
    val resolution = if (source.width > 0 && source.height > 0) {
        "${source.width} × ${source.height}"
    } else {
        null
    }
    val descriptor = when (source.backend) {
        "wayland-screencast-portal" ->
            BackendDescriptor(SourceGroup.Recommended, null, R.string.source_quality_portal, true)
        "hyprland-grim" ->
            BackendDescriptor(SourceGroup.Fallback, "Screenshot", R.string.source_quality_grim, false)
        "x11-ffmpeg" ->
            BackendDescriptor(SourceGroup.Fallback, "X11", R.string.source_quality_x11, false)
        else -> BackendDescriptor(SourceGroup.Fallback, source.backend, null, false)
    }
    // The same monitor appears once per fallback backend, so the method has to be part of the
    // name: two rows both reading "eDP-1" are impossible to tell apart at a glance.
    val title = listOfNotNull(sourceTitle(source), descriptor.tag).joinToString(" · ")
    return SourcePresentation(
        id = source.id,
        group = descriptor.group,
        title = title,
        quality = descriptor.quality,
        backend = source.backend,
        resolution = resolution,
        fast = descriptor.fast,
    )
}

/**
 * The source name without the parenthetical the daemon appends.
 *
 * Those parentheses carry the backend's frame rate, which the detail line now states properly, so
 * repeating it in the title just makes every row look the same at a glance.
 */
internal fun sourceTitle(source: ScreenSource): String {
    val label = source.label.ifBlank { source.id }
    val withoutSuffix = label.substringBefore(" (").trim()
    return withoutSuffix.ifBlank { label }
}

/** Sources grouped for display, recommended first, each group keeping the daemon's order. */
fun groupSources(sources: List<ScreenSource>): List<Pair<SourceGroup, List<SourcePresentation>>> {
    val presented = sources.map(::presentSource)
    return SourceGroup.entries
        .map { group -> group to presented.filter { it.group == group } }
        .filter { (_, items) -> items.isNotEmpty() }
}
