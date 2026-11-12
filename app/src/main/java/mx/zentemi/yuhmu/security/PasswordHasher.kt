package mx.zentemi.yuhmu.security

import android.util.Base64
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Hash de contraseñas con PBKDF2-HMAC-SHA256 + sal aleatoria única por cuenta.
 * Formato almacenado: pbkdf2-sha256|iteraciones|sal|hash  (Base64 URL-safe, sin relleno).
 * Las iteraciones viajan en el propio hash para poder subirlas en el futuro sin romper cuentas.
 */
object PasswordHasher {
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"   // disponible desde API 26
    private const val PREFIX = "pbkdf2-sha256"
    private const val ITERATIONS = 120_000
    private const val SALT_BYTES = 16
    private const val KEY_BITS = 256
    private const val FLAGS = Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING
    private val random = SecureRandom()

    fun hash(password: CharArray): String {
        val salt = ByteArray(SALT_BYTES).also { random.nextBytes(it) }
        val key = derive(password, salt, ITERATIONS)
        return listOf(PREFIX, ITERATIONS.toString(), b64(salt), b64(key)).joinToString("|")
    }

    fun verify(password: CharArray, stored: String): Boolean {
        return try {
            val parts = stored.split("|")
            if (parts.size != 4 || parts[0] != PREFIX) return false
            val iterations = parts[1].toInt()
            val salt = Base64.decode(parts[2], FLAGS)
            val expected = Base64.decode(parts[3], FLAGS)
            val actual = derive(password, salt, iterations)
            MessageDigest.isEqual(expected, actual)          // comparación en tiempo constante
        } catch (e: Exception) {
            false
        }
    }

    private fun derive(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        try {
            return SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    private fun b64(bytes: ByteArray): String = Base64.encodeToString(bytes, FLAGS)

    fun sha256Hex(text: String): String =
        MessageDigest.getInstance("SHA-256").digest(text.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
}
