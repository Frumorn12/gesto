package it.frumorn.gesto.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import it.frumorn.gesto.core.video.RemoteVideoRenderer
import it.frumorn.gesto.core.video.GestoVideoView

/**
 * Video output of the remote desktop.
 *
 * Hands the decoder a [android.view.Surface] and stays out of the way: frames are rendered by
 * MediaCodec straight into the surface, so no bitmap is ever allocated on the UI side and no
 * recomposition happens per frame. [GestoVideoView] measures itself to the source aspect ratio,
 * and the parent box centres unaligned children, so no sizing modifier is needed.
 *
 * The letterboxing it performs is the same fit-centre used by `core.video.VideoLayout.fitCentre`,
 * which is what [remoteDisplayGestures] assumes when it maps touches onto desktop coordinates.
 * The view never consumes pointer input; gestures belong to the parent box.
 */
@Composable
fun RemoteDisplayVideoSurface(
    renderer: RemoteVideoRenderer?,
    modifier: Modifier = Modifier,
) {
    if (renderer == null) {
        Box(modifier)
        return
    }
    AndroidView(
        factory = { GestoVideoView(it) },
        // Attaching belongs in `update`, not `factory`: the view is reused across recompositions
        // and would otherwise keep pointing at a renderer from a previous session.
        update = { view -> view.attachRenderer(renderer) },
        onRelease = { view -> view.detachRenderer() },
        modifier = modifier.fillMaxSize(),
    )
}
