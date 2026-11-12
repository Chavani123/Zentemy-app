package mx.zentemi.yuhmu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.zentemi.yuhmu.data.AuthRepository
import mx.zentemi.yuhmu.data.AuthResult
import mx.zentemi.yuhmu.model.SecurityEvent
import mx.zentemi.yuhmu.model.User

sealed interface AuthState {
    data object Loading : AuthState
    data object LoggedOut : AuthState
    data class LoggedIn(val user: User) : AuthState
}

class AuthViewModel(app: Application) : AndroidViewModel(app) {
    private val repo = AuthRepository(app)

    private val _state = MutableStateFlow<AuthState>(AuthState.Loading)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    /** Código de recuperación recién generado: se muestra una única vez y luego se descarta. */
    private val _recoveryCode = MutableStateFlow<String?>(null)
    val recoveryCode: StateFlow<String?> = _recoveryCode.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        viewModelScope.launch {
            val user = withContext(Dispatchers.IO) { repo.restoreSession() }
            _state.value = if (user != null) AuthState.LoggedIn(user) else AuthState.LoggedOut
        }
    }

    fun clearError() { _error.value = null }
    fun dismissRecoveryCode() { _recoveryCode.value = null }

    private fun submit(block: () -> AuthResult) {
        if (_busy.value) return
        _busy.value = true
        _error.value = null
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { block() }
            when (result) {
                is AuthResult.Success -> {
                    _recoveryCode.value = result.recoveryCode
                    _state.value = AuthState.LoggedIn(result.user)
                }
                is AuthResult.Failure -> _error.value = result.message
            }
            _busy.value = false
        }
    }

    fun register(username: String, displayName: String, avatar: String, password: String) =
        submit { repo.register(username, displayName, avatar, password) }

    fun login(username: String, password: String) = submit { repo.login(username, password) }

    fun recover(username: String, code: String, newPassword: String) =
        submit { repo.recover(username, code, newPassword) }

    fun logout() {
        val current = (_state.value as? AuthState.LoggedIn)?.user
        viewModelScope.launch {
            withContext(Dispatchers.IO) { repo.logout(current?.id) }
            _recoveryCode.value = null
            _state.value = AuthState.LoggedOut
        }
    }

    // ---- acciones de cuenta (devuelven null si salió bien o el mensaje de error) ----
    fun updateDisplayName(name: String, onDone: (String?) -> Unit) {
        val user = (_state.value as? AuthState.LoggedIn)?.user ?: return
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { repo.updateDisplayName(user.id, name) }
            if (r is AuthResult.Success) _state.value = AuthState.LoggedIn(r.user)
            onDone((r as? AuthResult.Failure)?.message)
        }
    }

    fun changePassword(old: String, new: String, onDone: (String?) -> Unit) {
        val user = (_state.value as? AuthState.LoggedIn)?.user ?: return
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { repo.changePassword(user.id, old, new) }
            onDone((r as? AuthResult.Failure)?.message)
        }
    }

    fun deleteAccount(password: String, onDone: (String?) -> Unit) {
        val user = (_state.value as? AuthState.LoggedIn)?.user ?: return
        viewModelScope.launch {
            val r = withContext(Dispatchers.IO) { repo.deleteAccount(user.id, password) }
            if (r is AuthResult.Success) {
                _recoveryCode.value = null
                _state.value = AuthState.LoggedOut
            }
            onDone((r as? AuthResult.Failure)?.message)
        }
    }

    fun logProgressReset() {
        val user = (_state.value as? AuthState.LoggedIn)?.user ?: return
        viewModelScope.launch(Dispatchers.IO) { repo.logEvent(user.id, "PROGRESS_RESET") }
    }

    suspend fun recentEvents(): List<SecurityEvent> {
        val user = (_state.value as? AuthState.LoggedIn)?.user ?: return emptyList()
        return withContext(Dispatchers.IO) { repo.recentEvents(user.id) }
    }
}
