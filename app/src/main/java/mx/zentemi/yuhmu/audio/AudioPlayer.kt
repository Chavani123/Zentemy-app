package mx.zentemi.yuhmu.audio

import android.content.Context
import android.media.MediaPlayer
import mx.zentemi.yuhmu.model.Word
import java.text.Normalizer
import java.util.Locale

/** Una grabación incluida en la aplicación y disponible sin conexión. */
data class AudioCatalogItem(
    val path: String,
    val title: String,
    val section: String,
    val isAlternative: Boolean,
    val note: String? = null
)

/** Reproduce audios desde assets/Audio y relaciona solo coincidencias exactas. */
class AudioPlayer(private val context: Context) {

    private val audioMap = mutableMapOf<String, String>()
    private var player: MediaPlayer? = null

    // Los números se relacionan por su significado en español, nunca por el sufijo del ID.
    // Así, numeros_020 no puede reproducir accidentalmente el archivo 20.m4a si ese audio
    // no existe o si el orden del catálogo cambia.
    private val numberFilesBySpanish = mapOf(
        "uno" to "1.m4a", "dos" to "2.m4a", "tres" to "3.m4a",
        "cuatro" to "4.m4a", "cinco" to "5.m4a", "seis" to "6.m4a",
        "siete" to "7.m4a", "ocho" to "8.m4a", "nueve" to "9.m4a",
        "diez" to "10.m4a"
    )

    private val categoryFolders = mapOf(
        "numeros" to "Audio/numeros",
        "cuerpo" to "Audio/Partes del cuerpo",
        "familia" to "Audio/Famila",
        "plantas" to "Audio/frutas , verduras,Plantas",
        "alimentos" to "Audio/comida",
        "animales" to "Audio/Animales",
        "objetos" to "Audio/Objetos",
        "colores" to "Audio/colores",
        "adjetivos" to "Audio/adjetivos",
        "verbos" to "Audio/verbos",
        "ropa" to "Audio/ropa",
        "clima" to "Audio/Elementos ,astros,clima"
    )

    /**
     * Registra únicamente relaciones inequívocas. Las traducciones repetidas que tienen
     * más de una forma Yhụmụ se dejan fuera para no reproducir una voz posiblemente distinta.
     */
    fun registerWordMappings(words: List<Word>) {
        audioMap.clear()
        val translations = words.groupBy { it.categoryId to normalize(it.spanish) }

        words.forEach { word ->
            val sameTranslation = translations[word.categoryId to normalize(word.spanish)].orEmpty()
            val hasDifferentForms = sameTranslation.map { normalize(it.yuhmu) }.distinct().size > 1
            if (hasDifferentForms) return@forEach

            val path = findWordAudio(word) ?: return@forEach
            audioMap[word.id] = path
        }
    }

    fun hasAudio(id: String): Boolean = getAssetPath(id) != null

    /** Resuelve un ID registrado o una ruta directa del catálogo. */
    fun getAssetPath(id: String): String? {
        val clean = id.trim().replace('\\', '/')
        if (clean.startsWith("Audio/") && clean.endsWith(".m4a", ignoreCase = true)) {
            return clean.takeIf { assetExists(it) }
        }
        return audioMap[clean]
    }

    /** Devuelve todas las grabaciones de Audio/, también las frases complementarias. */
    fun getCatalog(): List<AudioCatalogItem> {
        val entries = context.assets.list("Audio").orEmpty()
        val rootAudio = entries.asSequence()
            .filter { it.endsWith(".m4a", ignoreCase = true) }
            .map { filename ->
                val rawTitle = filename.substringBeforeLast('.')
                val isAlternative = COPY_SUFFIX_REGEX.containsMatchIn(rawTitle)
                val title = sanitizeTitle(rawTitle.replace(COPY_SUFFIX_REGEX, "").trim())
                AudioCatalogItem(
                    path = "Audio/$filename",
                    title = title,
                    section = "Audios de vocabulario",
                    isAlternative = isAlternative
                )
            }
        val nestedAudio = entries.asSequence()
            .filter { it != "README.txt" && !it.endsWith(".m4a", ignoreCase = true) }
            .flatMap { folder ->
                val directory = "Audio/$folder"
                context.assets.list(directory).orEmpty().asSequence()
                    .filter { it.endsWith(".m4a", ignoreCase = true) }
                    .map { filename ->
                        val rawTitle = filename.substringBeforeLast('.')
                        val isAlternative = COPY_SUFFIX_REGEX.containsMatchIn(rawTitle)
                        val title = sanitizeTitle(rawTitle.replace(COPY_SUFFIX_REGEX, "").trim())
                        AudioCatalogItem(
                            path = "$directory/$filename",
                            title = title,
                            section = folderTitle(folder),
                            isAlternative = isAlternative,
                            note = if (folder == "numeros" && rawTitle == "100") {
                                "Grabación de «cien» sin variante confirmada en el material."
                            } else null
                        )
                    }
            }
        return (rootAudio + nestedAudio)
            .sortedWith(compareBy<AudioCatalogItem> { it.section.lowercase(Locale.ROOT) }
                .thenBy { it.title.lowercase(Locale.ROOT) }
                .thenBy { it.isAlternative })
            .toList()
    }

    private fun sanitizeTitle(raw: String): String {
        val cleaned = raw.replace('_', ' ')
            .replace(SPACES_AROUND_COMMA_REGEX, ", ")
            .replace(MULTIPLE_SPACES_REGEX, " ")
            .trim()
        return if (cleaned.isNotEmpty()) {
            cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        } else {
            cleaned
        }
    }

    private fun folderTitle(folder: String): String = when (folder) {
        "Animales" -> "Animales"
        "Elementos ,astros,clima" -> "Elementos, astros y clima"
        "Famila" -> "Familia"
        "Honorificos-profesiones" -> "Honoríficos y profesiones"
        "Objetos" -> "Objetos"
        "Partes del cuerpo" -> "Partes del cuerpo"
        "Sustantivos" -> "Sustantivos"
        "adjetivos" -> "Adjetivos"
        "colores" -> "Colores"
        "comida" -> "Comida"
        "frases de la vida cotidiana" -> "Frases de la vida cotidiana"
        "frutas , verduras,Plantas" -> "Frutas, verduras y plantas"
        "numeros" -> "Números"
        "ropa" -> "Ropa"
        "verbos" -> "Verbos"
        else -> folder.replaceFirstChar { it.uppercase() }
    }

    private fun findWordAudio(word: Word): String? {
        val folder = categoryFolders[word.categoryId] ?: return null

        if (word.categoryId == "numeros") {
            val filename = numberFilesBySpanish[normalize(word.spanish)] ?: return null
            val path = "$folder/$filename"
            return path.takeIf { assetExists(it) }
        }

        val wanted = normalize(word.spanish)
        if (wanted.isEmpty()) return null
        val files = context.assets.list(folder).orEmpty()
            .filter { it.endsWith(".m4a", ignoreCase = true) }
            .map { name ->
                val stem = name.substringBeforeLast('.')
                val withoutCopyNumber = stem.replace(COPY_SUFFIX_REGEX, "").trim()
                Triple(name, normalize(withoutCopyNumber), stem.matches(COPY_SUFFIX_MATCH_REGEX))
            }

        // Coincidencia exacta por nombre de significado y carpeta de categoría.
        // Si hay dos grabaciones con el mismo nombre (por ejemplo, una versión "(1)"),
        // no elegimos ninguna automáticamente: el contenido puede ser diferente.
        val exact = files.filter { it.second == wanted }
        if (exact.size == 1) return "$folder/${exact.first().first}"
        if (exact.size > 1) return null

        // Solo se mantienen equivalencias ortográficas documentadas en los materiales.
        val explicitAlias = when (word.categoryId to wanted) {
            "adjetivos" to normalize("Alto (estatura)") -> "Alto"
            "ropa" to normalize("Ceñidor") -> "Señidor"
            else -> null
        }
        if (explicitAlias != null) {
            val aliasMatches = files.filter { it.second == normalize(explicitAlias) }
            if (aliasMatches.size == 1) return "$folder/${aliasMatches.first().first}"
            if (aliasMatches.size > 1) return null
        }

        // Grabaciones que se guardaron en carpetas transversales del material original.
        // Solo se acepta igualdad exacta del nombre, sin aproximaciones parciales.
        val extraFolders = when (word.categoryId) {
            "familia" -> listOf("Audio/Honorificos-profesiones")
            "plantas" -> listOf("Audio/Sustantivos")
            else -> emptyList()
        }
        for (extraFolder in extraFolders) {
            val exactExtra = context.assets.list(extraFolder).orEmpty()
                .filter { it.endsWith(".m4a", ignoreCase = true) }
                .filter { filename ->
                    val stem = filename.substringBeforeLast('.').replace(COPY_SUFFIX_REGEX, "").trim()
                    normalize(stem) == wanted || (word.categoryId == "plantas" && wanted == normalize("Campo") && normalize(stem) == normalize("el campo"))
                }
            if (exactExtra.size == 1) return "$extraFolder/${exactExtra.first()}"
        }
        return null
    }

    private fun assetExists(path: String): Boolean = try {
        context.assets.open(path).use { }
        true
    } catch (_: Exception) {
        false
    }

    fun play(idOrPath: String): Boolean {
        val path = getAssetPath(idOrPath) ?: return false
        release()
        val next = MediaPlayer()
        return try {
            context.assets.openFd(path).use { fd ->
                next.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            next.setOnCompletionListener { completed ->
                completed.release()
                if (player === completed) player = null
            }
            next.setOnErrorListener { failed, _, _ ->
                failed.release()
                if (player === failed) player = null
                true
            }
            next.prepare()
            player = next
            next.start()
            true
        } catch (_: Exception) {
            try { next.release() } catch (_: Exception) { }
            if (player === next) player = null
            false
        }
    }

    fun release() {
        val current = player
        player = null
        try { current?.release() } catch (_: Exception) { }
    }

    companion object {
        private val COPY_SUFFIX_REGEX = Regex("\\s*\\(\\d+\\)$")
        private val COPY_SUFFIX_MATCH_REGEX = Regex(".*\\(\\d+\\)$")
        private val SPACES_AROUND_COMMA_REGEX = Regex("\\s*,\\s*")
        private val MULTIPLE_SPACES_REGEX = Regex("\\s+")
        private val DIACRITICAL_MARKS_REGEX = Regex("\\p{InCombiningDiacriticalMarks}+")
        private val NON_ALPHANUMERIC_REGEX = Regex("[^a-z0-9]")

        fun normalize(text: String): String {
            val temp = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            return temp.replace(DIACRITICAL_MARKS_REGEX, "")
                .replace(NON_ALPHANUMERIC_REGEX, "")
        }
    }
}
