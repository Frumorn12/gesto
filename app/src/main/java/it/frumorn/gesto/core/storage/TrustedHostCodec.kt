package it.frumorn.gesto.core.storage

import it.frumorn.gesto.core.model.TrustedHost
import org.json.JSONArray
import org.json.JSONObject

/**
 * The JSON form of the paired PCs, as stored (encrypted) by [TrustedHostStore]. Fields added after
 * the first release are optional, so a list saved by an older version still loads.
 */
object TrustedHostCodec {
    fun encode(hosts: List<TrustedHost>): String {
        val array = JSONArray()
        hosts.forEach { host ->
            val item = JSONObject()
                .put("id", host.id)
                .put("hostName", host.hostName)
                .put("address", host.address)
                .put("port", host.port)
                .put("fingerprint", host.fingerprint)
                .put("deviceId", host.deviceId)
                .put("sessionToken", host.sessionToken)
                .put("lastConnectedAt", host.lastConnectedAt)
                .put("macAddresses", JSONArray(host.macAddresses))
            host.displayName?.let { item.put("displayName", it) }
            array.put(item)
        }
        return array.toString()
    }

    fun decode(json: String): List<TrustedHost> {
        val array = JSONArray(json)
        return List(array.length()) { index ->
            val item = array.getJSONObject(index)
            TrustedHost(
                id = item.getString("id"),
                hostName = item.getString("hostName"),
                address = item.getString("address"),
                port = item.getInt("port"),
                fingerprint = item.getString("fingerprint"),
                deviceId = item.getString("deviceId"),
                sessionToken = item.getString("sessionToken"),
                lastConnectedAt = item.optLong("lastConnectedAt", 0L),
                // isNull is also true when the key is missing (a list saved before renaming existed).
                displayName = if (item.isNull("displayName")) null else item.getString("displayName").ifBlank { null },
                macAddresses = item.optJSONArray("macAddresses").toStringList(),
            )
        }
    }
}

/**
 * [hosts] with [host] in place of the first entry that has its id or its fingerprint (other such
 * entries are dropped), or with [host] appended when there is none.
 */
internal fun upsertInPlace(hosts: List<TrustedHost>, host: TrustedHost): List<TrustedHost> {
    val same = { other: TrustedHost -> other.id == host.id || other.fingerprint == host.fingerprint }
    val first = hosts.indexOfFirst(same)
    if (first < 0) return hosts + host
    return hosts.filterIndexed { index, other -> index == first || !same(other) }
        .map { if (same(it)) host else it }
}

/** The strings of a JSON array, skipping anything else; empty when there is no array. */
internal fun JSONArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return (0 until length()).mapNotNull { index -> opt(index) as? String }
}
