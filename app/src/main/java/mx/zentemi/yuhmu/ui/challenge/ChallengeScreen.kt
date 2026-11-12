package mx.zentemi.yuhmu.ui.challenge

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.zentemi.yuhmu.audio.AudioPlayer
import mx.zentemi.yuhmu.model.ChallengeResult
import mx.zentemi.yuhmu.model.Question
import mx.zentemi.yuhmu.model.QuestionKind
import mx.zentemi.yuhmu.ui.components.*
import mx.zentemi.yuhmu.ui.theme.Brand
import kotlin.math.max
import kotlin.math.min

/**
 * Motor de preguntas reutilizado por lecciones, Reto del día y Retos de vocabulario.
 * Flujo estilo Duolingo: elegir → COMPROBAR → retroalimentación → CONTINUAR.
 */
@Composable
fun ChallengeScreen(
    questions: List<Question>,
    audio: AudioPlayer,
    showLevels: Boolean = false,
    questionsPerLevel: Int = 3,
    allowAudioPlayback: Boolean = true,
    onComplete: (ChallengeResult) -> Unit,
    onClose: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    var current by remember(questions) { mutableIntStateOf(0) }
    var correct by remember(questions) { mutableIntStateOf(0) }
    var combo by remember(questions) { mutableIntStateOf(0) }
    var bestCombo by remember(questions) { mutableIntStateOf(0) }
    var points by remember(questions) { mutableIntStateOf(0) }
    var selected by remember(questions) { mutableStateOf<String?>(null) }
    var checked by remember(questions) { mutableStateOf(false) }
    var result by remember(questions) { mutableStateOf<ChallengeResult?>(null) }
    var confirmExit by remember { mutableStateOf(false) }

    val finished = result
    if (finished != null) {
        ResultContent(finished, onClose)
        return
    }
    if (questions.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No hay preguntas disponibles.") }
        return
    }

    val question = questions[current]
    val isLast = current + 1 >= questions.size
    val wasCorrect = checked && selected == question.answer

    LaunchedEffect(current, questions) {
        if (question.kind == QuestionKind.LISTEN) audio.play(question.wordId)
    }

    fun check() {
        val choice = selected ?: return
        checked = true
        if (choice == question.answer) {
            correct++
            combo++
            bestCombo = max(bestCombo, combo)
            points += 10 + min(combo - 1, 5) * 2
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            // Los errores no penalizan ni terminan la ronda: la pregunta se puede repetir sin límite.
            combo = 0
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }

    fun next() {
        if (!wasCorrect) {
            selected = null
            checked = false
            return
        }
        if (isLast) {
            val r = ChallengeResult(correct, questions.size, points, 0, bestCombo)
            result = r
            onComplete(r)
        } else {
            current++
            selected = null
            checked = false
        }
    }

    fun stateOf(option: String): OptionState = when {
        !checked -> if (option == selected) OptionState.SELECTED else OptionState.IDLE
        option == question.answer -> OptionState.CORRECT
        option == selected -> OptionState.WRONG
        else -> OptionState.DIM
    }

    Column(Modifier.fillMaxSize()) {
        // Barra superior: cerrar y progreso de la actividad
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            IconButton(onClick = { confirmExit = true }) {
                Icon(Icons.Filled.Close, contentDescription = "Salir", tint = Brand.gray)
            }
            ChunkyProgress(
                fraction = (current + if (checked && wasCorrect) 1 else 0).toFloat() / questions.size,
                modifier = Modifier.weight(1f)
            )
        }

        // Pregunta + opciones
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (showLevels) {
                val levelNames = listOf("Primeros pasos", "Traducción rápida", "Oído atento", "Reto combinado", "Gran final")
                val levelNumber = (current / questionsPerLevel.coerceAtLeast(1) + 1).coerceAtMost(levelNames.size)
                Row(
                    Modifier.fillMaxWidth()
                        .background(Brand.lavender.copy(alpha = 0.55f), RoundedCornerShape(14.dp))
                        .padding(horizontal = 14.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("NIVEL $levelNumber DE ${levelNames.size}", color = Brand.purple, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold)
                    Text(levelNames[levelNumber - 1], color = Brand.purple, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
            question.context?.takeIf { it.isNotBlank() }?.let { clue ->
                Column(
                    Modifier.fillMaxWidth().card(),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text("PISTA EN CONTEXTO", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purpleMid, letterSpacing = 0.8.sp)
                    Text(clue, fontSize = 15.sp, lineHeight = 21.sp, color = Brand.ink)
                }
            }
            Text(
                when (question.kind) {
                    QuestionKind.YUHMU_TO_SPANISH -> "¿Qué significa?"
                    QuestionKind.SPANISH_TO_YUHMU -> "¿Cómo se dice en Yuhmu?"
                    QuestionKind.LISTEN -> "Escucha y elige"
                },
                fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Brand.ink
            )
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Mascot(
                    size = 88.dp,
                    mood = when {
                        !checked -> Mood.HAPPY
                        wasCorrect -> Mood.CHEER
                        else -> Mood.SAD
                    }
                )
                Row(
                    Modifier.weight(1f)
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .border(2.dp, Brand.lavender, RoundedCornerShape(18.dp))
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (question.kind != QuestionKind.LISTEN) {
                        Text(
                            question.prompt, Modifier.weight(1f),
                            fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple
                        )
                    } else {
                        Text(
                            "Toca el altavoz para escuchar", Modifier.weight(1f),
                            fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Brand.ink
                        )
                    }
                    if (allowAudioPlayback && (question.kind == QuestionKind.LISTEN || audio.hasAudio(question.wordId))) {
                        ChunkyCircle(size = 46.dp, face = Brand.purpleMid, rim = Brand.purpleMidDark, onClick = { audio.play(question.wordId) }) {
                            Icon(Icons.Filled.VolumeUp, contentDescription = "Escuchar", tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(2.dp))
            question.options.forEach { option ->
                OptionCard(option, stateOf(option), enabled = !checked) { selected = option }
            }
            Spacer(Modifier.height(12.dp))
        }

        // Panel inferior: comprobar / retroalimentación
        val panelColor = when {
            !checked -> Color.White
            wasCorrect -> Brand.greenLight
            else -> Brand.redLight
        }
        Column(
            Modifier.fillMaxWidth().background(panelColor).padding(horizontal = 20.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (checked) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(
                        if (wasCorrect) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        contentDescription = null,
                        tint = if (wasCorrect) Brand.greenDark else Brand.orange,
                        modifier = Modifier.size(34.dp)
                    )
                    Column {
                        Text(
                            if (wasCorrect) "¡Excelente!" else "¡Casi! Vamos otra vez",
                            fontSize = 20.sp, fontWeight = FontWeight.ExtraBold,
                            color = if (wasCorrect) Brand.greenDark else Brand.orange
                        )
                        if (wasCorrect && combo >= 2) {
                            Text("¡Combo x$combo!", fontSize = 14.sp, color = Brand.greenDark, fontWeight = FontWeight.Bold)
                        } else if (!wasCorrect) {
                            Text("Puedes intentarlo tantas veces como necesites.", fontSize = 13.sp, color = Brand.ink)
                        }
                    }
                }
                ChunkyButton(
                    when {
                        !wasCorrect -> "Intentar de nuevo"
                        isLast -> "Terminar"
                        else -> "Continuar"
                    },
                    color = if (wasCorrect) Brand.green else Brand.orange,
                    edge = if (wasCorrect) Brand.greenDark else Color(0xFFD97A00),
                    onClick = { next() }
                )
            } else {
                ChunkyButton("Comprobar", enabled = selected != null, onClick = { check() })
            }
        }
    }

    if (confirmExit) {
        AlertDialog(
            onDismissRequest = { confirmExit = false },
            title = { Text("¿Salir de la práctica?", fontWeight = FontWeight.ExtraBold) },
            text = { Text("Perderás el progreso de esta sesión.") },
            confirmButton = { TextButton(onClick = { confirmExit = false; onClose() }) { Text("Salir", color = Brand.red, fontWeight = FontWeight.Bold) } },
            dismissButton = { TextButton(onClick = { confirmExit = false }) { Text("Seguir", color = Brand.purple, fontWeight = FontWeight.Bold) } }
        )
    }
}
