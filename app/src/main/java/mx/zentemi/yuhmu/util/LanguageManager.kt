package mx.zentemi.yuhmu.util

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

enum class AppLanguage(val code: String, val displayName: String, val flag: String) {
    ES("es", "Español (Predeterminado)", "🇪🇸"),
    EN("en", "English", "🇬🇧");

    companion object {
        fun fromCode(code: String): AppLanguage = entries.firstOrNull { it.code == code } ?: ES
    }
}

object LanguageManager {
    private const val PREF_NAME = "yuhmu_settings"
    private const val KEY_LANG = "app_language"

    fun getLanguage(context: Context): AppLanguage {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val code = prefs.getString(KEY_LANG, AppLanguage.ES.code) ?: AppLanguage.ES.code
        return AppLanguage.fromCode(code)
    }

    fun setLanguage(context: Context, language: AppLanguage) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANG, language.code).apply()
        applyLocale(context, language)
    }

    @Suppress("DEPRECATION")
    fun applyLocale(context: Context, language: AppLanguage) {
        val locale = Locale(language.code)
        Locale.setDefault(locale)
        val config = context.resources.configuration
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.ES }

fun tr(key: String, lang: AppLanguage): String {
    val es = STRINGS[key]?.first ?: key
    val en = STRINGS[key]?.second ?: es
    return if (lang == AppLanguage.EN) en else es
}

@Composable
fun String.tr(): String {
    val lang = LocalAppLanguage.current
    return tr(this, lang)
}

private val STRINGS = mapOf(
    "nav_home" to Pair("Inicio", "Home"),
    "nav_games" to Pair("Juegos", "Games"),
    "nav_trophies" to Pair("Trofeos", "Trophies"),
    "nav_culture" to Pair("Cultura", "Culture"),
    "nav_profile" to Pair("Perfil", "Profile"),

    "app_language" to Pair("Idioma de la aplicación", "App Language"),
    "select_language" to Pair("Seleccionar idioma", "Select Language"),
    "spanish_default" to Pair("Español (Predeterminado)", "Spanish (Default)"),
    "english" to Pair("Inglés", "English"),
    "app_settings" to Pair("Ajustes de la aplicación", "App Settings"),

    "your_achievements" to Pair("Tus logros", "Your Achievements"),
    "current_streak" to Pair("Racha actual", "Current streak"),
    "completed_lessons" to Pair("Lecciones completadas", "Completed lessons"),
    "learned_words" to Pair("Palabras aprendidas", "Learned words"),
    "played_games" to Pair("Juegos jugados", "Games played"),
    "days" to Pair("días", "days"),

    "account_security" to Pair("Seguridad de la cuenta", "Account Security"),
    "change_password" to Pair("Cambiar contraseña", "Change password"),
    "view_recent_activity" to Pair("Ver actividad reciente", "View recent activity"),
    "hide_recent_activity" to Pair("Ocultar actividad reciente", "Hide recent activity"),
    "log_out" to Pair("Cerrar sesión", "Log out"),
    "danger_zone" to Pair("Zona de peligro", "Danger zone"),
    "delete_account" to Pair("Eliminar mi cuenta", "Delete my account"),

    "save" to Pair("Guardar", "Save"),
    "cancel" to Pair("Cancelar", "Cancel"),
    "change_name" to Pair("Cambiar nombre", "Change name"),

    "daily_challenge" to Pair("Reto del día", "Daily Challenge"),
    "start_lesson" to Pair("Iniciar lección", "Start lesson"),
    "review_lesson" to Pair("Repasar lección", "Review lesson"),
    "listen_repeat" to Pair("Escucha y repite", "Listen & repeat"),
    "memory_game" to Pair("Memorama", "Memory Game"),
    "repeat_word" to Pair("Repite la palabra", "Repeat the word"),
    "quiz_game" to Pair("Cuestionario Yuhmu", "Yuhmu Quiz"),
    "score" to Pair("Puntuación", "Score"),
    "time" to Pair("Tiempo", "Time"),
    "win" to Pair("¡Ganaste!", "You Won!"),
    "play_again" to Pair("Jugar de nuevo", "Play again")
)
