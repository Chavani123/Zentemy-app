package mx.zentemi.yuhmu.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Identidad visual Yuhmu: paleta morada del Project Charter (RF-10) + acentos de maíz. */
object Brand {
    // Paleta oficial del Charter
    val purple = Color(0xFF6B2E83)
    val purpleMid = Color(0xFF9B4DCA)
    val purpleLight = Color(0xFFBA7CFF)
    val lavender = Color(0xFFEDE0F9)
    val mist = Color(0xFFF7F2FF)

    // Bordes inferiores de los botones 3D
    val purpleDark = Color(0xFF4B1F5E)
    val purpleMidDark = Color(0xFF7A38A8)

    // Acentos
    val gold = Color(0xFFFFC83D)
    val goldDark = Color(0xFFD9A000)
    val orange = Color(0xFFFF9600)
    val green = Color(0xFF58CC02)
    val greenDark = Color(0xFF46A302)
    val greenLight = Color(0xFFD7FFB8)
    val red = Color(0xFFFF4B4B)
    val redDark = Color(0xFFD33131)
    val redLight = Color(0xFFFFDFE0)
    val leaf = Color(0xFF58A83A)
    val leafDark = Color(0xFF3F8A2B)
    val pink = Color(0xFFFF8FB1)

    // Neutros
    val ink = Color(0xFF3B2347)
    val gray = Color(0xFF8E8A94)
    val grayLight = Color(0xFFE5DEEC)
    val grayEdge = Color(0xFFC9C0D3)
    val bg = Color(0xFFFCFAFF)
    val sky = Color(0xFF29B6F6)
    val teal = Color(0xFF1FA89A)
    val bgBrush = Brush.verticalGradient(listOf(Color(0xFFF1E6FF), Color(0xFFFFF3E8), Color(0xFFE9F8FF)))
}

private val YuhmuColors = lightColorScheme(
    primary = Brand.purple,
    onPrimary = Color.White,
    primaryContainer = Brand.lavender,
    secondary = Brand.purpleMid,
    tertiary = Brand.gold,
    background = Brand.bg,
    onBackground = Brand.ink,
    surface = Color.White,
    onSurface = Brand.ink,
    outline = Brand.grayLight
)

@Composable
fun YuhmuTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = YuhmuColors, content = content)
}
