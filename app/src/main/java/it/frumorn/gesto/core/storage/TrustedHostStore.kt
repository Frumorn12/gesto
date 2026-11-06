package it.frumorn.gesto.core.storage

import android.content.Context
import android.content.SharedPreferences
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import it.frumorn.gesto.core.model.TrustedHost
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class TrustedHostStore(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("waypad_secure", Context.MODE_PRIVATE)
    private val box = AndroidSecretBox()

    fun load(): List<TrustedHost> {
        val encrypted = prefs.getString("trusted_hosts", null) ?: return emptyList()
        val raw = runCatching { box.decrypt(encrypted) }.getOrElse { return emptyList() }
        return TrustedHostCodec.decode(raw)
    }

    fun save(hosts: List<TrustedHost>) {
        prefs.edit().putString("trusted_hosts", box.encrypt(TrustedHostCodec.encode(hosts))).apply()
    }

    /** Applies [change] to the stored PC with [id]; returns the updated PC, null when there is none. */
    fun update(id: String, change: (TrustedHost) -> TrustedHost): TrustedHost? {
        val hosts = load()
        val current = hosts.find { it.id == id } ?: return null
        val updated = change(current)
        save(hosts.map { if (it.id == id) updated else it })
        return updated
    }

    /**
     * Stores [host], replacing the entry with the same id or the same PC identity in its place (the
     * PCs list keeps its order); a new PC goes last.
     */
    fun upsert(host: TrustedHost) {
        save(upsertInPlace(load(), host))
    }

    fun remove(id: String) {
        save(load().filterNot { it.id == id })
    }
}

private class AndroidSecretBox {
    private val alias = "waypad_trusted_hosts_v1"

    fun encrypt(plaintext: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return b64(cipher.iv + encrypted)
    }

    fun decrypt(encoded: String): String {
        val raw = android.util.Base64.decode(encoded, android.util.Base64.NO_WRAP)
        val iv = raw.copyOfRange(0, 12)
        val ciphertext = raw.copyOfRange(12, raw.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        return String(cipher.doFinal(ciphertext), Charsets.UTF_8)
    }

    private fun getOrCreateKey(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun b64(bytes: ByteArray): String =
        android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
}
