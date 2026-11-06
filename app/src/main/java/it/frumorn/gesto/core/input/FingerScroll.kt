package it.frumorn.gesto.core.input

/**
 * The scroll to send for fingers that moved by ([fingerDx], [fingerDy]) on the phone.
 *
 * One convention for the whole app (touchpad, its scroll strip, the remote screen): the PC's
 * content follows the fingers, like scrolling on the phone. On the wire a positive dy scrolls
 * down (the content moves up), which is how the daemon's portal and Hyprland backends read it, so
 * fingers moving up send a positive dy. A backend that reads it the other way round is fixed in
 * the daemon, not compensated here.
 *
 * [damping] scales the distance (the remote screen sends three quarters of it).
 */
fun fingerScroll(fingerDx: Float, fingerDy: Float, damping: Float = 1f): PointerDelta =
    PointerDelta(-fingerDx * damping, -fingerDy * damping)
