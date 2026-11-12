package mx.zentemi.yuhmu.ui.components

import android.app.Activity
import android.content.pm.ApplicationInfo
import android.view.WindowManager
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.zentemi.yuhmu.data.UserProgress
import mx.zentemi.yuhmu.data.currentStreak
import mx.zentemi.yuhmu.data.level
import mx.zentemi.yuhmu.model.ChallengeResult
import mx.zentemi.yuhmu.ui.theme.Brand

// ---------------------------------------------------------------- modificadores

/** Tarjeta blanca con borde lavanda. */
fun Modifier.card(): Modifier = this
    .shadow(6.dp, RoundedCornerShape(20.dp), ambientColor = Brand.purpleMid, spotColor = Brand.purpleMid)
    .background(Color.White, RoundedCornerShape(20.dp))
    .border(2.dp, Brand.lavender, RoundedCornerShape(20.dp))
    .padding(16.dp)

/** Caja "3D": cara de color + borde inferior más oscuro (estilo Duolingo). */
fun Modifier.chunky(face: Color, edge: Color, radius: Dp = 18.dp, depth: Dp = 5.dp): Modifier = this
    .padding(bottom = depth)
    .drawBehind {
        val corner = CornerRadius(radius.toPx())
        drawRoundRect(edge, topLeft = Offset(0f, depth.toPx()), size = size, cornerRadius = corner)
        drawRoundRect(face, size = size, cornerRadius = corner)
    }

/** Mapea los SF Symbols de vocabulary.json a iconos de Material. */
fun categoryIcon(name: String): ImageVector = when (name) {
    "number" -> Icons.Filled.Tag
    "figure.stand" -> Icons.Filled.Accessibility
    "person.2" -> Icons.Filled.Groups
    "leaf" -> Icons.Filled.Eco
    "fork.knife", "restaurant" -> Icons.Filled.Restaurant
    "pets" -> Icons.Filled.Pets
    "checkroom" -> Icons.Filled.Checkroom
    "home" -> Icons.Filled.Home
    "directions_run" -> Icons.Filled.DirectionsRun
    "palette" -> Icons.Filled.Palette
    "auto_awesome" -> Icons.Filled.AutoAwesome
    "cloud" -> Icons.Filled.Cloud
    "menu_book" -> Icons.Filled.MenuBook
    else -> Icons.Filled.MenuBook
}

// ---------------------------------------------------------------- botones

@Composable
fun ChunkyButton(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Brand.green,
    edge: Color = Brand.greenDark,
    contentColor: Color = Color.White,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = 5.dp
    val dy by animateDpAsState(if (pressed && enabled) depth else 0.dp, label = "press")
    val face = if (enabled) color else Brand.grayLight
    val rim = if (enabled) edge else Brand.grayEdge
    val label = if (enabled) contentColor else Brand.gray

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp + depth)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
    ) {
        Box(
            Modifier.fillMaxWidth().height(54.dp).align(Alignment.BottomCenter)
                .background(rim, RoundedCornerShape(16.dp))
        )
        Box(
            Modifier.fillMaxWidth().height(54.dp).offset(y = dy).background(face, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(text.uppercase(), color = label, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, letterSpacing = 0.8.sp)
        }
    }
}

/** Botón circular 3D (nodos del camino, botón de audio). */
@Composable
fun ChunkyCircle(
    size: Dp,
    face: Color,
    rim: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val depth = 7.dp
    val dy by animateDpAsState(if (pressed) depth else 0.dp, label = "circle")
    Box(
        modifier = modifier
            .size(size, size + depth)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
    ) {
        Box(Modifier.size(size).align(Alignment.BottomCenter).background(rim, CircleShape))
        Box(
            Modifier.size(size).offset(y = dy).background(face, CircleShape),
            contentAlignment = Alignment.Center,
            content = content
        )
    }
}

// ---------------------------------------------------------------- respuestas

enum class OptionState { IDLE, SELECTED, CORRECT, WRONG, DIM }

@Composable
fun OptionCard(text: String, state: OptionState, enabled: Boolean, onClick: () -> Unit) {
    val (bg, border, textColor) = when (state) {
        OptionState.IDLE -> Triple(Color.White, Brand.grayLight, Brand.ink)
        OptionState.SELECTED -> Triple(Brand.lavender, Brand.purpleMid, Brand.purple)
        OptionState.CORRECT -> Triple(Brand.greenLight, Brand.green, Brand.greenDark)
        OptionState.WRONG -> Triple(Brand.redLight, Brand.red, Brand.redDark)
        OptionState.DIM -> Triple(Color.White, Brand.grayLight, Brand.gray)
    }
    val shape = RoundedCornerShape(16.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg)
            .border(2.dp, border, shape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Text(text, color = textColor, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

// ---------------------------------------------------------------- progreso y estadísticas

@Composable
fun ChunkyProgress(
    fraction: Float,
    modifier: Modifier = Modifier,
    color: Color = Brand.green,
    height: Dp = 16.dp
) {
    val animated by animateFloatAsState(fraction.coerceIn(0f, 1f), animationSpec = tween(400), label = "progress")
    Box(modifier.height(height).clip(RoundedCornerShape(50)).background(Brand.grayLight)) {
        Box(Modifier.fillMaxHeight().fillMaxWidth(animated).clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
fun StatsBar(streak: Int, points: Int, level: Int, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        StatItem(Icons.Filled.LocalFireDepartment, "$streak", Brand.orange)
        StatItem(Icons.Filled.Star, "$points", Brand.goldDark)
        StatItem(Icons.Filled.Shield, "Nv. $level", Brand.purpleMid)
    }
}

@Composable
private fun StatItem(icon: ImageVector, text: String, color: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.15f)).padding(horizontal = 14.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(24.dp))
        Text(text, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = color)
    }
}

/** Contenedor de las pestañas principales: barra superior con racha, puntos y nivel. */
@Composable
fun TabScaffold(
    progress: UserProgress,
    bottomBar: @Composable () -> Unit,
    content: @Composable (PaddingValues) -> Unit
) {
    Box(Modifier.fillMaxSize().background(Brand.bgBrush)) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            Surface(color = Color.Transparent) {
                Column(Modifier.statusBarsPadding()) {
                    StatsBar(
                        progress.currentStreak(), progress.points, progress.level(),
                        Modifier.padding(horizontal = 24.dp, vertical = 10.dp)
                    )
                    HorizontalDivider(color = Brand.lavender, thickness = 2.dp)
                }
            }
        },
        bottomBar = bottomBar,
        content = content
    )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun YuhmuScaffold(
    title: String,
    onBack: (() -> Unit)? = null,
    bottomBar: @Composable () -> Unit = {},
    content: @Composable (PaddingValues) -> Unit
) {
    Box(Modifier.fillMaxSize().background(Brand.bgBrush)) {
    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { Text(title, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    if (onBack != null) {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Atrás")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Brand.purple,
                    navigationIconContentColor = Brand.purple
                )
            )
        },
        bottomBar = bottomBar,
        content = content
    )
    }
}

/** Pantalla completa sin barras (lecciones, retos): respeta los insets del sistema. */
@Composable
fun FullScreen(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize().background(Brand.bgBrush).systemBarsPadding()) { content() }
}

// ---------------------------------------------------------------- resultado

@Composable
fun ResultContent(result: ChallengeResult, onClose: () -> Unit) {
    val title = when (result.stars) {
        3 -> "¡Increíble!"
        2 -> "¡Muy bien!"
        1 -> "¡Buen intento!"
        else -> "¡Sigue practicando!"
    }
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Spacer(Modifier.weight(1f))
        Mascot(size = 150.dp, mood = if (result.stars > 0) Mood.CHEER else Mood.SAD)
        Text(title, fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, color = Brand.purple)
        Row {
            repeat(3) { i ->
                Icon(
                    if (i < result.stars) Icons.Filled.Star else Icons.Outlined.StarBorder,
                    contentDescription = null,
                    tint = Brand.gold,
                    modifier = Modifier.size(44.dp)
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ResultStat("PUNTOS", "+${result.points}", Brand.goldDark, Modifier.weight(1f))
            ResultStat("ACIERTOS", "${result.correct}/${result.total}", Brand.greenDark, Modifier.weight(1f))
            ResultStat("COMBO", "x${result.bestCombo}", Brand.purpleMid, Modifier.weight(1f))
        }
        Spacer(Modifier.weight(1f))
        ChunkyButton("Continuar", onClick = onClose)
    }
}

@Composable
private fun ResultStat(label: String, value: String, color: Color, modifier: Modifier) {
    Column(modifier.clip(RoundedCornerShape(16.dp)).background(color).padding(2.dp)) {
        Text(
            label, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
        )
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color.White).padding(vertical = 16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(value, color = color, fontSize = 22.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun TextCenter(text: String, color: Color = Brand.gray, size: Int = 12) {
    Text(text, fontSize = size.sp, color = color, textAlign = TextAlign.Center)
}

// ---------------------------------------------------------------- seguridad de ventana

/**
 * Bloquea capturas y la vista previa en "recientes" mientras se escriben contraseñas.
 * En builds de depuración se omite para poder tomar capturas de la documentación.
 */
@Composable
fun SecureWindowEffect() {
    val context = LocalContext.current
    DisposableEffect(Unit) {
        val debuggable = (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val window = (context as? Activity)?.window
        if (!debuggable) window?.setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE)
        onDispose { if (!debuggable) window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE) }
    }
}
