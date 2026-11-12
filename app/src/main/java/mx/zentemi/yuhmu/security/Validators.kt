package mx.zentemi.yuhmu.security

/** Validación y saneamiento de entradas (lista blanca de caracteres, longitudes acotadas). */
object Validators {
    private val USERNAME = Regex("[A-Za-z0-9_]{3,20}")
    private val DISPLAY_NAME = Regex("[\\p{L}\\p{N} ]{2,20}")
    private val COMMON = setOf(
        "password1", "contrasena1", "contraseña1", "abc12345", "qwerty123", "yuhmu123",
        "12345678a", "a1234567", "admin123", "iloveyou1", "123456789a", "password123"
    )

    const val MIN_PASSWORD = 8
    const val MAX_PASSWORD = 64

    fun username(v: String): String? =
        if (USERNAME.matches(v)) null
        else "El usuario debe tener de 3 a 20 caracteres: letras, números o guion bajo."

    fun displayName(v: String): String? =
        if (DISPLAY_NAME.matches(v)) null
        else "El nombre debe tener de 2 a 20 caracteres (letras, números y espacios)."

    fun password(v: String, username: String = ""): String? = when {
        v.length < MIN_PASSWORD -> "La contraseña debe tener al menos $MIN_PASSWORD caracteres."
        v.length > MAX_PASSWORD -> "La contraseña es demasiado larga (máximo $MAX_PASSWORD)."
        v.none { it.isLetter() } || v.none { it.isDigit() } -> "Usa letras y números en tu contraseña."
        username.length >= 3 && v.contains(username, ignoreCase = true) ->
            "Tu contraseña no debe contener tu nombre de usuario."
        v.lowercase() in COMMON -> "Esa contraseña es muy común. Elige otra."
        else -> null
    }

    fun hasMinLength(v: String) = v.length >= MIN_PASSWORD
    fun hasLetter(v: String) = v.any { it.isLetter() }
    fun hasDigit(v: String) = v.any { it.isDigit() }

    /** Fortaleza 0..4 para el indicador visual. */
    fun strength(v: String): Int {
        if (v.isEmpty() || v.lowercase() in COMMON) return 0
        var score = 0
        if (v.length >= MIN_PASSWORD) score++
        if (v.length >= 11) score++
        if (hasLetter(v) && hasDigit(v)) score++
        if ((v.any { it.isUpperCase() } && v.any { it.isLowerCase() }) || v.any { !it.isLetterOrDigit() }) score++
        return score
    }
}
