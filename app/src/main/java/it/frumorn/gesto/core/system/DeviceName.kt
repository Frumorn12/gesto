package it.frumorn.gesto.core.system

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.Locale

/** The name the PC shows for this phone (in its notification and among the paired phones). */
object DeviceName {
    /** The daemon accepts names of 1 to 80 bytes, counted in UTF-8. */
    const val MAX_BYTES = 80

    /** The phone's name from its settings ("Nothing Phone (3a)"), else maker and model. */
    fun get(context: Context): String {
        val named = runCatching {
            Settings.Global.getString(context.contentResolver, Settings.Global.DEVICE_NAME)
        }.getOrNull()
        return choose(named, Build.MANUFACTURER, Build.MODEL)
    }

    internal fun choose(deviceName: String?, manufacturer: String?, model: String?): String {
        val name = deviceName?.trim().orEmpty().ifEmpty {
            val maker = manufacturer?.trim().orEmpty().replaceFirstChar { it.titlecase(Locale.ROOT) }
            val device = model?.trim().orEmpty()
            when {
                maker.isEmpty() -> device
                device.startsWith(maker, ignoreCase = true) -> device
                else -> "$maker $device".trim()
            }
        }
        return fitUtf8(name.ifEmpty { "Android" }, MAX_BYTES).trimEnd()
    }

    /** The longest start of [text] that is at most [maxBytes] in UTF-8, never cutting a character in two. */
    private fun fitUtf8(text: String, maxBytes: Int): String {
        var bytes = 0
        var end = 0
        while (end < text.length) {
            val codePoint = text.codePointAt(end)
            val size = when {
                codePoint < 0x80 -> 1
                codePoint < 0x800 -> 2
                codePoint < 0x10000 -> 3
                else -> 4
            }
            if (bytes + size > maxBytes) break
            bytes += size
            end += Character.charCount(codePoint)
        }
        return text.substring(0, end)
    }
}
