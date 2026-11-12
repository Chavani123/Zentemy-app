package mx.zentemi.yuhmu.security

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Guarda el token de sesión cifrado con AES-256-GCM. La llave vive en Android Keystore
 * (respaldada por hardware cuando el dispositivo lo permite) y nunca sale de él.
 */
class SessionVault(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences("yuhmu_session", Context.MODE_PRIVATE)

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(ALIAS, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        )
        return generator.generateKey()
    }

    fun save(userId: Long, token: String) {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val encrypted = cipher.doFinal("$userId:$token".toByteArray(Charsets.UTF_8))
        prefs.edit().putString(KEY_BLOB, Base64.encodeToString(cipher.iv + encrypted, Base64.NO_WRAP)).apply()
    }

    /** Devuelve (userId, token) o null si no hay sesión o el contenido fue alterado. */
    fun load(): Pair<Long, String>? {
        val raw = prefs.getString(KEY_BLOB, null) ?: return null
        return try {
            val bytes = Base64.decode(raw, Base64.NO_WRAP)
            val iv = bytes.copyOfRange(0, IV_BYTES)
            val data = bytes.copyOfRange(IV_BYTES, bytes.size)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
            val text = String(cipher.doFinal(data), Charsets.UTF_8)
            val idx = text.indexOf(':')
            text.substring(0, idx).toLong() to text.substring(idx + 1)
        } catch (e: Exception) {
            clear()      // blob corrupto o llave invalidada: se descarta la sesión
            null
        }
    }

    fun clear() {
        prefs.edit().remove(KEY_BLOB).apply()
    }

    private companion object {
        const val ALIAS = "yuhmu_session_key"
        const val TRANSFORMATION = "AES/GCM/NoPadding"
        const val KEY_BLOB = "blob"
        const val IV_BYTES = 12
    }
}
