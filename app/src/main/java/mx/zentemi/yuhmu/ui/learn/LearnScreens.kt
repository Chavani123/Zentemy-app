package mx.zentemi.yuhmu.ui.learn

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.zentemi.yuhmu.data.*
import mx.zentemi.yuhmu.model.Lesson
import mx.zentemi.yuhmu.model.Question
import mx.zentemi.yuhmu.ui.challenge.ChallengeScreen
import mx.zentemi.yuhmu.ui.components.*
import mx.zentemi.yuhmu.ui.theme.Brand
import mx.zentemi.yuhmu.ui.theme.YuhmuTheme
import mx.zentemi.yuhmu.viewmodel.YuhmuViewModel

private enum class NodeState { LOCKED, CURRENT, DONE, SKIPPED }

/** (color de la sección, color del borde 3D) */
private val SECTION_COLORS = listOf(
    Brand.purple to Brand.purpleDark,
    Color(0xFFB0368F) to Color(0xFF8A2670),
    Color(0xFF1FA89A) to Color(0xFF17847A),
    Color(0xFFE08A1E) to Color(0xFFB86C10),
    Brand.purpleMid to Brand.purpleMidDark,
    Color(0xFF4C6FFF) to Color(0xFF3550C9),
    Color(0xFFE5484D) to Color(0xFFB8353A)
)

private val ZIGZAG = intArrayOf(0, 44, 70, 44, 0, -44, -70, -44)

@Composable
fun HomeScreen(
    vm: YuhmuViewModel,
    onOpenLesson: (String) -> Unit,
    bottomBar: @Composable () -> Unit
) {
    val progress by vm.progress.collectAsState()
    val categories = vm.content.categories.filterNot { it.id == "numeros" }
    val homeLessons = categories.flatMap { vm.content.lessonsIn(it) }
    val completed = progress.completedLessons
    val skipped = progress.skippedLessons
    var selectedLesson by remember { mutableStateOf<Lesson?>(null) }

    // La siguiente lección se desbloquea al cursar o saltar la anterior.
    val unlocked = buildSet {
        homeLessons.forEachIndexed { i, lesson ->
            val previousDone = i == 0 || homeLessons[i - 1].id in completed || homeLessons[i - 1].id in skipped
            if (previousDone) add(lesson.id)
        }
    }

    TabScaffold(progress, bottomBar) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp)
        ) {
            item(key = "greeting") {
                Greeting(progress)
                Spacer(Modifier.height(20.dp))
                Text(
                    "Tu camino de aprendizaje",
                    Modifier.fillMaxWidth(),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Brand.purple
                )
                Text(
                    "Avanza a tu ritmo. Puedes comenzar una lección o saltarla para continuar con la siguiente.",
                    Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 14.dp),
                    fontSize = 13.sp,
                    lineHeight = 19.sp,
                    color = Brand.gray
                )
            }
            categories.forEachIndexed { ci, category ->
                val (color, edge) = SECTION_COLORS[ci % SECTION_COLORS.size]
                item(key = "header_${category.id}") {
                    UnitHeader(ci + 1, category.title, category.words.size, categoryIcon(category.icon), color, edge)
                }
                itemsIndexed(vm.content.lessonsIn(category), key = { _, lesson -> lesson.id }) { i, lesson ->
                    val state = when (lesson.id) {
                        in completed -> NodeState.DONE
                        in skipped -> NodeState.SKIPPED
                        in unlocked -> NodeState.CURRENT
                        else -> NodeState.LOCKED
                    }
                    PathNode(i, state, completed[lesson.id] ?: 0, color, edge) {
                        selectedLesson = lesson
                    }
                }
                item(key = "gap_${category.id}") { Spacer(Modifier.height(18.dp)) }
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
            text = {
                Text(
                    "${lesson.words.size} palabras para aprender. Puedes entrar y practicar ahora, o saltar esta unidad para desbloquear la siguiente.",
                    color = Brand.ink
                )
            },
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
                    TextButton(onClick = { selectedLesson = null }) {
                        Text("Ahora no", color = Brand.gray)
                    }
                }
            }
        )
    }
}

@Composable
fun NumberSystemCard() {
    Column(Modifier.fillMaxWidth().card(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(34.dp).clip(CircleShape).background(Brand.lavender), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Calculate, contentDescription = null, tint = Brand.purple)
            }
            Text("¿Cómo contamos en Yhụmụ?", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple)
        }
        Text(
            "El sistema numérico usa una base vigesimal: el número 20, llamado ’Róte. Los números mayores se forman combinando veintes y unidades.",
            fontSize = 13.sp, lineHeight = 19.sp, color = Brand.ink
        )
        Text("20 = ’Róte     ·     40 = 2 × 20     ·     60 = 3 × 20     ·     80 = 4 × 20", fontSize = 12.sp, lineHeight = 18.sp, color = Brand.purple, fontWeight = FontWeight.Bold)
        Text("Del 11 al 19 y del 21 al 39 se usa «ma» para sumar unidades. Del 41 al 49 se añade la unidad a la base de 40 sin ese conector.", fontSize = 12.sp, lineHeight = 18.sp, color = Brand.ink)
        Text(
            "El 50 (Ngú̱ pho) y el 100 tienen formas independientes en el material. Para cien, el vocabulario conserva las dos formas registradas.",
            fontSize = 12.sp, lineHeight = 18.sp, color = Brand.gray
        )
    }
}



@Composable
private fun Greeting(progress: UserProgress) {
    Row(
        Modifier.fillMaxWidth()
            .background(Brush.linearGradient(listOf(Brand.purple, Color(0xFFB0368F))), RoundedCornerShape(26.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Mascot(size = 76.dp, mood = Mood.CHEER, character = progress.avatarCharacter())
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text("¡Hola, ${progress.name}!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Nivel ${progress.level()} · ${progress.levelTitle()}", fontSize = 13.sp, color = Brand.lavender, fontWeight = FontWeight.SemiBold)
            ChunkyProgress(progress.levelProgress(), color = Brand.gold, height = 10.dp)
        }
    }
}

@Composable
private fun UnitHeader(number: Int, title: String, words: Int, icon: ImageVector, color: Color, edge: Color) {
    Box(Modifier.fillMaxWidth().chunky(color, edge, radius = 22.dp).clip(RoundedCornerShape(22.dp))) {
    androidx.compose.foundation.Canvas(Modifier.matchParentSize()) {
        drawCircle(Color.White.copy(alpha = 0.10f), 90.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * 0.9f, 0f))
        drawCircle(Color.White.copy(alpha = 0.08f), 50.dp.toPx(), androidx.compose.ui.geometry.Offset(size.width * 0.62f, size.height))
    }
    Row(Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("SECCIÓN $number · $words PALABRAS", fontSize = 11.sp, fontWeight = FontWeight.ExtraBold, color = Color.White.copy(alpha = 0.8f), letterSpacing = 0.8.sp)
            Text(title, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
        }
        Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.2f)), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
        }
    }
    }
}

@Composable
private fun PathNode(index: Int, state: NodeState, stars: Int, color: Color, edge: Color, onClick: () -> Unit) {
    Column(
        Modifier.offset(x = ZIGZAG[index % ZIGZAG.size].dp).padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (state == NodeState.CURRENT) StartBubble()
        val face = when (state) {
            NodeState.LOCKED -> Brand.grayLight
            NodeState.CURRENT -> color
            NodeState.DONE -> Brand.gold
            NodeState.SKIPPED -> Brand.lavender
        }
        val rim = when (state) {
            NodeState.LOCKED -> Brand.grayEdge
            NodeState.CURRENT -> edge
            NodeState.DONE -> Brand.goldDark
            NodeState.SKIPPED -> Brand.purpleMid
        }
        val icon = when (state) {
            NodeState.LOCKED -> Icons.Filled.Lock
            NodeState.CURRENT -> Icons.Filled.PlayArrow
            NodeState.DONE -> Icons.Filled.Check
            NodeState.SKIPPED -> Icons.Filled.SkipNext
        }
        ChunkyCircle(size = 76.dp, face = face, rim = rim, enabled = state != NodeState.LOCKED, onClick = onClick) {
            Icon(
                icon,
                contentDescription = when (state) {
                    NodeState.LOCKED -> "Lección bloqueada"
                    NodeState.CURRENT -> "Abrir o saltar lección ${index + 1}"
                    NodeState.DONE -> "Repasar lección ${index + 1}"
                    NodeState.SKIPPED -> "Repasar lección omitida ${index + 1}"
                },
                tint = if (state == NodeState.LOCKED) Brand.gray else if (state == NodeState.SKIPPED) Brand.purple else Color.White,
                modifier = Modifier.size(38.dp)
            )
        }
        if (state == NodeState.DONE && stars > 0) {
            Row(Modifier.padding(top = 4.dp)) {
                repeat(3) { i ->
                    Icon(
                        if (i < stars) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = null, tint = Brand.gold, modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else if (state == NodeState.SKIPPED) {
            Text("Saltado", Modifier.padding(top = 3.dp), fontSize = 10.sp, color = Brand.gray, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun StartBubble() {
    val transition = rememberInfiniteTransition(label = "bubble")
    val dy by transition.animateFloat(
        initialValue = 0f, targetValue = 6f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "dy"
    )
    Text(
        "¡EMPIEZA!",
        modifier = Modifier.offset(y = dy.dp)
            .background(Color.White, RoundedCornerShape(12.dp))
            .border(2.dp, Brand.lavender, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 6.dp),
        color = Brand.purple, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp
    )
    Spacer(Modifier.height(10.dp))
}

@Composable
fun DailyChallengeCard(done: Boolean, onClick: () -> Unit) {
    val face = if (done) Brand.green else Brand.orange
    val edge = if (done) Brand.greenDark else Color(0xFFD97A00)
    Row(
        Modifier.fillMaxWidth().chunky(face, edge).clickable(enabled = !done, onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(48.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
            Icon(if (done) Icons.Filled.Verified else Icons.Filled.CalendarMonth, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Column(Modifier.weight(1f)) {
            Text("Reto del día", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(
                if (done) "¡Completado! Vuelve mañana" else "5 palabras en frases cotidianas · +50 puntos",
                fontSize = 13.sp, color = Color.White.copy(alpha = 0.92f)
            )
        }
        if (!done) Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = Color.White)
    }
}

@Composable
fun DailyScreen(vm: YuhmuViewModel, onBack: () -> Unit) {
    val questions: List<Question> = remember { vm.content.dailyQuestions(dayNumber()) }
    FullScreen {
        ChallengeScreen(
            questions = questions,
            audio = vm.audio,
            onComplete = { vm.completeDaily(it) },
            onClose = onBack
        )
    }
}

@Composable
fun LessonScreen(lessonId: String, vm: YuhmuViewModel, onBack: () -> Unit) {
    val lesson = vm.content.lessons.first { it.id == lessonId }
    var index by remember(lessonId) { mutableIntStateOf(0) }
    var quiz by remember(lessonId) { mutableStateOf<List<Question>?>(null) }

    FullScreen {
        val questions = quiz
        if (questions != null) {
            ChallengeScreen(
                questions = questions,
                audio = vm.audio,
                allowAudioPlayback = false,
                onComplete = { vm.completeLesson(lesson, it) },
                onClose = onBack
            )
        } else {
            StudyPhase(
                lesson = lesson,
                index = index,
                vm = vm,
                onClose = onBack,
                onNext = {
                    if (index == lesson.words.lastIndex) {
                        quiz = vm.content.makeLessonQuestions(lesson.words, minOf(8, lesson.words.size))
                    } else index++
                },
                onPrev = { if (index > 0) index-- }
            )
        }
    }
}

/**
 * Cada palabra se presenta en contexto y después se practica con una pregunta de comprensión.
 * Las actividades de escucha y pronunciación se encuentran en la sección Juegos.
 */
@Composable
private fun StudyPhase(
    lesson: Lesson,
    index: Int,
    vm: YuhmuViewModel,
    onClose: () -> Unit,
    onNext: () -> Unit,
    onPrev: () -> Unit
) {
    val word = lesson.words[index]
    val isLast = index == lesson.words.lastIndex
    var practiceMode by remember(word.id) { mutableStateOf(false) }
    var selected by remember(word.id) { mutableStateOf<String?>(null) }
    var checked by remember(word.id) { mutableStateOf(false) }

    val options = remember(word.id) {
        val answer = word.spanish
        val alternatives = vm.content.allWords
            .asSequence()
            .filter { it.id != word.id && it.categoryId == word.categoryId }
            .map { it.spanish.trim() }
            .filter { it.isNotBlank() && !it.equals(answer, ignoreCase = true) }
            .distinctBy { it.lowercase() }
            .shuffled()
            .take(3)
            .toMutableList()
        if (alternatives.size < 3) {
            vm.content.allWords.asSequence()
                .filter { it.id != word.id }
                .map { it.spanish.trim() }
                .filter { it.isNotBlank() && !it.equals(answer, ignoreCase = true) }
                .distinctBy { it.lowercase() }
                .shuffled()
                .forEach { candidate ->
                    if (alternatives.size < 3 && alternatives.none { it.equals(candidate, true) }) alternatives.add(candidate)
                }
        }
        (listOf(answer) + alternatives).shuffled()
    }
    val correct = checked && selected == word.spanish
    val contextHint = word.context
        ?.replace("«${word.yuhmu}»", "________")
        ?.replace("«${word.spanish}»", "________")
        ?: "Piensa en la situación y relaciona la palabra con su significado."

    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            IconButton(onClick = if (practiceMode) { { practiceMode = false; selected = null; checked = false } } else onClose) {
                Icon(Icons.Filled.Close, contentDescription = if (practiceMode) "Volver a la palabra" else "Cerrar", tint = Brand.gray)
            }
            Column(Modifier.weight(1f)) {
                Text(lesson.title, fontWeight = FontWeight.ExtraBold, color = Brand.purple, fontSize = 14.sp)
                Text(if (practiceMode) "Comprueba lo aprendido" else "Conoce una palabra", color = Brand.gray, fontSize = 12.sp)
            }
            ChunkyProgress((index + if (practiceMode && correct) 1f else 0f) / lesson.words.size, Modifier.width(100.dp))
            Text("${index + 1}/${lesson.words.size}", fontWeight = FontWeight.ExtraBold, color = Brand.gray)
        }

        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (!practiceMode) {
                Spacer(Modifier.height(4.dp))
                Text("¡Vamos a aprender!", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple)
                Mascot(size = 76.dp, mood = Mood.HAPPY, character = vm.progress.collectAsState().value.avatarCharacter())
                Column(
                    Modifier.fillMaxWidth().card(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    LearningIllustration(word, size = 112.dp)
                    Text(word.yuhmu, fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple, textAlign = TextAlign.Center)
                    Text(word.spanish, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Brand.ink, textAlign = TextAlign.Center)
                    word.note?.takeIf { it.isNotBlank() }?.let { TextCenter(it, size = 13) }
                    word.context?.takeIf { it.isNotBlank() }?.let {
                        Column(
                            Modifier.fillMaxWidth().background(Brand.lavender.copy(alpha = 0.55f), RoundedCornerShape(16.dp)).padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text("EN UNA SITUACIÓN", fontSize = 10.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purpleMid, letterSpacing = 0.8.sp)
                            Text(it, fontSize = 14.sp, lineHeight = 20.sp, color = Brand.ink, textAlign = TextAlign.Center)
                        }
                    }
                    Text(
                        "La escucha y la pronunciación se practican en Juegos, donde encontrarás las grabaciones por tema.",
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        color = Brand.gray,
                        textAlign = TextAlign.Center
                    )
                }
                Text(
                    "Primero observa la palabra y su ejemplo. Después elegirás su significado.",
                    fontSize = 14.sp, lineHeight = 20.sp, color = Brand.gray, textAlign = TextAlign.Center
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Text("¿Qué significa esta palabra?", fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = Brand.ink, textAlign = TextAlign.Center)
                Mascot(size = 76.dp, mood = if (correct) Mood.CHEER else Mood.HAPPY, character = vm.progress.collectAsState().value.avatarCharacter())
                Column(Modifier.fillMaxWidth().card(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(word.yuhmu, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                    Text("Lee la pista para descubrir qué quiere decir.", fontSize = 13.sp, color = Brand.gray, textAlign = TextAlign.Center)
                    Text(contextHint, fontSize = 15.sp, lineHeight = 21.sp, color = Brand.ink, textAlign = TextAlign.Center)
                }
                options.forEach { option ->
                    val state = when {
                        !checked && selected == option -> OptionState.SELECTED
                        !checked -> OptionState.IDLE
                        option == word.spanish -> OptionState.CORRECT
                        option == selected -> OptionState.WRONG
                        else -> OptionState.DIM
                    }
                    OptionCard(option, state, enabled = !checked) { selected = option }
                }
                if (checked) {
                    Text(
                        if (correct) "¡Muy bien! Ya entendiste esta palabra." else "Casi. Lee otra vez la pista y vuelve a intentarlo.",
                        color = if (correct) Brand.greenDark else Brand.orange,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (!practiceMode) {
            ChunkyButton("Practicar esta palabra", onClick = { practiceMode = true })
        } else if (!checked) {
            ChunkyButton("Comprobar", enabled = selected != null, onClick = { checked = true })
        } else if (!correct) {
            ChunkyButton("Intentar otra vez", color = Brand.orange, edge = Color(0xFFD97A00), onClick = {
                selected = null
                checked = false
            })
        } else {
            ChunkyButton(if (isLast) "Ir al reto final" else "Siguiente palabra", onClick = onNext)
        }
        if (index > 0 && !practiceMode) {
            TextButton(onClick = onPrev, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text("Volver a la palabra anterior", color = Brand.gray)
            }
        }
    }
}

@Preview(name = "1. Teléfono Portrait", showBackground = true, widthDp = 360, heightDp = 740)
@Preview(name = "2. Tablet / Pantalla Grande", showBackground = true, widthDp = 768, heightDp = 1024)
@Preview(name = "3. Teléfono Landscape", showBackground = true, widthDp = 740, heightDp = 360)
@Composable
fun LearnScreenAdaptabilityPreview() {
    YuhmuTheme {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Brand.bg)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Greeting(progress = UserProgress(name = "Explorador Yhụmụ", points = 350, streak = 5))
            DailyChallengeCard(done = false, onClick = {})
            UnitHeader(
                number = 1,
                title = "Saludos y Frases",
                words = 12,
                icon = Icons.Filled.AutoAwesome,
                color = Brand.purple,
                edge = Brand.purpleDark
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                PathNode(index = 0, state = NodeState.DONE, stars = 3, color = Brand.purple, edge = Brand.purpleDark, onClick = {})
                PathNode(index = 1, state = NodeState.CURRENT, stars = 0, color = Brand.purple, edge = Brand.purpleDark, onClick = {})
                PathNode(index = 2, state = NodeState.LOCKED, stars = 0, color = Brand.purple, edge = Brand.purpleDark, onClick = {})
            }
            NumberSystemCard()
        }
    }
}

@Preview(name = "Tarjetas de Reto Diario", showBackground = true)
@Composable
fun DailyChallengeCardPreview() {
    YuhmuTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            DailyChallengeCard(done = false, onClick = {})
            DailyChallengeCard(done = true, onClick = {})
        }
    }
}
