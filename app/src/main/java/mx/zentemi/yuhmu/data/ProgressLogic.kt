package mx.zentemi.yuhmu.data

import kotlinx.serialization.Serializable
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

@Serializable
data class UserProgress(
    val name: String = "Explorador",
    val points: Int = 0,
    val streak: Int = 0,
    val bestStreak: Int = 0,
    val lastActiveDay: String? = null,
    val completedLessons: Map<String, Int> = emptyMap(),   // lessonId -> estrellas (1..3)
    val skippedLessons: Set<String> = emptySet(),
    val learnedWordIds: Set<String> = emptySet(),
    val badges: Set<String> = emptySet(),
    val dailyDay: String? = null,
    val dailyCount: Int = 0,
    val gamesPlayed: Int = 0,
    val memoramaWins: Int = 0,
    val perfectChallenges: Int = 0,
    val storiesDone: Set<String> = emptySet(),
    val spokenWords: Int = 0,
    val selectedAvatar: String = "rabbit"
)

fun UserProgress.avatarCharacter(): mx.zentemi.yuhmu.model.AppCharacter =
    mx.zentemi.yuhmu.model.AppCharacter.fromId(selectedAvatar)

const val LEVEL_STEP = 150
val LEVEL_TITLES = listOf("Semilla", "Brote", "Retoño", "Milpa", "Maíz tierno", "Jilote", "Mazorca", "Cosecha", "Sabio de Ixtenco", "Voz del Yhụmụ", "Guardián Zentemi")

fun dayString(offsetDays: Int = 0): String {
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, offsetDays)
    return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
}

/** Número de día desde 1970 en la zona horaria local. */
fun dayNumber(): Int {
    val now = System.currentTimeMillis()
    return ((now + TimeZone.getDefault().getOffset(now)) / 86_400_000L).toInt()
}

// Derivados (equivalentes a las propiedades computadas de ProgressStore)
fun UserProgress.level(): Int = points / LEVEL_STEP + 1
fun UserProgress.levelTitle(): String = LEVEL_TITLES[minOf(level() - 1, LEVEL_TITLES.lastIndex)]
fun UserProgress.levelProgress(): Float = (points % LEVEL_STEP).toFloat() / LEVEL_STEP
fun UserProgress.pointsToNextLevel(): Int = LEVEL_STEP - points % LEVEL_STEP
fun UserProgress.dailyDone(): Boolean = dailyDay == dayString()

/** Racha vigente: se pierde si pasó más de un día sin actividad. */
fun UserProgress.currentStreak(): Int {
    val last = lastActiveDay ?: return 0
    return if (last == dayString() || last == dayString(-1)) streak else 0
}

private fun UserProgress.withActivity(): UserProgress {
    val today = dayString()
    if (lastActiveDay == today) return this
    val newStreak = if (lastActiveDay == dayString(-1)) streak + 1 else 1
    return copy(streak = newStreak, bestStreak = maxOf(bestStreak, newStreak), lastActiveDay = today)
}

private fun UserProgress.awarded(pts: Int): UserProgress {
    val base = withActivity()
    return base.copy(points = base.points + pts)
}


/** Saltar una unidad solo desbloquea la siguiente; no otorga XP, estrellas ni palabras aprendidas. */
fun UserProgress.afterSkippingLesson(lessonId: String): UserProgress =
    if (lessonId in completedLessons || lessonId in skippedLessons) this
    else copy(skippedLessons = skippedLessons + lessonId)

/** Reglas de insignias (idénticas a evaluateBadges() en iOS). */
fun UserProgress.withBadges(): UserProgress {
    val b = badges.toMutableSet()
    val lessons = completedLessons.size
    val words = learnedWordIds.size
    if (lessons >= 1) b += "first_lesson"
    if (lessons >= 5) b += "lessons_5"
    if (bestStreak >= 3) b += "streak_3"
    if (bestStreak >= 7) b += "streak_7"
    if (words >= 50) b += "words_50"
    if (words >= 100) b += "words_100"
    if (points >= 500) b += "points_500"
    if (dailyCount >= 1) b += "daily_1"
    if (memoramaWins >= 1) b += "memorama_1"
    if (perfectChallenges >= 1) b += "perfect_1"
    if (lessons >= 20) b += "lessons_20"
    if (words >= 150) b += "words_150"
    if (points >= 1500) b += "points_1500"
    if (storiesDone.isNotEmpty()) b += "story_1"
    if (storiesDone.size >= 4) b += "story_4"
    if (spokenWords >= 10) b += "speak_10"
    if (spokenWords >= 50) b += "speak_50"
    return copy(badges = b)
}

// Acciones puras: devuelven el nuevo progreso
fun UserProgress.afterLesson(lessonId: String, wordIds: List<String>, points: Int, stars: Int, correct: Int, total: Int): UserProgress {
    // Una lección aprobada otorga al menos 100 XP; los aciertos y combos pueden elevar la recompensa.
    val lessonReward = if (stars > 0) maxOf(points + 20, 100) else points
    var n = awarded(lessonReward)
    if (stars > 0) {
        n = n.copy(
            completedLessons = n.completedLessons + (lessonId to maxOf(n.completedLessons[lessonId] ?: 0, stars)),
            skippedLessons = n.skippedLessons - lessonId,
            learnedWordIds = n.learnedWordIds + wordIds
        )
    }
    if (total > 0 && correct == total) n = n.copy(perfectChallenges = n.perfectChallenges + 1)
    return n
}

fun UserProgress.afterDaily(points: Int): UserProgress {
    if (dailyDone()) return this
    val n = awarded(points + 50)
    return n.copy(dailyDay = dayString(), dailyCount = n.dailyCount + 1)
}

fun UserProgress.afterChallenge(points: Int, correct: Int, total: Int): UserProgress {
    var n = awarded(points).copy(gamesPlayed = gamesPlayed + 1)
    if (total > 0 && correct == total) n = n.copy(perfectChallenges = n.perfectChallenges + 1)
    return n
}

fun UserProgress.afterMemorama(points: Int, won: Boolean): UserProgress {
    val n = awarded(points).copy(gamesPlayed = gamesPlayed + 1)
    return if (won) n.copy(memoramaWins = n.memoramaWins + 1) else n
}

fun UserProgress.afterStory(storyId: String, points: Int): UserProgress {
    val n = awarded(if (storyId in storiesDone) points / 3 else points)
    return n.copy(storiesDone = n.storiesDone + storyId)
}

fun UserProgress.afterSpoken(points: Int): UserProgress {
    val n = awarded(points)
    return n.copy(spokenWords = n.spokenWords + 1)
}
