package mx.zentemi.yuhmu.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.zentemi.yuhmu.model.Word
import mx.zentemi.yuhmu.ui.theme.Brand

/** Ilustración pequeña y consistente para apoyar visualmente el vocabulario sin emojis. */
@Composable
fun LearningIllustration(word: Word, size: Dp = 78.dp) {
    val icon = illustrationIconFor(word)
    Box(
        Modifier.size(size)
            .clip(RoundedCornerShape(22.dp))
            .background(Brand.lavender.copy(alpha = 0.72f))
            .border(1.dp, Brand.purpleLight, RoundedCornerShape(22.dp)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = Brand.purple, modifier = Modifier.size(size * 0.5f))
    }
}

private fun illustrationIconFor(word: Word): ImageVector {
    val term = word.spanish.lowercase()
    return when {
        "agua" in term -> Icons.Filled.WaterDrop
        "maíz" in term || "mazorca" in term -> Icons.Filled.Eco
        "manzana" in term || "naranja" in term || "comida" in term || "alimento" in term -> Icons.Filled.Restaurant
        "flor" in term -> Icons.Filled.LocalFlorist
        "perro" in term || "gato" in term || "abeja" in term || "animal" in term -> Icons.Filled.Pets
        "cielo" in term || "nube" in term || "lluvia" in term || "clima" in term -> Icons.Filled.Cloud
        "sol" in term -> Icons.Filled.WbSunny
        "luna" in term -> Icons.Filled.NightsStay
        "estrella" in term -> Icons.Filled.Star
        "ropa" in term -> Icons.Filled.Checkroom
        "casa" in term -> Icons.Filled.Home
        word.categoryId == "numeros" -> Icons.Filled.Tag
        word.categoryId == "cuerpo" -> Icons.Filled.Accessibility
        word.categoryId == "familia" -> Icons.Filled.Groups
        word.categoryId == "plantas" -> Icons.Filled.Eco
        word.categoryId == "alimentos" -> Icons.Filled.Restaurant
        word.categoryId == "animales" -> Icons.Filled.Pets
        word.categoryId == "ropa" -> Icons.Filled.Checkroom
        word.categoryId == "objetos" -> Icons.Filled.ShoppingBag
        word.categoryId == "colores" -> Icons.Filled.Palette
        word.categoryId == "adjetivos" -> Icons.Filled.AutoAwesome
        word.categoryId == "verbos" -> Icons.AutoMirrored.Filled.DirectionsRun
        word.categoryId == "clima" -> Icons.Filled.WbSunny
        else -> Icons.AutoMirrored.Filled.MenuBook
    }
}
