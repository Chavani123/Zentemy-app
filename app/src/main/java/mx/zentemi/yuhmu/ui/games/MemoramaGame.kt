package mx.zentemi.yuhmu.ui.games

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mx.zentemi.yuhmu.model.ChallengeResult
import mx.zentemi.yuhmu.model.Word
import mx.zentemi.yuhmu.ui.components.*
import mx.zentemi.yuhmu.ui.theme.Brand
import mx.zentemi.yuhmu.viewmodel.YuhmuViewModel
import kotlin.math.max

private val PAIR_COLORS = listOf(
    Color(0xFFFF6B9D), Color(0xFF29B6F6), Color(0xFFFFB703), Color(0xFF58CC02),
    Color(0xFFB794F4), Color(0xFFFF8E3C), Color(0xFF1FA89A), Color(0xFFEF476F)
)

private data class MemoCard(
    val id: Int = 0,
    val pairId: String,
    val text: String,
    val isYuhmu: Boolean,
    val color: Color,
    val faceUp: Boolean = false,
    val matched: Boolean = false,
    val bounce: Boolean = false
)

/** Memorama con cinco dificultades, selección aleatoria de parejas y recompensas. */
@Composable
fun MemoramaScreen(words: List<Word>, vm: YuhmuViewModel, onClose: () -> Unit, modifier: Modifier = Modifier) {
    var pairs by remember { mutableStateOf<Int?>(null) }
    val availablePairs = words.size
    val predefinedLevels = listOf(
        Triple(4, "Explorador", "4 parejas · para comenzar"),
        Triple(6, "Aventurero", "6 parejas · memoria en marcha"),
        Triple(8, "Estratega", "8 parejas · pon atención"),
        Triple(10, "Maestro", "10 parejas · desafío avanzado"),
        Triple(12, "Guardián", "12 parejas · ¡nivel máximo!")
    )
    val levels = predefinedLevels.filter { it.first <= availablePairs }
        .ifEmpty {
            if (availablePairs > 0) listOf(Triple(availablePairs, "Ronda rápida", "$availablePairs parejas disponibles"))
            else emptyList()
        }

    val selectedPairs = pairs
    if (selectedPairs == null) {
        if (levels.isEmpty()) {
            Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Todavía no hay palabras para jugar en este tema.", color = Brand.gray, textAlign = TextAlign.Center)
            }
        } else {
            DifficultyPicker(modifier, levels) { pairs = it }
        }
        return
    }
    Board(words, selectedPairs, vm, onClose, modifier)
}

@Composable
private fun DifficultyPicker(modifier: Modifier, levels: List<Triple<Int, String, String>>, onPick: (Int) -> Unit) {
    val levelColors = listOf(
        Color(0xFF58CC02) to Color(0xFF46A302),
        Color(0xFF29B6F6) to Color(0xFF1687C2),
        Color(0xFFFFB703) to Color(0xFFD99100),
        Color(0xFFFF8E3C) to Color(0xFFD96C20),
        Color(0xFFFF6B9D) to Color(0xFFD94F7C)
    )
    Column(
        modifier.fillMaxSize().padding(20.dp).verticalScroll(androidx.compose.foundation.rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Mascot(size = 82.dp, mood = Mood.CHEER)
        Text("¡Elige tu nivel!", fontSize = 25.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple)
        Text(
            "Encuentra las parejas Yuhmu–Español. Las palabras cambian en cada partida.",
            fontSize = 14.sp, color = Brand.gray, textAlign = TextAlign.Center
        )
        levels.forEachIndexed { index, level ->
            val colorPair = levelColors[index.coerceAtMost(levelColors.lastIndex)]
            Level(level.second, level.third, colorPair.first, colorPair.second) { onPick(level.first) }
        }
    }
}

@Composable
private fun Level(title: String, sub: String, face: Color, edge: Color, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().chunky(face, edge, radius = 20.dp, depth = 5.dp).clickable(onClick = onClick).padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(Modifier.size(46.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.3f)), contentAlignment = Alignment.Center) {
            Icon(Icons.Filled.Bolt, null, tint = Color.White, modifier = Modifier.size(28.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(title, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text(sub, fontSize = 13.sp, color = Color.White.copy(alpha = 0.95f), fontWeight = FontWeight.SemiBold)
        }
        Icon(Icons.Filled.ChevronRight, null, tint = Color.White)
    }
}

@Composable
private fun Board(words: List<Word>, pairCount: Int, vm: YuhmuViewModel, onClose: () -> Unit, modifier: Modifier) {
    val initial = remember(words, pairCount) {
        val fitting = words.filter { it.yuhmu.length <= 12 && it.spanish.length <= 14 }
        val base = (if (fitting.size >= pairCount) fitting else words)
        val withAudio = base.filter { vm.audio.hasAudio(it.id) }
        val pool = if (withAudio.size >= pairCount) withAudio else base
        pool.shuffled().take(pairCount).flatMapIndexed { i, w ->
            val c = PAIR_COLORS[i % PAIR_COLORS.size]
            listOf(
                MemoCard(pairId = w.id, text = w.yuhmu, isYuhmu = true, color = c),
                MemoCard(pairId = w.id, text = w.spanish, isYuhmu = false, color = c)
            )
        }.shuffled().mapIndexed { i, c -> c.copy(id = i) }
    }

    var cards by remember(initial) { mutableStateOf(initial) }
    var first by remember(initial) { mutableStateOf<Int?>(null) }
    var busy by remember(initial) { mutableStateOf(false) }
    var moves by remember(initial) { mutableIntStateOf(0) }
    var combo by remember(initial) { mutableIntStateOf(0) }
    var bestCombo by remember(initial) { mutableIntStateOf(0) }
    var seconds by remember(initial) { mutableIntStateOf(0) }
    var finished by remember(initial) { mutableStateOf<ChallengeResult?>(null) }
    var mood by remember(initial) { mutableStateOf(Mood.HAPPY) }
    var message by remember(initial) { mutableStateOf("¡Encuentra las parejas!") }
    val scope = rememberCoroutineScope()

    val total = cards.size / 2
    val matchedPairs = cards.count { it.matched } / 2

    LaunchedEffect(initial) {
        while (finished == null) { delay(1000); seconds++ }
    }

    val done = finished
    if (done != null) {
        Box(modifier) { ResultContent(done, onClose) }
        return
    }

    fun tap(i: Int) {
        val c = cards[i]
        if (busy || c.faceUp || c.matched) return
        cards = cards.mapIndexed { idx, card -> if (idx == i) card.copy(faceUp = true) else card }
        if (c.isYuhmu) vm.audio.play(c.pairId)
        val f = first
        if (f == null) { first = i; return }
        moves++
        first = null
        busy = true
        scope.launch {
            if (cards[f].pairId == cards[i].pairId) {
                delay(350)
                combo++
                bestCombo = max(bestCombo, combo)
                mood = Mood.CHEER
                message = if (combo >= 2) "¡Combo x$combo! 🔥" else listOf("¡Muy bien!", "¡Pareja!", "¡Así se hace!").random()
                vm.audio.play(cards[i].pairId)
                cards = cards.mapIndexed { idx, card -> if (idx == f || idx == i) card.copy(matched = true, bounce = true) else card }
                delay(450)
                cards = cards.map { it.copy(bounce = false) }
                busy = false
                if (cards.all { it.matched }) {
                    val timeBonus = (total * 10 - seconds).coerceIn(0, 40)
                    val pts = total * 10 + bestCombo * 5 + timeBonus
                    val stars = when { moves <= total * 3 / 2 -> 3; moves <= total * 5 / 2 -> 2; else -> 1 }
                    val r = ChallengeResult(total, total, pts, 3, bestCombo, starsOverride = stars)
                    vm.recordMemorama(pts, won = true)
                    finished = r
                }
            } else {
                combo = 0
                mood = Mood.SAD
                message = listOf("¡Casi!", "Inténtalo otra vez", "Recuerda dónde estaba").random()
                delay(850)
                cards = cards.mapIndexed { idx, card -> if (idx == f || idx == i) card.copy(faceUp = false) else card }
                mood = Mood.HAPPY
                busy = false
            }
        }
    }

    val cols = if (pairCount >= 8) 4 else 3
    val cardH: Dp = when {
        pairCount <= 4 -> 118.dp
        pairCount <= 6 -> 100.dp
        pairCount <= 8 -> 86.dp
        pairCount <= 10 -> 76.dp
        else -> 68.dp
    }
    val fontSize = when {
        pairCount >= 10 -> 11
        pairCount >= 8 -> 13
        else -> 16
    }

    Column(modifier.padding(horizontal = 14.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Mascot(size = 62.dp, mood = mood)
            Text(
                message, Modifier.weight(1f).background(Color.White, RoundedCornerShape(16.dp))
                    .border(2.dp, Brand.lavender, RoundedCornerShape(16.dp)).padding(12.dp),
                fontWeight = FontWeight.ExtraBold, color = Brand.purple, fontSize = 16.sp
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Chip(Icons.Filled.Timer, "%d:%02d".format(seconds / 60, seconds % 60), Color(0xFF29B6F6), Modifier.weight(1f))
            Chip(Icons.Filled.TouchApp, "$moves", Brand.purpleMid, Modifier.weight(1f))
            Chip(Icons.Filled.Bolt, "x$combo", Brand.orange, Modifier.weight(1f))
            Chip(Icons.Filled.Eco, "$matchedPairs/$total", Brand.green, Modifier.weight(1f))
        }
        ChunkyProgress(matchedPairs.toFloat() / total.coerceAtLeast(1), color = Brand.green, height = 10.dp)
        LazyVerticalGrid(
            columns = GridCells.Fixed(cols),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            itemsIndexed(cards, key = { _, c -> c.id }) { i, c -> MemoCell(c, cardH, fontSize) { tap(i) } }
        }
    }
}

@Composable
private fun Chip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, color: Color, modifier: Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(4.dp))
        Text(text, color = color, fontWeight = FontWeight.ExtraBold, fontSize = 13.sp)
    }
}

/** Carta con volteo 3D, color propio por pareja y "rebote" al acertar. */
@Composable
private fun MemoCell(card: MemoCard, height: Dp, fontSize: Int, onClick: () -> Unit) {
    val open = card.faceUp || card.matched
    val rotation by animateFloatAsState(if (open) 180f else 0f, tween(380), label = "flip")
    val pop by animateFloatAsState(if (card.bounce) 1.14f else 1f, spring(Spring.DampingRatioMediumBouncy), label = "pop")
    val density = LocalDensity.current.density
    val shape = RoundedCornerShape(18.dp)

    Box(
        Modifier.fillMaxWidth().height(height)
            .graphicsLayer { rotationY = rotation; scaleX = pop; scaleY = pop; cameraDistance = 14f * density }
            .clip(shape).clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        if (rotation <= 90f) {
            Box(
                Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Brand.purpleMid, Brand.purple)))
                    .border(3.dp, Brand.purpleLight, shape),
                contentAlignment = Alignment.Center
            ) {
                Box(Modifier.size(44.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Eco, null, tint = Brand.gold, modifier = Modifier.size(28.dp))
                }
            }
        } else {
            val bg = if (card.matched) card.color else Color.White
            Box(
                Modifier.fillMaxSize().graphicsLayer { rotationY = 180f }
                    .background(bg).border(3.dp, card.color, shape),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        if (card.isYuhmu) "Yuhmu" else "Español", fontSize = 9.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (card.matched) Color.White.copy(alpha = 0.85f) else card.color
                    )
                    Text(
                        card.text, fontSize = fontSize.sp, fontWeight = FontWeight.ExtraBold,
                        color = if (card.matched) Color.White else Brand.ink,
                        textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }
            }
        }
    }
}
