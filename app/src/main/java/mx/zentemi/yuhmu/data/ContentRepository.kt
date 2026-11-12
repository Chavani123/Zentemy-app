package mx.zentemi.yuhmu.data

import android.content.Context
import kotlinx.serialization.json.Json
import mx.zentemi.yuhmu.audio.AudioPlayer
import mx.zentemi.yuhmu.model.*

/** Carga vocabulary.json desde assets, arma lecciones y genera preguntas. */
class ContentRepository(context: Context, private val audio: AudioPlayer) {

    // Se declara primero: lo usa buildLessons durante la inicialización.
    private val wordsPerLesson = 4

    val categories: List<WordCategory> = loadCategories(context)
    val lessons: List<Lesson> = buildLessons(categories)
    val allWords: List<Word> = categories.flatMap { it.words }
    val wordById: Map<String, Word> = allWords.associateBy { it.id }

    init {
        audio.registerWordMappings(allWords)
    }

    /** Palabras de la misma categoría con audio: sirven de "rivales" al juzgar la pronunciación. */
    fun poolFor(w: Word, n: Int = 7): List<String> =
        allWords.filter { it.categoryId == w.categoryId && it.id != w.id && audio.hasAudio(it.id) }
            .shuffled().take(n).map { it.id }

    private fun loadCategories(context: Context): List<WordCategory> {
        val text = context.assets.open("vocabulary.json").bufferedReader().use { it.readText() }
        return Json { ignoreUnknownKeys = true }.decodeFromString<VocabularyFile>(text).categories
    }

    private fun buildLessons(cats: List<WordCategory>): List<Lesson> = cats.flatMap { c ->
        val chunks = c.words.chunked(wordsPerLesson).toMutableList()
        // Evita una última lección demasiado corta (igual que en iOS)
        if (chunks.size > 1 && chunks.last().size < 3) {
            val last = chunks.removeAt(chunks.lastIndex)
            chunks[chunks.lastIndex] = chunks.last() + last
        }
        chunks.mapIndexed { i, ws ->
            Lesson("${c.id}_L${i + 1}", c.id, c.title, i + 1, ws)
        }
    }

    fun lessonsIn(category: WordCategory): List<Lesson> =
        lessons.filter { it.categoryId == category.id }

    /**
     * Preguntas para el recorrido principal de lecciones.
     * Aquí no aparecen ejercicios de audio: esa práctica vive en Juegos.
     */
    fun makeLessonQuestions(words: List<Word>, count: Int): List<Question> {
        if (words.isEmpty() || count <= 0) return emptyList()
        return words.shuffled().take(count).map { word ->
            val kind = if (canAskSpanishToYuhmu(word) && kotlin.random.Random.nextBoolean()) {
                QuestionKind.SPANISH_TO_YUHMU
            } else {
                QuestionKind.YUHMU_TO_SPANISH
            }
            question(word, kind, words)
        }
    }

    /** Preguntas aleatorias para retos y juegos; LISTEN solo aparece si existe audio. */
    fun makeQuestions(words: List<Word>, count: Int): List<Question> {
        if (words.isEmpty() || count <= 0) return emptyList()
        return words.shuffled().take(count).map { w ->
            val kinds = mutableListOf(QuestionKind.YUHMU_TO_SPANISH)
            if (canAskSpanishToYuhmu(w)) kinds.add(QuestionKind.SPANISH_TO_YUHMU)
            if (audio.hasAudio(w.id)) kinds.add(QuestionKind.LISTEN)
            question(w, kinds.random(), words)
        }
    }

    /**
     * Crea una ruta de cinco niveles para Retos de vocabulario.
     * Cada partida mezcla palabras, opciones y modos de pregunta. Si una categoría tiene pocas
     * palabras, se vuelven a barajar antes de repetirlas para conservar una ronda completa.
     */
    fun makeLevelledQuestions(
        words: List<Word>,
        levelCount: Int = 5,
        questionsPerLevel: Int = 3
    ): List<Question> {
        if (words.isEmpty() || levelCount <= 0 || questionsPerLevel <= 0) return emptyList()

        val picked = mutableListOf<Word>()
        val usedIds = mutableSetOf<String>()
        repeat(levelCount) { level ->
            // En el nivel de escucha se priorizan palabras con grabación confirmada.
            val audioWords = if (level == 2) words.filter { audio.hasAudio(it.id) } else emptyList()
            val source = audioWords.ifEmpty { words }
            val chosen = source.filterNot { it.id in usedIds }
                .shuffled().take(questionsPerLevel).toMutableList()
            usedIds.addAll(chosen.map { it.id })

            // Se completan las preguntas con palabras aún no usadas antes de repetir alguna.
            if (chosen.size < questionsPerLevel) {
                val extra = words.filterNot { it.id in usedIds }
                    .shuffled().take(questionsPerLevel - chosen.size)
                chosen.addAll(extra)
                usedIds.addAll(extra.map { it.id })
            }
            if (chosen.size < questionsPerLevel) {
                // Solo se reinicia la bolsa cuando ya se utilizaron todas las palabras del grupo.
                usedIds.clear()
                val extra = words.shuffled().take(questionsPerLevel - chosen.size)
                chosen.addAll(extra)
                usedIds.addAll(chosen.map { it.id })
            }
            picked.addAll(chosen)
        }

        return picked.mapIndexed { index, word ->
            val level = index / questionsPerLevel
            val kind = when (level) {
                0 -> QuestionKind.YUHMU_TO_SPANISH
                1 -> if (canAskSpanishToYuhmu(word)) QuestionKind.SPANISH_TO_YUHMU else QuestionKind.YUHMU_TO_SPANISH
                2 -> if (audio.hasAudio(word.id)) QuestionKind.LISTEN else QuestionKind.YUHMU_TO_SPANISH
                3 -> mixedKind(word)
                else -> mixedKind(word)
            }
            question(word, kind, words)
        }
    }

    private fun mixedKind(word: Word): QuestionKind {
        val kinds = mutableListOf(QuestionKind.YUHMU_TO_SPANISH)
        if (canAskSpanishToYuhmu(word)) kinds.add(QuestionKind.SPANISH_TO_YUHMU)
        if (audio.hasAudio(word.id)) kinds.add(QuestionKind.LISTEN)
        return kinds.random()
    }

    /** Evita preguntar una traducción inversa cuando el Excel documenta varias formas Yuhmu. */
    private fun canAskSpanishToYuhmu(word: Word): Boolean = allWords
        .filter { AudioPlayer.normalize(it.spanish) == AudioPlayer.normalize(word.spanish) }
        .map { AudioPlayer.normalize(it.yuhmu) }
        .distinct()
        .size <= 1

    /** Reto del día: selección determinista de palabras por día. */
    fun dailyQuestions(day: Int, count: Int = 5): List<Question> {
        if (allWords.isEmpty() || count <= 0) return emptyList()
        val picked = mutableListOf<Word>()
        var idx = Math.floorMod(day * 31, allWords.size)
        val target = minOf(count, allWords.size)
        while (picked.size < target) {
            val w = allWords[idx]
            if (w !in picked) picked.add(w)
            idx = (idx + 17) % allWords.size
        }
        return makeQuestions(picked, count)
    }

    private fun question(w: Word, kind: QuestionKind, pool: List<Word>): Question {
        val pick: (Word) -> String = { if (kind == QuestionKind.SPANISH_TO_YUHMU) it.yuhmu else it.spanish }
        val answer = pick(w)
        val options = mutableListOf(answer)
        val candidates = pool.shuffled() + allWords.filter { it.categoryId == w.categoryId }.shuffled() + allWords.shuffled()
        for (c in candidates) {
            if (options.size >= 4) break
            if (kind == QuestionKind.SPANISH_TO_YUHMU && !canAskSpanishToYuhmu(c)) continue
            val text = pick(c)
            if (text !in options) options.add(text)
        }
        val prompt = when (kind) {
            QuestionKind.YUHMU_TO_SPANISH -> w.yuhmu
            QuestionKind.SPANISH_TO_YUHMU -> w.spanish
            QuestionKind.LISTEN -> "Escucha y elige el significado"
        }
        // En la práctica ocultamos la palabra objetivo para que el contexto sea una pista,
        // no una respuesta escrita dentro de la propia frase.
        val contextText = w.context?.replace("«${w.yuhmu}»", "________")
            ?.replace("«${w.spanish}»", "________")
        return Question(w.id, kind, prompt, options.shuffled(), answer, contextText)
    }
}
