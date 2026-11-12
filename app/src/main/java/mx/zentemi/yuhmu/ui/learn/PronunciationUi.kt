package mx.zentemi.yuhmu.ui.learn

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.zentemi.yuhmu.audio.VoiceMatcher
import mx.zentemi.yuhmu.model.ChallengeResult
import mx.zentemi.yuhmu.model.Word
import mx.zentemi.yuhmu.ui.components.*
import mx.zentemi.yuhmu.ui.theme.Brand
import mx.zentemi.yuhmu.viewmodel.YuhmuViewModel

/** No hay límite de intentos: repetir la práctica no bloquea ni consume oportunidades. */
private enum class Status { IDLE, LISTENING, ANALYZING, OK, FAIL, NO_SPEECH, DENIED }

/**
 * "Escucha y repite": la persona toca el micrófono, dice la palabra y SOLO si coincide con el
 * audio nativo se llama a onPassed (que habilita el avance).
 */
@Composable
fun PronunciationPractice(word: Word, vm: YuhmuViewModel, onPassed: () -> Unit, modifier: Modifier = Modifier) {
    val scope = rememberCoroutineScope()
    val pool = remember(word.id) { vm.content.poolFor(word) }
    var status by remember(word.id) { mutableStateOf(Status.IDLE) }
    var level by remember { mutableFloatStateOf(0f) }

    fun start() {
        if (status == Status.LISTENING || status == Status.ANALYZING) return
        vm.audio.release()
        status = Status.LISTENING
        scope.launch {
            val r = vm.voice.listenAndJudge(word.id, pool) { level = it }
            status = Status.ANALYZING
            delay(250)
            when (r) {
                VoiceMatcher.Result.PASS -> { status = Status.OK; vm.recordSpoken(); delay(700); onPassed() }
                VoiceMatcher.Result.FAIL -> status = Status.FAIL
                VoiceMatcher.Result.NO_SPEECH -> status = Status.NO_SPEECH
                VoiceMatcher.Result.NO_MIC -> status = Status.DENIED
                VoiceMatcher.Result.NO_REFERENCE -> { status = Status.OK; onPassed() }
            }
        }
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) start() else status = Status.DENIED
    }

    val pulse by rememberInfiniteTransition(label = "mic").animateFloat(
        initialValue = 1f, targetValue = 1.18f,
        animationSpec = infiniteRepeatable(tween(550), RepeatMode.Reverse), label = "pulse"
    )

    val (msg, msgColor) = when (status) {
        Status.IDLE -> "Escucha y toca el micrófono para repetir" to Brand.gray
        Status.LISTENING -> "Te escucho… ¡dilo!" to Brand.purple
        Status.ANALYZING -> "Comparando con la pronunciación nativa…" to Brand.purpleMid
        Status.OK -> "¡Excelente pronunciación!" to Brand.greenDark
        Status.FAIL -> "Casi. Escucha otra vez e inténtalo de nuevo" to Brand.redDark
        Status.NO_SPEECH -> "No te escuché bien. Acércate un poco y repite" to Brand.orange
        Status.DENIED -> "Sin permiso de micrófono no podemos escucharte" to Brand.redDark
    }
    val ringColor = when (status) {
        Status.OK -> Brand.green
        Status.FAIL, Status.DENIED -> Brand.red
        Status.LISTENING -> Brand.pink
        else -> Brand.purpleLight
    }

    Column(
        modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(24.dp)).padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(104.dp)) {
            Box(
                Modifier.size(84.dp).scale(if (status == Status.LISTENING) pulse + level * 0.25f else 1f)
                    .background(ringColor.copy(alpha = 0.25f), CircleShape)
            )
            ChunkyCircle(
                size = 70.dp,
                face = if (status == Status.OK) Brand.green else Brand.pink,
                rim = if (status == Status.OK) Brand.greenDark else Color(0xFFD94F7C),
                enabled = status != Status.ANALYZING && status != Status.OK,
                onClick = { if (vm.voice.hasPermission()) start() else launcher.launch(Manifest.permission.RECORD_AUDIO) }
            ) {
                Icon(Icons.Filled.Mic, contentDescription = "Hablar", tint = Color.White, modifier = Modifier.size(34.dp))
            }
        }
        Text(msg, color = msgColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, textAlign = TextAlign.Center)
        if (status == Status.DENIED) {
            TextButton(onClick = onPassed) { Text("Continuar sin micrófono", color = Brand.purple, fontWeight = FontWeight.Bold) }
        }
    }
}

/** Juego "Repite la palabra": 8 palabras con audio; cada una exige pronunciarla bien. */
@Composable
fun SpeakGame(words: List<Word>, vm: YuhmuViewModel, onClose: () -> Unit) {
    val session = remember(words) {
        words.filter { vm.audio.hasAudio(it.id) && it.yuhmu.length <= 14 }.shuffled().take(8)
    }
    var index by remember(session) { mutableIntStateOf(0) }
    var passed by remember(session) { mutableStateOf(false) }
    var result by remember(session) { mutableStateOf<ChallengeResult?>(null) }

    FullScreen {
        val done = result
        when {
            done != null -> ResultContent(done, onClose)
            session.isEmpty() -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Mascot(size = 120.dp, mood = Mood.SAD)
                Spacer(Modifier.height(12.dp))
                Text("Aún no hay audios en esta categoría.", color = Brand.ink, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                Spacer(Modifier.height(16.dp))
                ChunkyButton("Volver", onClick = onClose)
            }
            else -> {
                val word = session[index]
                LaunchedEffect(word.id) { delay(350); vm.audio.play(word.id) }
                Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        IconButton(onClick = onClose) { Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Brand.gray) }
                        ChunkyProgress((index + if (passed) 1 else 0).toFloat() / session.size, Modifier.weight(1f), color = Brand.pink)
                        Text("${index + 1}/${session.size}", fontWeight = FontWeight.ExtraBold, color = Brand.gray)
                    }
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text("Repite la palabra", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Brand.ink)
                        Mascot(size = 90.dp, mood = if (passed) Mood.CHEER else Mood.HAPPY)
                        Column(
                            Modifier.fillMaxWidth().card(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(word.yuhmu, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple, textAlign = TextAlign.Center)
                            Text(word.spanish, fontSize = 20.sp, color = Brand.ink, fontWeight = FontWeight.SemiBold)
                            ChunkyCircle(size = 60.dp, face = Brand.gold, rim = Brand.goldDark, onClick = { vm.audio.play(word.id) }) {
                                Icon(Icons.Filled.VolumeUp, contentDescription = "Escuchar", tint = Color.White, modifier = Modifier.size(30.dp))
                            }
                        }
                        key(word.id) { PronunciationPractice(word, vm, onPassed = { passed = true }) }
                    }
                    ChunkyButton(
                        if (index == session.size - 1) "Terminar" else "Siguiente",
                        enabled = passed,
                        onClick = {
                            if (index == session.size - 1) {
                                val r = ChallengeResult(session.size, session.size, session.size * 15, 3, 0, starsOverride = 3)
                                result = r
                                vm.recordChallenge(r)
                            } else { index++; passed = false }
                        }
                    )
                }
            }
        }
    }
}
