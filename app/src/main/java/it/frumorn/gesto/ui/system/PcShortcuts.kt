package it.frumorn.gesto.ui.system

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import it.frumorn.gesto.EXTRA_CONNECT_HOST
import it.frumorn.gesto.R
import it.frumorn.gesto.core.model.TrustedHost
import it.frumorn.gesto.core.model.label

/**
 * Launcher shortcuts, one per paired PC (the most recently used first, at most 4): a long press on
 * the app icon opens Gesto straight on that PC through `gesto://connect?host=<id>`.
 */
object PcShortcuts {
    private const val TAG = "PcShortcuts"
    private const val MAX_SHORTCUTS = 4
    private const val ID_PREFIX = "pc:"

    /** The link a shortcut opens. */
    fun connectUri(hostId: String): Uri =
        Uri.Builder().scheme("gesto").authority("connect").appendQueryParameter("host", hostId).build()

    fun update(context: Context, hosts: List<TrustedHost>) {
        val shortcuts = runCatching {
            val limit = minOf(MAX_SHORTCUTS, ShortcutManagerCompat.getMaxShortcutCountPerActivity(context))
            hosts.sortedByDescending { it.lastConnectedAt }
                .take(limit)
                .mapIndexed { rank, host -> shortcut(context, host, rank) }
        }.getOrElse {
            Log.w(TAG, "shortcuts_build_failed", it)
            return
        }
        // A PC paired again after being forgotten gets its pinned shortcut back.
        runCatching { ShortcutManagerCompat.enableShortcuts(context, shortcuts) }
            .onFailure { Log.w(TAG, "shortcuts_enable_failed", it) }
        runCatching { ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts) }
            .onFailure { Log.w(TAG, "shortcuts_publish_failed", it) }
        // A PC that was forgotten must not stay reachable from a pinned shortcut.
        runCatching {
            val current = hosts.mapTo(HashSet()) { ID_PREFIX + it.id }
            val stale = ShortcutManagerCompat.getShortcuts(context, ShortcutManagerCompat.FLAG_MATCH_PINNED)
                .map { it.id }
                .filter { it.startsWith(ID_PREFIX) && it !in current }
            if (stale.isNotEmpty()) ShortcutManagerCompat.disableShortcuts(context, stale, null)
        }.onFailure { Log.w(TAG, "shortcuts_disable_failed", it) }
    }

    private fun shortcut(context: Context, host: TrustedHost, rank: Int): ShortcutInfoCompat {
        val intent = Intent(Intent.ACTION_VIEW, connectUri(host.id))
            .setPackage(context.packageName)
            // Read by the ViewModel on a cold start, so it opens this PC rather than the last one.
            .putExtra(EXTRA_CONNECT_HOST, host.id)
        return ShortcutInfoCompat.Builder(context, ID_PREFIX + host.id)
            .setShortLabel(host.label)
            .setLongLabel(context.getString(R.string.shortcut_connect_long, host.label))
            .setIcon(IconCompat.createWithResource(context, R.mipmap.ic_launcher))
            .setIntent(intent)
            .setRank(rank)
            .build()
    }
}

/** Keeps the launcher shortcuts in step with the paired PCs. */
@Composable
fun PcShortcutsEffect(hosts: List<TrustedHost>) {
    val context = LocalContext.current.applicationContext
    LaunchedEffect(hosts) { PcShortcuts.update(context, hosts) }
}
