package mx.zentemi.yuhmu.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import mx.zentemi.yuhmu.audio.AudioPlayer
import mx.zentemi.yuhmu.audio.VoiceMatcher
import mx.zentemi.yuhmu.data.*
import mx.zentemi.yuhmu.model.ChallengeResult
import mx.zentemi.yuhmu.model.Lesson
import mx.zentemi.yuhmu.model.User

import mx.zentemi.yuhmu.util.AppLanguage
import mx.zentemi.yuhmu.util.LanguageManager

/** Contenido + progreso del usuario con sesión activa (se enlaza con bindUser al iniciar sesión). */
class YuhmuViewModel(app: Application) : AndroidViewModel(app) {

    val audio = AudioPlayer(app)
    val content = ContentRepository(app, audio)
    val voice = VoiceMatcher(app, audio)
    private val repo = ProgressRepository(app)
    private val writeLock = Mutex()

    private var userId: Long? = null

    private val _progress = MutableStateFlow(UserProgress())
    val progress: StateFlow<UserProgress> = _progress.asStateFlow()

    private val _language = MutableStateFlow(LanguageManager.getLanguage(app))
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    fun setLanguage(lang: AppLanguage) {
        LanguageManager.setLanguage(getApplication(), lang)
        _language.value = lang
    }

    fun bindUser(user: User) {
        if (userId == user.id) {
            _progress.value = _progress.value.copy(name = user.displayName)
            return
        }
        userId = user.id
        _progress.value = UserProgress(name = user.displayName)
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { repo.load(user.id) }
            if (userId == user.id) _progress.value = loaded.copy(name = user.displayName)
        }
    }

    fun unbind() {
        userId = null
        _progress.value = UserProgress()
    }

    private fun update(transform: (UserProgress) -> UserProgress) {
        val id = userId ?: return
        val next = transform(_progress.value).withBadges()
        _progress.value = next
        viewModelScope.launch(Dispatchers.IO) { writeLock.withLock { repo.save(id, next) } }
    }

    fun skipLesson(lesson: Lesson) = update { it.afterSkippingLesson(lesson.id) }

    fun completeLesson(lesson: Lesson, result: ChallengeResult) = update {
        it.afterLesson(
            lessonId = lesson.id,
            wordIds = lesson.words.map { w -> w.id },
            points = result.points,
            stars = result.stars,
            correct = result.correct,
            total = result.total
        )
    }

    fun completeDaily(result: ChallengeResult) = update { it.afterDaily(result.points) }

    fun recordChallenge(result: ChallengeResult) = update {
        it.afterChallenge(result.points, result.correct, result.total)
    }

    fun completeStory(storyId: String, points: Int) = update { it.afterStory(storyId, points) }

    fun recordSpoken(points: Int = 5) = update { it.afterSpoken(points) }

    fun recordMemorama(points: Int, won: Boolean) = update { it.afterMemorama(points, won) }

    override fun onCleared() {
        audio.release()
        super.onCleared()
    }
}
