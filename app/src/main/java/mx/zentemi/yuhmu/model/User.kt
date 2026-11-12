package mx.zentemi.yuhmu.model

/** Avatares disponibles (se valida contra esta lista para no guardar texto arbitrario). */
val AVATARS = listOf("🌽", "🦋", "🌵", "🦅", "🐢", "🌺", "🦎", "🐝")

/** Cuenta local. Nunca contiene hashes ni secretos. */
data class User(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatar: String,
    val createdAt: Long
)

data class SecurityEvent(val type: String, val createdAt: Long, val detail: String?)
