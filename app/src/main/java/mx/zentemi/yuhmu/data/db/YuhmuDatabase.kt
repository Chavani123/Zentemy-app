package mx.zentemi.yuhmu.data.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * Base de datos local (SQLite). Claves foráneas activas; todas las consultas de la app son
 * parametrizadas (nunca se concatena texto del usuario en el SQL).
 */
class YuhmuDatabase private constructor(context: Context) :
    SQLiteOpenHelper(context.applicationContext, "yuhmu.db", null, VERSION) {

    override fun onConfigure(db: SQLiteDatabase) {
        super.onConfigure(db)
        db.setForeignKeyConstraintsEnabled(true)
    }

    override fun onCreate(db: SQLiteDatabase) {
        SCHEMA.forEach { db.execSQL(it) }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Las unidades saltadas se guardan por separado para no confundirlas con lecciones aprobadas.
        if (oldVersion < 2) {
            db.execSQL(
                """CREATE TABLE IF NOT EXISTS skipped_lessons (
                    user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                    lesson_id  TEXT    NOT NULL,
                    skipped_at INTEGER NOT NULL,
                    PRIMARY KEY (user_id, lesson_id)
                )"""
            )
        }
    }

    companion object {
        const val VERSION = 2

        @Volatile
        private var instance: YuhmuDatabase? = null

        fun get(context: Context): YuhmuDatabase =
            instance ?: synchronized(this) {
                instance ?: YuhmuDatabase(context).also { instance = it }
            }

        val SCHEMA = listOf(
            """CREATE TABLE users (
                id              INTEGER PRIMARY KEY AUTOINCREMENT,
                username        TEXT    NOT NULL UNIQUE COLLATE NOCASE,
                display_name    TEXT    NOT NULL,
                avatar          TEXT    NOT NULL DEFAULT '🌽',
                password_hash   TEXT    NOT NULL,
                recovery_hash   TEXT    NOT NULL,
                failed_attempts INTEGER NOT NULL DEFAULT 0,
                locked_until    INTEGER NOT NULL DEFAULT 0,
                created_at      INTEGER NOT NULL,
                last_login_at   INTEGER
            )""",
            """CREATE TABLE sessions (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                token_hash TEXT    NOT NULL UNIQUE,
                created_at INTEGER NOT NULL,
                expires_at INTEGER NOT NULL
            )""",
            "CREATE INDEX idx_sessions_user ON sessions(user_id)",
            """CREATE TABLE user_progress (
                user_id            INTEGER PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
                points             INTEGER NOT NULL DEFAULT 0 CHECK (points >= 0),
                streak             INTEGER NOT NULL DEFAULT 0 CHECK (streak >= 0),
                best_streak        INTEGER NOT NULL DEFAULT 0 CHECK (best_streak >= 0),
                last_active_day    TEXT,
                daily_day          TEXT,
                daily_count        INTEGER NOT NULL DEFAULT 0,
                games_played       INTEGER NOT NULL DEFAULT 0,
                memorama_wins      INTEGER NOT NULL DEFAULT 0,
                perfect_challenges INTEGER NOT NULL DEFAULT 0
            )""",
            """CREATE TABLE lesson_progress (
                user_id      INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                lesson_id    TEXT    NOT NULL,
                stars        INTEGER NOT NULL CHECK (stars BETWEEN 1 AND 3),
                completed_at INTEGER NOT NULL,
                PRIMARY KEY (user_id, lesson_id)
            )""",
            """CREATE TABLE skipped_lessons (
                user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                lesson_id  TEXT    NOT NULL,
                skipped_at INTEGER NOT NULL,
                PRIMARY KEY (user_id, lesson_id)
            )""",
            """CREATE TABLE learned_words (
                user_id    INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                word_id    TEXT    NOT NULL,
                learned_at INTEGER NOT NULL,
                PRIMARY KEY (user_id, word_id)
            )""",
            """CREATE TABLE badges (
                user_id   INTEGER NOT NULL REFERENCES users(id) ON DELETE CASCADE,
                badge_id  TEXT    NOT NULL,
                earned_at INTEGER NOT NULL,
                PRIMARY KEY (user_id, badge_id)
            )""",
            """CREATE TABLE security_events (
                id         INTEGER PRIMARY KEY AUTOINCREMENT,
                user_id    INTEGER REFERENCES users(id) ON DELETE SET NULL,
                event_type TEXT    NOT NULL,
                detail     TEXT,
                created_at INTEGER NOT NULL
            )""",
            "CREATE INDEX idx_events_user_time ON security_events(user_id, created_at DESC)"
        )
    }
}
