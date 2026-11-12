package mx.zentemi.yuhmu.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import mx.zentemi.yuhmu.data.db.YuhmuDatabase
import mx.zentemi.yuhmu.model.AVATARS
import mx.zentemi.yuhmu.model.SecurityEvent
import mx.zentemi.yuhmu.model.User
import mx.zentemi.yuhmu.security.PasswordHasher
import mx.zentemi.yuhmu.security.SessionVault
import mx.zentemi.yuhmu.security.Validators
import java.security.SecureRandom

sealed interface AuthResult {
    data class Success(val user: User, val recoveryCode: String? = null) : AuthResult
    data class Failure(val message: String) : AuthResult
}

/**
 * Autenticación local: registro, inicio de sesión, sesión persistente, recuperación y borrado.
 * Todos los métodos son bloqueantes (hash + SQLite): llamar desde Dispatchers.IO.
 */
class AuthRepository(context: Context) {
    private val appContext = context.applicationContext
    private val helper = YuhmuDatabase.get(appContext)
    private val vault = SessionVault(appContext)
    private val random = SecureRandom()

    private class Row(
        val id: Long, val username: String, val displayName: String, val avatar: String,
        val passwordHash: String, val recoveryHash: String,
        val failedAttempts: Int, val lockedUntil: Long, val createdAt: Long
    ) {
        fun toUser() = User(id, username, displayName, avatar, createdAt)
    }

    // ------------------------------------------------------------------ registro
    fun register(usernameRaw: String, displayNameRaw: String, avatar: String, password: String): AuthResult = safely {
        val username = usernameRaw.trim()
        val displayName = displayNameRaw.trim().replace(Regex("\\s+"), " ")
        Validators.username(username)?.let { return@safely AuthResult.Failure(it) }
        Validators.displayName(displayName)?.let { return@safely AuthResult.Failure(it) }
        Validators.password(password, username)?.let { return@safely AuthResult.Failure(it) }

        val db = helper.writableDatabase
        val taken = db.rawQuery("SELECT 1 FROM users WHERE username = ? COLLATE NOCASE", arrayOf(username))
            .use { it.moveToFirst() }
        if (taken) return@safely AuthResult.Failure("Ese nombre de usuario ya está en uso.")

        val safeAvatar = if (avatar in AVATARS) avatar else AVATARS.first()
        val recovery = newRecoveryCode()
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("username", username)
            put("display_name", displayName)
            put("avatar", safeAvatar)
            put("password_hash", PasswordHasher.hash(password.toCharArray()))
            put("recovery_hash", PasswordHasher.hash(normalizeCode(recovery).toCharArray()))
            put("created_at", now)
        }

        var userId = -1L
        db.beginTransaction()
        try {
            userId = db.insertOrThrow("users", null, values)
            db.execSQL("INSERT INTO user_progress(user_id) VALUES (?)", arrayOf<Any?>(userId))
            audit(db, userId, "REGISTER")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        startSession(userId)
        AuthResult.Success(User(userId, username, displayName, safeAvatar, now), recovery)
    }

    // ------------------------------------------------------------------ login
    fun login(usernameRaw: String, password: String): AuthResult = safely {
        val db = helper.writableDatabase
        val now = System.currentTimeMillis()
        val row = findByUsername(db, usernameRaw.trim())

        if (row == null) {
            // Se hace un hash de relleno para que el tiempo de respuesta no revele si el usuario existe.
            PasswordHasher.verify(password.toCharArray(), DUMMY_HASH)
            audit(db, null, "LOGIN_FAIL", "usuario_desconocido")
            return@safely INVALID_LOGIN
        }
        if (row.lockedUntil > now) return@safely AuthResult.Failure(lockedMessage(row.lockedUntil - now))

        if (!PasswordHasher.verify(password.toCharArray(), row.passwordHash)) {
            return@safely registerFailure(db, row, now, INVALID_LOGIN)
        }
        db.execSQL(
            "UPDATE users SET failed_attempts = 0, locked_until = 0, last_login_at = ? WHERE id = ?",
            arrayOf<Any?>(now, row.id)
        )
        audit(db, row.id, "LOGIN_OK")
        startSession(row.id)
        AuthResult.Success(row.toUser())
    }

    // ------------------------------------------------------------------ recuperación
    /** Restablece la contraseña con el código de recuperación. El código es de un solo uso. */
    fun recover(usernameRaw: String, codeRaw: String, newPassword: String): AuthResult = safely {
        val db = helper.writableDatabase
        val now = System.currentTimeMillis()
        val row = findByUsername(db, usernameRaw.trim())
        if (row == null) {
            PasswordHasher.verify(codeRaw.toCharArray(), DUMMY_HASH)
            return@safely INVALID_RECOVERY
        }
        if (row.lockedUntil > now) return@safely AuthResult.Failure(lockedMessage(row.lockedUntil - now))
        if (!PasswordHasher.verify(normalizeCode(codeRaw).toCharArray(), row.recoveryHash)) {
            return@safely registerFailure(db, row, now, INVALID_RECOVERY)
        }
        Validators.password(newPassword, row.username)?.let { return@safely AuthResult.Failure(it) }

        val newCode = newRecoveryCode()
        db.execSQL(
            "UPDATE users SET password_hash = ?, recovery_hash = ?, failed_attempts = 0, locked_until = 0, last_login_at = ? WHERE id = ?",
            arrayOf<Any?>(
                PasswordHasher.hash(newPassword.toCharArray()),
                PasswordHasher.hash(normalizeCode(newCode).toCharArray()),
                now, row.id
            )
        )
        audit(db, row.id, "PASSWORD_RESET")
        startSession(row.id)            // invalida cualquier sesión anterior
        AuthResult.Success(row.toUser(), newCode)
    }

    // ------------------------------------------------------------------ sesión
    fun restoreSession(): User? {
        return try {
            val (userId, token) = vault.load() ?: return null
            helper.readableDatabase.rawQuery(
                """SELECT u.id, u.username, u.display_name, u.avatar, u.created_at
                   FROM sessions s JOIN users u ON u.id = s.user_id
                   WHERE s.user_id = ? AND s.token_hash = ? AND s.expires_at > ?""",
                arrayOf(userId.toString(), PasswordHasher.sha256Hex(token), System.currentTimeMillis().toString())
            ).use { c ->
                if (c.moveToFirst()) userFrom(c) else { vault.clear(); null }
            }
        } catch (e: Exception) {
            vault.clear()
            null
        }
    }

    fun logout(userId: Long?) {
        try {
            val db = helper.writableDatabase
            vault.load()?.let { (_, token) ->
                db.execSQL("DELETE FROM sessions WHERE token_hash = ?", arrayOf<Any?>(PasswordHasher.sha256Hex(token)))
            }
            if (userId != null) audit(db, userId, "LOGOUT")
        } catch (e: Exception) {
            // el cierre de sesión local nunca debe fallar de cara al usuario
        } finally {
            vault.clear()
        }
    }

    // ------------------------------------------------------------------ cuenta
    fun changePassword(userId: Long, oldPassword: String, newPassword: String): AuthResult = safely {
        val db = helper.writableDatabase
        val row = findById(db, userId) ?: return@safely AuthResult.Failure("Cuenta no encontrada.")
        if (!PasswordHasher.verify(oldPassword.toCharArray(), row.passwordHash)) {
            audit(db, userId, "PASSWORD_CHANGE_FAIL")
            return@safely AuthResult.Failure("La contraseña actual no es correcta.")
        }
        Validators.password(newPassword, row.username)?.let { return@safely AuthResult.Failure(it) }
        db.execSQL(
            "UPDATE users SET password_hash = ? WHERE id = ?",
            arrayOf<Any?>(PasswordHasher.hash(newPassword.toCharArray()), userId)
        )
        audit(db, userId, "PASSWORD_CHANGE")
        startSession(userId)
        AuthResult.Success(row.toUser())
    }

    fun updateDisplayName(userId: Long, nameRaw: String): AuthResult = safely {
        val name = nameRaw.trim().replace(Regex("\\s+"), " ")
        Validators.displayName(name)?.let { return@safely AuthResult.Failure(it) }
        val db = helper.writableDatabase
        db.execSQL("UPDATE users SET display_name = ? WHERE id = ?", arrayOf<Any?>(name, userId))
        val row = findById(db, userId) ?: return@safely AuthResult.Failure("Cuenta no encontrada.")
        AuthResult.Success(row.toUser())
    }

    /** Borra la cuenta y TODOS sus datos (derecho de cancelación). Las FK en cascada limpian el resto. */
    fun deleteAccount(userId: Long, password: String): AuthResult = safely {
        val db = helper.writableDatabase
        val row = findById(db, userId) ?: return@safely AuthResult.Failure("Cuenta no encontrada.")
        if (!PasswordHasher.verify(password.toCharArray(), row.passwordHash)) {
            audit(db, userId, "ACCOUNT_DELETE_FAIL")
            return@safely AuthResult.Failure("La contraseña no es correcta.")
        }
        db.execSQL("DELETE FROM users WHERE id = ?", arrayOf<Any?>(userId))
        audit(db, null, "ACCOUNT_DELETE")
        vault.clear()
        AuthResult.Success(row.toUser())
    }

    fun logEvent(userId: Long, type: String) {
        try { audit(helper.writableDatabase, userId, type) } catch (e: Exception) { /* no crítico */ }
    }

    fun recentEvents(userId: Long, limit: Int = 8): List<SecurityEvent> =
        helper.readableDatabase.rawQuery(
            "SELECT event_type, created_at, detail FROM security_events WHERE user_id = ? ORDER BY created_at DESC, id DESC LIMIT ?",
            arrayOf(userId.toString(), limit.toString())
        ).use { c ->
            buildList { while (c.moveToNext()) add(SecurityEvent(c.getString(0), c.getLong(1), if (c.isNull(2)) null else c.getString(2))) }
        }

    // ------------------------------------------------------------------ internos
    private fun registerFailure(db: SQLiteDatabase, row: Row, now: Long, message: AuthResult.Failure): AuthResult.Failure {
        val attempts = row.failedAttempts + 1
        return if (attempts >= MAX_ATTEMPTS) {
            db.execSQL(
                "UPDATE users SET failed_attempts = 0, locked_until = ? WHERE id = ?",
                arrayOf<Any?>(now + LOCK_MS, row.id)
            )
            audit(db, row.id, "LOCKOUT")
            AuthResult.Failure(lockedMessage(LOCK_MS))
        } else {
            db.execSQL("UPDATE users SET failed_attempts = ? WHERE id = ?", arrayOf<Any?>(attempts, row.id))
            audit(db, row.id, "LOGIN_FAIL")
            message
        }
    }

    private fun startSession(userId: Long) {
        val db = helper.writableDatabase
        val now = System.currentTimeMillis()
        val token = ByteArray(32).also { random.nextBytes(it) }
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        db.beginTransaction()
        try {
            // Una sola sesión activa por cuenta + limpieza de sesiones vencidas
            db.execSQL("DELETE FROM sessions WHERE user_id = ? OR expires_at < ?", arrayOf<Any?>(userId, now))
            db.execSQL(
                "INSERT INTO sessions(user_id, token_hash, created_at, expires_at) VALUES (?,?,?,?)",
                arrayOf<Any?>(userId, PasswordHasher.sha256Hex(token), now, now + SESSION_MS)
            )
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        vault.save(userId, token)
    }

    private fun audit(db: SQLiteDatabase, userId: Long?, type: String, detail: String? = null) {
        db.execSQL(
            "INSERT INTO security_events(user_id, event_type, detail, created_at) VALUES (?,?,?,?)",
            arrayOf<Any?>(userId, type, detail, System.currentTimeMillis())
        )
        // Bitácora acotada: se conservan los últimos 500 eventos
        db.execSQL("DELETE FROM security_events WHERE id <= (SELECT MAX(id) FROM security_events) - 500")
    }

    private fun findByUsername(db: SQLiteDatabase, username: String): Row? =
        db.rawQuery(SELECT_ROW + " WHERE username = ? COLLATE NOCASE", arrayOf(username)).use { c ->
            if (c.moveToFirst()) rowFrom(c) else null
        }

    private fun findById(db: SQLiteDatabase, id: Long): Row? =
        db.rawQuery(SELECT_ROW + " WHERE id = ?", arrayOf(id.toString())).use { c ->
            if (c.moveToFirst()) rowFrom(c) else null
        }

    private fun rowFrom(c: Cursor) = Row(
        c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4),
        c.getString(5), c.getInt(6), c.getLong(7), c.getLong(8)
    )

    private fun userFrom(c: Cursor) = User(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getLong(4))

    private fun newRecoveryCode(): String {
        val chars = CharArray(12) { CODE_ALPHABET[random.nextInt(CODE_ALPHABET.length)] }
        return String(chars).chunked(4).joinToString("-")
    }

    private fun normalizeCode(code: String): String =
        code.uppercase().filter { it.isLetterOrDigit() }

    private fun lockedMessage(remainingMs: Long): String {
        val minutes = ((remainingMs + 59_999) / 60_000).coerceAtLeast(1)
        return "Demasiados intentos fallidos. Inténtalo de nuevo en $minutes min."
    }

    /** Nunca se propagan detalles internos (excepciones, SQL) a la interfaz. */
    private inline fun safely(block: () -> AuthResult): AuthResult = try {
        block()
    } catch (e: SQLiteConstraintException) {
        AuthResult.Failure("Ese nombre de usuario ya está en uso.")
    } catch (e: Exception) {
        AuthResult.Failure("Ocurrió un error inesperado. Inténtalo de nuevo.")
    }

    private companion object {
        const val MAX_ATTEMPTS = 5
        const val LOCK_MS = 5 * 60 * 1000L
        const val SESSION_MS = 30L * 24 * 60 * 60 * 1000
        const val CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789"   // sin 0/O/1/I/L para evitar confusiones
        const val SELECT_ROW =
            "SELECT id, username, display_name, avatar, password_hash, recovery_hash, failed_attempts, locked_until, created_at FROM users"

        val INVALID_LOGIN = AuthResult.Failure("Usuario o contraseña incorrectos.")
        val INVALID_RECOVERY = AuthResult.Failure("Usuario o código de recuperación incorrectos.")
        val DUMMY_HASH: String by lazy { PasswordHasher.hash("relleno-para-igualar-tiempos".toCharArray()) }
    }
}
