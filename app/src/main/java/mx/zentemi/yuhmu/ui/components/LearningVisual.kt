package mx.zentemi.yuhmu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.zentemi.yuhmu.model.Word
import mx.zentemi.yuhmu.ui.theme.Brand

/** Ilustración pequeña y consistente para apoyar visualmente el vocabulario. */
@Composable
fun LearningIllustration(word: Word, size: Dp = 78.dp) {
    val emoji = illustrationFor(word)
    Box(
        Modifier.size(size)
            .clip(RoundedCornerShape(22.dp))
            .background(Brand.lavender.copy(alpha = 0.72f))
            .border(1.dp, Brand.purpleLight, RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(emoji, fontSize = (size.value * 0.55f).sp)
    }
}

private fun illustrationFor(word: Word): String {
    val term = word.spanish.lowercase()
    return when {
        "agua" in term -> "💧"
        "maíz" in term || "mazorca" in term -> "🌽"
        "manzana" in term -> "🍎"
        "aguacate" in term -> "🥑"
        "naranja" in term -> "🍊"
        "calabaza" in term -> "🎃"
        "flor" in term -> "🌼"
        "frijol" in term || "haba" in term -> "🫘"
        "tamal" in term -> "🫔"
        "tortilla" in term -> "🫓"
        "atole" in term || "comida" in term || "alimento" in term -> "🥣"
        "huevo" in term -> "🥚"
        "perro" in term -> "🐕"
        "gato" in term -> "🐈"
        "abeja" in term -> "🐝"
        "caballo" in term || "burro" in term -> "🐴"
        "gallina" in term || "pollo" in term || "gallo" in term -> "🐓"
        "cielo" in term || "nube" in term -> "☁️"
        "lluvia" in term -> "🌧️"
        "sol" in term -> "☀️"
        "luna" in term -> "🌙"
        "estrella" in term -> "⭐"
        "ropa" in term || "camisa" in term || "sombrero" in term -> "👕"
        "casa" in term -> "🏠"
        word.categoryId == "numeros" -> "🔢"
        word.categoryId == "cuerpo" -> "🧍"
        word.categoryId == "familia" -> "👨‍👩‍👧"
        word.categoryId == "plantas" -> "🌱"
        word.categoryId == "alimentos" -> "🍽️"
        word.categoryId == "animales" -> "🐾"
        word.categoryId == "ropa" -> "🧣"
        word.categoryId == "objetos" -> "🧺"
        word.categoryId == "colores" -> "🎨"
        word.categoryId == "adjetivos" -> "✨"
        word.categoryId == "verbos" -> "🏃"
        word.categoryId == "clima" -> "🌤️"
        else -> "📖"
    }
}
