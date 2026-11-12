package mx.zentemi.yuhmu.model

import kotlinx.serialization.Serializable

@Serializable
data class Word(
    val id: String,          // ej. "cuerpo_012" -> también es el nombre del audio
    val yuhmu: String,
    val spanish: String,
    val note: String? = null,
    val context: String? = null
) {
    val categoryId: String get() = id.substringBefore("_")
}

@Serializable
data class WordCategory(
    val id: String,
    val title: String,
    val icon: String,        // nombre del icono original (SF Symbol)
    val words: List<Word>
)

@Serializable
data class VocabularyFile(val version: Int, val categories: List<WordCategory>)

data class Lesson(
    val id: String,
    val categoryId: String,
    val categoryTitle: String,
    val number: Int,
    val words: List<Word>
) {
    val title: String get() = "$categoryTitle $number"
}

enum class QuestionKind { YUHMU_TO_SPANISH, SPANISH_TO_YUHMU, LISTEN }

data class Question(
    val wordId: String,
    val kind: QuestionKind,
    val prompt: String,
    val options: List<String>,
    val answer: String,
    val context: String? = null
)

data class ChallengeResult(
    val correct: Int,
    val total: Int,
    val points: Int,
    val livesLeft: Int,
    val bestCombo: Int,
    val starsOverride: Int? = null
) {
    val ratio: Double get() = if (total == 0) 0.0 else correct.toDouble() / total
    val stars: Int
        get() = starsOverride ?: when {
            ratio >= 0.9 -> 3
            ratio >= 0.7 -> 2
            ratio >= 0.5 -> 1
            else -> 0
        }
}
