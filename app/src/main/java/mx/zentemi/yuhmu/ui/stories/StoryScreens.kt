package mx.zentemi.yuhmu.ui.stories

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.zentemi.yuhmu.data.STORIES
import mx.zentemi.yuhmu.data.Story
import mx.zentemi.yuhmu.model.ChallengeResult
import mx.zentemi.yuhmu.model.Word
import mx.zentemi.yuhmu.ui.components.*
import mx.zentemi.yuhmu.ui.theme.Brand
import mx.zentemi.yuhmu.viewmodel.YuhmuViewModel

private val TOKEN = Regex("\\{([a-z]+_\\d+)\\}")

/** Colores (principal, oscuro) de cada historia. */
val STORY_COLORS = listOf(
    Color(0xFF58A83A) to Color(0xFF3F8A2B),
    Color(0xFFE08A1E) to Color(0xFFB86C10),
    Color(0xFF1FA89A) to Color(0xFF17847A),
    Color(0xFFB0368F) to Color(0xFF8A2670),
    Color(0xFF4C6FFF) to Color(0xFF3550C9)
)

/** Carrusel de historias para el inicio. */
@Composable
fun StoriesRow(done: Set<String>, onOpen: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text("Historias de Yuhmu", modifier = Modifier.weight(1f), fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Brand.ink)
            Icon(Icons.Filled.ChevronRight, contentDescription = "Desliza para ver más historias", tint = Brand.gray.copy(alpha = 0.45f), modifier = Modifier.size(18.dp))
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(bottom = 6.dp)) {
            items(STORIES, key = { it.id }) { s ->
                val (c1, c2) = STORY_COLORS[s.colorIndex % STORY_COLORS.size]
                Column(
                    Modifier.width(150.dp).clip(RoundedCornerShape(22.dp))
                        .background(Brush.linearGradient(listOf(c1, c2)))
                        .clickable { onOpen(s.id) }.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(Modifier.size(54.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.25f)), contentAlignment = Alignment.Center) {
                        Text(s.emoji, fontSize = 30.sp)
                    }
                    Text(s.title, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, minLines = 2)
                    Text(
                        if (s.id in done) "✔ Leída" else "${s.scenes.size} escenas",
                        color = Color.White.copy(alpha = 0.9f), fontSize = 12.sp, fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun StoryScreen(storyId: String, vm: YuhmuViewModel, onBack: () -> Unit) {
    val story: Story = remember(storyId) { STORIES.first { it.id == storyId } }
    val (c1, c2) = STORY_COLORS[story.colorIndex % STORY_COLORS.size]
    val lookup = vm.content.wordById

    var step by remember { mutableIntStateOf(0) }
    var mistakes by remember { mutableIntStateOf(0) }
    var result by remember { mutableStateOf<ChallengeResult?>(null) }

    FullScreen {
        val done = result
        if (done != null) {
            ResultContent(done, onBack)
            return@FullScreen
        }
        val scene = story.scenes[step]
        var picked by remember(step) { mutableStateOf<String?>(null) }
        var answered by remember(step) { mutableStateOf(false) }
        var shown by remember(step) { mutableStateOf(false) }
        LaunchedEffect(step) { shown = true }

        val options: List<Word> = remember(step) {
            (listOf(scene.ask) + scene.wrong).mapNotNull { lookup[it] }.shuffled()
        }
        val target = lookup[scene.ask]
        val inScene = remember(step) { TOKEN.findAll(scene.text).mapNotNull { lookup[it.groupValues[1]] }.toList() }

        Column(Modifier.fillMaxSize()) {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(onClick = onBack) { Icon(Icons.Filled.Close, contentDescription = "Salir", tint = Brand.gray) }
                ChunkyProgress((step + if (answered) 1 else 0).toFloat() / story.scenes.size, Modifier.weight(1f), color = c1)
                Text("${step + 1}/${story.scenes.size}", Modifier.padding(end = 8.dp), fontWeight = FontWeight.ExtraBold, color = Brand.gray)
            }
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(story.title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = c2)
                AnimatedVisibility(shown, enter = fadeIn(tween(500)) + slideInVertically(tween(500)) { it / 4 }) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        // Ilustración de la escena
                        Box(
                            Modifier.fillMaxWidth().height(150.dp).clip(RoundedCornerShape(28.dp))
                                .background(Brush.linearGradient(listOf(c1, c2))),
                            contentAlignment = Alignment.Center
                        ) { Text(scene.emoji, fontSize = 84.sp) }
                        // Narración con la mascota
                        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Mascot(size = 64.dp, mood = if (answered) Mood.CHEER else Mood.HAPPY)
                            Text(
                                buildAnnotatedString {
                                    var last = 0
                                    for (m in TOKEN.findAll(scene.text)) {
                                        append(scene.text.substring(last, m.range.first))
                                        withStyle(SpanStyle(color = c2, fontWeight = FontWeight.ExtraBold, background = c1.copy(alpha = 0.15f))) {
                                            append(" ${lookup[m.groupValues[1]]?.yuhmu.orEmpty()} ")
                                        }
                                        last = m.range.last + 1
                                    }
                                    append(scene.text.substring(last))
                                },
                                Modifier.weight(1f).background(Color.White, RoundedCornerShape(18.dp))
                                    .border(2.dp, c1.copy(alpha = 0.4f), RoundedCornerShape(18.dp)).padding(14.dp),
                                fontSize = 18.sp, lineHeight = 26.sp, color = Brand.ink, fontWeight = FontWeight.SemiBold
                            )
                        }
                        // Palabras tocables (audio + significado)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            inScene.forEach { w ->
                                Row(
                                    Modifier.clip(RoundedCornerShape(50)).background(Brand.lavender)
                                        .clickable { vm.audio.play(w.id) }.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Filled.VolumeUp, null, tint = Brand.purple, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("${w.yuhmu} = ${w.spanish}", color = Brand.purple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
                // Pregunta de la escena
                Text("¿Cómo se dice «${target?.spanish.orEmpty()}» en Yuhmu?", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Brand.ink, textAlign = TextAlign.Center)
                options.forEach { w ->
                    val st = when {
                        picked == w.id && w.id == scene.ask -> OptionState.CORRECT
                        picked == w.id -> OptionState.WRONG
                        answered -> OptionState.DIM
                        else -> OptionState.IDLE
                    }
                    OptionCard(w.yuhmu, st, enabled = !answered) {
                        picked = w.id
                        if (w.id == scene.ask) { answered = true; vm.audio.play(w.id) } else mistakes++
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
            Column(Modifier.fillMaxWidth().background(if (answered) Brand.greenLight else Color.White).padding(20.dp, 14.dp)) {
                if (answered) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Brand.greenDark, modifier = Modifier.size(30.dp))
                        Text("¡Muy bien!", fontWeight = FontWeight.ExtraBold, fontSize = 20.sp, color = Brand.greenDark)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                ChunkyButton(
                    if (step == story.scenes.size - 1) "Terminar historia" else "Continuar",
                    enabled = answered
                ) {
                    if (step == story.scenes.size - 1) {
                        val n = story.scenes.size
                        val pts = (30 + 10 * n - 5 * mistakes).coerceAtLeast(20)
                        val stars = when { mistakes == 0 -> 3; mistakes <= 2 -> 2; else -> 1 }
                        vm.completeStory(story.id, pts)
                        result = ChallengeResult(n, n + mistakes, pts, 3, 0, starsOverride = stars)
                    } else step++
                }
            }
        }
    }
}
