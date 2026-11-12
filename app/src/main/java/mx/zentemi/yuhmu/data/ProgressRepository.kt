package mx.zentemi.yuhmu.data

import android.content.Context
import mx.zentemi.yuhmu.data.db.YuhmuDatabase

/** Persiste el progreso por usuario (todas las consultas se filtran por user_id: aislamiento de datos). */
class ProgressRepository(context: Context) {
    private val helper = YuhmuDatabase.get(context.applicationContext)

    fun load(userId: Long): UserProgress {
        val db = helper.readableDatabase
        val args = arrayOf(userId.toString())
        var p = UserProgress()
        db.rawQuery(
            "SELECT points, streak, best_streak, last_active_day, daily_day, daily_count, games_played, memorama_wins, perfect_challenges FROM user_progress WHERE user_id = ?",
            args
        ).use { c ->
            if (c.moveToFirst()) {
                p = p.copy(
                    points = c.getInt(0), streak = c.getInt(1), bestStreak = c.getInt(2),
                    lastActiveDay = if (c.isNull(3)) null else c.getString(3),
                    dailyDay = if (c.isNull(4)) null else c.getString(4),
                    dailyCount = c.getInt(5), gamesPlayed = c.getInt(6),
                    memoramaWins = c.getInt(7), perfectChallenges = c.getInt(8)
                )
            }
        }
        val lessons = db.rawQuery("SELECT lesson_id, stars FROM lesson_progress WHERE user_id = ?", args).use { c ->
            buildMap { while (c.moveToNext()) put(c.getString(0), c.getInt(1)) }
        }
        val words = db.rawQuery("SELECT word_id FROM learned_words WHERE user_id = ?", args).use { c ->
            buildSet { while (c.moveToNext()) add(c.getString(0)) }
        }
        val badges = db.rawQuery("SELECT badge_id FROM badges WHERE user_id = ?", args).use { c ->
            buildSet { while (c.moveToNext()) add(c.getString(0)) }
        }
        val skipped = db.rawQuery("SELECT lesson_id FROM skipped_lessons WHERE user_id = ?", args).use { c ->
            buildSet { while (c.moveToNext()) add(c.getString(0)) }
        }
        return p.copy(completedLessons = lessons, skippedLessons = skipped, learnedWordIds = words, badges = badges)
    }

    fun save(userId: Long, p: UserProgress) {
        val db = helper.writableDatabase
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            db.execSQL(
                "UPDATE user_progress SET points = ?, streak = ?, best_streak = ?, last_active_day = ?, daily_day = ?, daily_count = ?, games_played = ?, memorama_wins = ?, perfect_challenges = ? WHERE user_id = ?",
                arrayOf<Any?>(
                    p.points, p.streak, p.bestStreak, p.lastActiveDay, p.dailyDay,
                    p.dailyCount, p.gamesPlayed, p.memoramaWins, p.perfectChallenges, userId
                )
            )
            p.completedLessons.forEach { (lesson, stars) ->
                db.execSQL(
                    "INSERT OR IGNORE INTO lesson_progress(user_id, lesson_id, stars, completed_at) VALUES (?,?,?,?)",
                    arrayOf<Any?>(userId, lesson, stars.coerceIn(1, 3), now)
                )
                db.execSQL(
                    "UPDATE lesson_progress SET stars = ? WHERE user_id = ? AND lesson_id = ?",
                    arrayOf<Any?>(stars.coerceIn(1, 3), userId, lesson)
                )
            }
            // Si el usuario vuelve a una unidad saltada y la completa, deja de figurar como omitida.
            p.completedLessons.keys.forEach { lesson ->
                db.execSQL(
                    "DELETE FROM skipped_lessons WHERE user_id = ? AND lesson_id = ?",
                    arrayOf<Any?>(userId, lesson)
                )
            }
            p.skippedLessons.forEach { lesson ->
                db.execSQL(
                    "INSERT OR IGNORE INTO skipped_lessons(user_id, lesson_id, skipped_at) VALUES (?,?,?)",
                    arrayOf<Any?>(userId, lesson, now)
                )
            }
            p.learnedWordIds.forEach {
                db.execSQL(
                    "INSERT OR IGNORE INTO learned_words(user_id, word_id, learned_at) VALUES (?,?,?)",
                    arrayOf<Any?>(userId, it, now)
                )
            }
            p.badges.forEach {
                db.execSQL(
                    "INSERT OR IGNORE INTO badges(user_id, badge_id, earned_at) VALUES (?,?,?)",
                    arrayOf<Any?>(userId, it, now)
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun reset(userId: Long) {
        val db = helper.writableDatabase
        db.beginTransaction()
        try {
            listOf("lesson_progress", "skipped_lessons", "learned_words", "badges").forEach {
                db.execSQL("DELETE FROM $it WHERE user_id = ?", arrayOf<Any?>(userId))
            }
            db.execSQL(
                "UPDATE user_progress SET points = 0, streak = 0, best_streak = 0, last_active_day = NULL, daily_day = NULL, daily_count = 0, games_played = 0, memorama_wins = 0, perfect_challenges = 0 WHERE user_id = ?",
                arrayOf<Any?>(userId)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
