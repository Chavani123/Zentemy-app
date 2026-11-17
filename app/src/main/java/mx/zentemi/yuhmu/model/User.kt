package mx.zentemi.yuhmu.model

import mx.zentemi.yuhmu.R

/** Personajes / Avatares oficiales de Yhụmụ Zentemi. */
enum class AppCharacter(
    val id: String,
    val characterName: String,
    val title: String,
    val drawableRes: Int
) {
    RABBIT("rabbit", "Conejo Sabio", "Sabio de la Sierra", R.drawable.avatar_rabbit),
    JAR("jar", "Jarrito Yhụmụ", "Jarrito Tradicional", R.drawable.avatar_jar),
    COLIBRI("colibri", "Colibrí Mensajero", "Colibrí Bordado", R.drawable.avatar_colibri),
    CORN("corn", "Maíz Zentemi", "Maíz Morado", R.drawable.avatar_corn);

    companion object {
        fun fromId(id: String?): AppCharacter =
            entries.firstOrNull { it.id.equals(id, ignoreCase = true) || it.characterName.equals(id, ignoreCase = true) } ?: RABBIT
    }
}

/** Avatares disponibles (se valida contra esta lista). */
val AVATARS = AppCharacter.entries.map { it.id }

/** Cuenta local. Nunca contiene hashes ni secretos. */
data class User(
    val id: Long,
    val username: String,
    val displayName: String,
    val avatar: String,
    val createdAt: Long
)

data class SecurityEvent(val type: String, val createdAt: Long, val detail: String?)
