package it.frumorn.gesto.core.system

import android.content.Context
import android.os.Build
import android.provider.Settings
import java.util.Locale

/** The name the PC shows for this phone (in its notification and among the paired phones). */
object DeviceName {
    /** The daemon accepts names of 1 to 80 characters. */
    const val MAX_LENGTH = 80

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
        return name.ifEmpty { "Android" }.take(MAX_LENGTH)
    }
}
