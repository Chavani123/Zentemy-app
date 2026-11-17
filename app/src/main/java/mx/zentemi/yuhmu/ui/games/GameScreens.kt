package mx.zentemi.yuhmu.ui.games

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.zentemi.yuhmu.audio.AudioCatalogItem
import mx.zentemi.yuhmu.data.avatarCharacter
import mx.zentemi.yuhmu.data.dailyDone
import mx.zentemi.yuhmu.model.Word
import mx.zentemi.yuhmu.ui.challenge.ChallengeScreen
import mx.zentemi.yuhmu.ui.components.*
import mx.zentemi.yuhmu.ui.learn.SpeakGame
import mx.zentemi.yuhmu.ui.learn.DailyChallengeCard
import mx.zentemi.yuhmu.ui.learn.NumberSystemCard
import mx.zentemi.yuhmu.ui.stories.StoriesRow
import mx.zentemi.yuhmu.model.Lesson
import mx.zentemi.yuhmu.ui.theme.Brand
import mx.zentemi.yuhmu.viewmodel.YuhmuViewModel

@Composable
fun GamesScreen(
    vm: YuhmuViewModel,
    onPick: (String) -> Unit,
    onOpenDaily: () -> Unit,
    onOpenStory: (String) -> Unit,
    onOpenNumbers: () -> Unit,
    onOpenAudioLibrary: () -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val progress by vm.progress.collectAsState()
    TabScaffold(progress, bottomBar) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Mascot(size = 70.dp, mood = Mood.CHEER, character = progress.avatarCharacter())
                Column {
                    Text("¡Hora de jugar!", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple)
                    Text("Practica, descubre y gana puntos", fontSize = 14.sp, color = Brand.gray)
                }
            }
            GameCard(Icons.Filled.GridView, "Memorama", "Encuentra las parejas Yuhmu – Español", Brand.purpleMid, Brand.purpleMidDark) { onPick("memorama") }
            GameCard(Icons.Filled.Mic, "Escucha y pronuncia", "Practica con las grabaciones a tu ritmo", Color(0xFF1FA89A), Color(0xFF17847A)) { onPick("speak") }
            GameCard(Icons.Filled.LibraryMusic, "Biblioteca de audios", "Explora las grabaciones por tema y escúchalas", Brand.purple, Brand.purpleDark, onOpenAudioLibrary)
            GameCard(Icons.Filled.FlashOn, "Retos de vocabulario", "Cinco niveles de traducción, contexto y escucha", Color(0xFFB0368F), Color(0xFF8A2670)) { onPick("challenge") }

            Text("Otras actividades", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple)
            DailyChallengeCard(done = progress.dailyDone(), onClick = onOpenDaily)
            GameCard(Icons.Filled.Calculate, "Números en Yhụmụ", "Aprende a contar y avanza por sus niveles", Brand.orange, Color(0xFFD97A00), onOpenNumbers)
            StoriesRow(progress.storiesDone, onOpenStory)
        }
    }
}

@Composable
private fun GameCard(icon: ImageVector, title: String, subtitle: String, face: Color, edge: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().chunky(face, edge, radius = 22.dp, depth = 6.dp).clickable(onClick = onClick).padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(34.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(subtitle, fontSize = 13.sp, color = Color.White.copy(alpha = 0.9f))
        }
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White)
    }
}

/** Elegir categoría (o "Todos los temas") antes de jugar. */
@Composable
fun GameSetupScreen(kind: String, vm: YuhmuViewModel, onBack: () -> Unit, onPick: (String) -> Unit) {
    YuhmuScaffold(when (kind) { "memorama" -> "Memorama"; "speak" -> "Repite la palabra"; else -> "Retos de vocabulario" }, onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Elige un tema", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Brand.ink)
            SetupRow(Icons.Filled.AutoAwesome, "Todos los temas", Brand.gold) { onPick("all") }
            vm.content.categories.forEach { c ->
                SetupRow(categoryIcon(c.icon), c.title, Brand.purpleMid) { onPick(c.id) }
            }
        }
    }
}

@Composable
private fun SetupRow(icon: ImageVector, title: String, tint: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).clickable(onClick = onClick).card(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(44.dp).clip(CircleShape).background(tint), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Text(title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Brand.ink, modifier = Modifier.weight(1f))
        Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Brand.grayEdge)
    }
}

/** Juego concreto: "memorama" o "challenge", sobre una categoría o "all". */
@Composable
fun GameScreen(kind: String, category: String, vm: YuhmuViewModel, onBack: () -> Unit) {
    val words: List<Word> = remember(category) {
        if (category == "all") vm.content.allWords
        else vm.content.categories.first { it.id == category }.words
    }

    when (kind) {
        "speak" -> {
            SpeakGame(words, vm, onClose = onBack)
        }
        "memorama" -> {
            YuhmuScaffold("Memorama", onBack = onBack) { padding ->
                MemoramaScreen(words, vm, onClose = onBack, modifier = Modifier.padding(padding).fillMaxSize())
            }
        }
        else -> {
            val questions = remember(words) {
                vm.content.makeLevelledQuestions(words, levelCount = 5, questionsPerLevel = 3)
            }
            FullScreen {
                ChallengeScreen(
                    questions = questions,
                    audio = vm.audio,
                    showLevels = true,
                    questionsPerLevel = 3,
                    onComplete = { vm.recordChallenge(it) },
                    onClose = onBack
                )
            }
        }
    }
}



/** Biblioteca de grabaciones; se accede desde Juegos, no desde el diccionario. */
@Composable
fun AudioLibraryScreen(vm: YuhmuViewModel, onBack: () -> Unit) {
    var query by remember { mutableStateOf("") }
    val recordings = remember { vm.audio.getCatalog() }
    val filtered = remember(recordings, query) {
        if (query.isBlank()) recordings
        else recordings.filter {
            it.title.contains(query, ignoreCase = true) ||
            (!it.yuhmu.isNullOrBlank() && it.yuhmu.contains(query, ignoreCase = true)) ||
            it.section.contains(query, ignoreCase = true)
        }
    }

    YuhmuScaffold("Biblioteca de audios", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = Brand.purple) },
                placeholder = { Text("Busca una palabra o categoría") }
            )
            Text(
                "${filtered.size} grabaciones · disponibles sin conexión",
                Modifier.padding(horizontal = 18.dp),
                fontSize = 12.sp,
                color = Brand.gray
            )
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                filtered.groupBy { it.section }.forEach { (section, itemsInSection) ->
                    item(key = "audio_section_$section") {
                        Text(
                            section,
                            Modifier.padding(start = 4.dp, top = 8.dp),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Brand.purple
                        )
                    }
                    items(itemsInSection, key = { it.path }) { recording ->
                        AudioRecordingRow(recording, vm)
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioRecordingRow(recording: AudioCatalogItem, vm: YuhmuViewModel) {
    Row(
        Modifier.fillMaxWidth().card(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(Brand.lavender),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Filled.GraphicEq, contentDescription = null, tint = Brand.purple, modifier = Modifier.size(26.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(recording.title, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Brand.ink)
            if (!recording.yuhmu.isNullOrBlank()) {
                Text(recording.yuhmu, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Brand.purple)
            }
            if (recording.isAlternative) Text("Grabación alternativa", fontSize = 11.sp, color = Brand.gray)
            recording.note?.let { Text(it, fontSize = 11.sp, lineHeight = 15.sp, color = Brand.gray) }
        }
        IconButton(onClick = { vm.audio.play(recording.path) }) {
            Icon(Icons.AutoMirrored.Filled.VolumeUp, contentDescription = "Reproducir ${recording.title}", tint = Brand.purple)
        }
    }
}

/** Recorrido de números trasladado desde Inicio a Juegos. */
@Composable
fun NumberLessonsScreen(
    vm: YuhmuViewModel,
    onBack: () -> Unit,
    onOpenLesson: (String) -> Unit
) {
    val progress by vm.progress.collectAsState()
    val category = vm.content.categories.firstOrNull { it.id == "numeros" }
    val lessons = category?.let { vm.content.lessonsIn(it) }.orEmpty()
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }
    val completed = progress.completedLessons
    val skipped = progress.skippedLessons
    val unlocked = buildSet {
        lessons.forEachIndexed { index, lesson ->
            val previousDone = index == 0 || lessons[index - 1].id in completed || lessons[index - 1].id in skipped
            if (previousDone) add(lesson.id)
        }
    }

    YuhmuScaffold("Números en Yhụmụ", onBack = onBack) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NumberSystemCard()
            Text("Tu recorrido de números", fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple)
            Text("Cada unidad tiene palabras nuevas y un pequeño reto para practicar.", fontSize = 13.sp, color = Brand.gray)
            lessons.forEachIndexed { index, lesson ->
                val isDone = lesson.id in completed
                val isSkipped = lesson.id in skipped
                val isUnlocked = lesson.id in unlocked
                Row(
                    Modifier.fillMaxWidth().card().clickable(enabled = isUnlocked || isDone || isSkipped) { selectedLesson = lesson },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(42.dp).clip(CircleShape).background(
                            when {
                                isDone -> Brand.gold
                                isSkipped -> Brand.lavender
                                isUnlocked -> Brand.purpleMid
                                else -> Brand.grayLight
                            }
                        ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            when {
                                isDone -> Icons.Filled.Check
                                isSkipped -> Icons.Filled.SkipNext
                                isUnlocked -> Icons.Filled.PlayArrow
                                else -> Icons.Filled.Lock
                            },
                            contentDescription = null,
                            tint = when { isSkipped -> Brand.purple; !isUnlocked -> Brand.gray; else -> Color.White }
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Nivel ${index + 1}", fontSize = 11.sp, color = Brand.gray, fontWeight = FontWeight.Bold)
                        Text("${lesson.words.first().spanish} – ${lesson.words.last().spanish}", fontSize = 15.sp, color = Brand.ink, fontWeight = FontWeight.ExtraBold)
                        Text(
                            when { isDone -> "Completado"; isSkipped -> "Saltado · puedes practicarlo"; isUnlocked -> "Disponible"; else -> "Completa o salta el nivel anterior" },
                            fontSize = 11.sp, color = Brand.gray
                        )
                    }
                    Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Brand.gray.copy(alpha = 0.6f))
                }
            }
        }
    }

    selectedLesson?.let { lesson ->
        val alreadyCompleted = lesson.id in completed
        val alreadySkipped = lesson.id in skipped
        val canSkip = lesson.id in unlocked && !alreadyCompleted && !alreadySkipped
        AlertDialog(
            onDismissRequest = { selectedLesson = null },
            title = { Text(lesson.title, fontWeight = FontWeight.ExtraBold, color = Brand.purple) },
            text = { Text("Puedes comenzar esta unidad o saltarla para desbloquear el siguiente nivel.", color = Brand.ink) },
            confirmButton = {
                TextButton(onClick = { selectedLesson = null; onOpenLesson(lesson.id) }) {
                    Text(if (alreadyCompleted || alreadySkipped) "Practicar" else "Comenzar", color = Brand.purple, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                Row {
                    if (canSkip) {
                        TextButton(onClick = { vm.skipLesson(lesson); selectedLesson = null }) {
                            Text("Saltar nivel", color = Brand.orange, fontWeight = FontWeight.Bold)
                        }
                    }
                    TextButton(onClick = { selectedLesson = null }) { Text("Ahora no", color = Brand.gray) }
                }
            }
        )
    }
}
