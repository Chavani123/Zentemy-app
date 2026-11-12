package mx.zentemi.yuhmu.ui.trophies

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.zentemi.yuhmu.data.*
import mx.zentemi.yuhmu.ui.components.*
import mx.zentemi.yuhmu.ui.theme.Brand
import mx.zentemi.yuhmu.viewmodel.YuhmuViewModel
import kotlin.math.max

private data class Achievement(val id: String, val title: String, val detail: String, val icon: ImageVector)

private val ACHIEVEMENTS = listOf(
    Achievement("first_lesson", "Primer paso", "Completa tu primera lección", Icons.Filled.Eco),
    Achievement("lessons_5", "Constante", "Completa 5 lecciones", Icons.Filled.MenuBook),
    Achievement("streak_3", "Racha de 3", "3 días seguidos", Icons.Filled.LocalFireDepartment),
    Achievement("streak_7", "Racha de 7", "7 días seguidos", Icons.Filled.Whatshot),
    Achievement("words_50", "50 palabras", "Aprende 50 palabras", Icons.Filled.Book),
    Achievement("words_100", "100 palabras", "Aprende 100 palabras", Icons.Filled.School),
    Achievement("points_500", "500 puntos", "Suma 500 puntos", Icons.Filled.Stars),
    Achievement("daily_1", "Reto diario", "Completa un reto del día", Icons.Filled.EventAvailable),
    Achievement("memorama_1", "Buena memoria", "Gana un Memorama", Icons.Filled.Psychology),
    Achievement("perfect_1", "¡Perfecto!", "Reto sin errores", Icons.Filled.Verified),
    Achievement("lessons_20", "Imparable", "Completa 20 lecciones", Icons.Filled.Bolt),
    Achievement("words_150", "150 palabras", "Aprende 150 palabras", Icons.Filled.AutoStories),
    Achievement("points_1500", "1500 puntos", "Suma 1500 puntos", Icons.Filled.WorkspacePremium),
    Achievement("story_1", "Cuentacuentos", "Termina una historia", Icons.Filled.Language),
    Achievement("story_4", "Gran lector", "Termina 4 historias", Icons.Filled.LibraryBooks),
    Achievement("speak_10", "Buena voz", "Pronuncia 10 palabras", Icons.Filled.RecordVoiceOver),
    Achievement("speak_50", "Hablante nativo", "Pronuncia 50 palabras", Icons.Filled.Mic)
)

@Composable
fun TrophiesScreen(vm: YuhmuViewModel, bottomBar: @Composable () -> Unit) {
    val progress by vm.progress.collectAsState()

    TabScaffold(progress, bottomBar) { padding ->
        Column(
            Modifier.padding(padding).fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().chunky(Brand.purple, Brand.purpleDark, radius = 22.dp, depth = 6.dp).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Mascot(size = 82.dp, mood = Mood.CHEER)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Nivel ${progress.level()}", fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                    Text(progress.levelTitle(), fontSize = 15.sp, color = Brand.lavender, fontWeight = FontWeight.Bold)
                    ChunkyProgress(progress.levelProgress(), color = Brand.gold, height = 10.dp)
                    Text("${progress.pointsToNextLevel()} puntos para el siguiente nivel", fontSize = 12.sp, color = Brand.lavender)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                StatCard(Icons.Filled.LocalFireDepartment, "${progress.currentStreak()}", "Racha", Brand.orange, Modifier.weight(1f))
                StatCard(Icons.Filled.Star, "${progress.points}", "Puntos", Brand.goldDark, Modifier.weight(1f))
                StatCard(Icons.Filled.EmojiEvents, "${progress.bestStreak}", "Mejor racha", Brand.purpleMid, Modifier.weight(1f))
            }

            Column(Modifier.fillMaxWidth().card(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tu avance", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Brand.ink)
                ProgressRow("Palabras aprendidas", progress.learnedWordIds.size, vm.content.allWords.size, Brand.purpleMid)
                ProgressRow("Lecciones completadas", progress.completedLessons.size, vm.content.lessons.size, Brand.green)
            }

            Column(Modifier.fillMaxWidth().card(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Logros", fontSize = 18.sp, fontWeight = FontWeight.ExtraBold, color = Brand.ink)
                ACHIEVEMENTS.chunked(3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        row.forEach { a -> AchievementTile(a, a.id in progress.badges, Modifier.weight(1f)) }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(icon: ImageVector, value: String, label: String, color: Color, modifier: Modifier) {
    Column(modifier.card(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(28.dp))
        Text(value, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold, color = color)
        Text(label, fontSize = 12.sp, color = Brand.gray, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ProgressRow(label: String, value: Int, total: Int, color: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, fontSize = 14.sp, color = Brand.ink, fontWeight = FontWeight.SemiBold)
            Text("$value/$total", fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, color = color)
        }
        ChunkyProgress(value.toFloat() / max(total, 1), color = color, height = 12.dp)
    }
}

@Composable
private fun AchievementTile(a: Achievement, earned: Boolean, modifier: Modifier) {
    Column(modifier.heightIn(min = 112.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(if (earned) Brand.gold else Brand.grayLight),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (earned) a.icon else Icons.Filled.Lock,
                contentDescription = null,
                tint = if (earned) Color.White else Brand.grayEdge,
                modifier = Modifier.size(28.dp)
            )
        }
        Text(a.title, fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, color = if (earned) Brand.ink else Brand.gray, textAlign = TextAlign.Center)
        Text(a.detail, fontSize = 10.sp, color = Brand.gray, textAlign = TextAlign.Center)
    }
}
