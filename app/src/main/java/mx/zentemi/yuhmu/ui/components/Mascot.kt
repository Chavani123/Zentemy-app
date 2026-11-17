package mx.zentemi.yuhmu.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.zentemi.yuhmu.model.AppCharacter
import mx.zentemi.yuhmu.ui.theme.Brand

enum class Mood { HAPPY, SAD, CHEER }

/** Mascotas / Personajes oficiales de Yhụmụ Zentemi: Muestra el avatar del personaje en su insignia circular con sus colores e ilustraciones intactas. */
@Composable
fun Mascot(
    size: Dp = 96.dp,
    mood: Mood = Mood.HAPPY,
    character: AppCharacter = AppCharacter.RABBIT,
    animate: Boolean = true,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "mascot")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )

    val scaleFactor = when (mood) {
        Mood.CHEER -> 1.06f + (if (animate) bob * 0.04f else 0f)
        Mood.SAD -> 0.94f
        Mood.HAPPY -> 1.0f
    }

    val glowColor = when (mood) {
        Mood.CHEER -> Brand.gold
        Mood.SAD -> Brand.grayLight
        Mood.HAPPY -> Brand.purpleMid
    }

    Box(
        modifier = modifier
            .size(size)
            .graphicsLayer {
                translationY = -size.toPx() * 0.04f * (if (animate) bob else 0f)
                scaleX = scaleFactor
                scaleY = scaleFactor
            },
        contentAlignment = Alignment.Center
    ) {
        // Marco de insignia circular que contiene al personaje con su fondo blanco e ilustraciones intactas
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(CircleShape)
                .background(Color.White)
                .border(3.dp, glowColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = character.drawableRes),
                contentDescription = character.characterName,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color.White)
            )
        }
    }
}
