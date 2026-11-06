package it.frumorn.gesto.core.pairing

import java.util.Locale

/** The 4-digit PIN the phone shows while the PC asks whether to let it in. */
object PinGenerator {
    /** A PIN from 0000 to 9999, always four ASCII digits (whatever the phone's language). */
    fun newPin(random: java.util.Random = java.security.SecureRandom()): String =
        String.format(Locale.ROOT, "%04d", random.nextInt(10_000))
}
