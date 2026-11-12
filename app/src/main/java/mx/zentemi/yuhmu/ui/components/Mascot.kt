package mx.zentemi.yuhmu.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mx.zentemi.yuhmu.ui.theme.Brand

enum class Mood { HAPPY, SAD, CHEER }

/** Yuhmu, el elote morado: mascota dibujada con Canvas (sin imágenes, escala a cualquier tamaño). */
@Composable
fun Mascot(
    size: Dp = 96.dp,
    mood: Mood = Mood.HAPPY,
    animate: Boolean = true,
    modifier: Modifier = Modifier
) {
    val sizePx = with(LocalDensity.current) { size.toPx() }
    val transition = rememberInfiniteTransition(label = "mascot")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "bob"
    )
    Canvas(
        modifier = modifier
            .size(size)
            .graphicsLayer { translationY = -sizePx * 0.04f * (if (animate) bob else 0f) }
    ) {
        val w = this.size.width
        val h = this.size.height
        val cx = w / 2f

        // Totomoxtle (hojas del elote)
        rotate(degrees = -24f, pivot = Offset(cx, h * 0.95f)) {
            drawOval(Brand.leaf, Offset(cx - w * 0.30f, h * 0.42f), Size(w * 0.25f, h * 0.55f))
        }
        rotate(degrees = 24f, pivot = Offset(cx, h * 0.95f)) {
            drawOval(Brand.leafDark, Offset(cx + w * 0.05f, h * 0.42f), Size(w * 0.25f, h * 0.55f))
        }

        // Mazorca morada
        val cobTopLeft = Offset(cx - w * 0.28f, h * 0.05f)
        val cobSize = Size(w * 0.56f, h * 0.84f)
        drawOval(
            brush = Brush.verticalGradient(listOf(Brand.purpleLight, Brand.purpleMid)),
            topLeft = cobTopLeft,
            size = cobSize
        )

        // Granos
        val rx = cobSize.width / 2f
        val ry = cobSize.height / 2f
        val ocx = cobTopLeft.x + rx
        val ocy = cobTopLeft.y + ry
        for (row in 0..8) {
            for (col in -2..2) {
                val kx = ocx + col * w * 0.1f + (if (row % 2 == 0) 0f else w * 0.05f)
                val ky = h * 0.11f + row * h * 0.093f
                val nx = (kx - ocx) / (rx * 0.88f)
                val ny = (ky - ocy) / (ry * 0.9f)
                if (nx * nx + ny * ny < 1f) {
                    drawCircle(Color.White.copy(alpha = 0.2f), radius = w * 0.03f, center = Offset(kx, ky))
                }
            }
        }

        // Barbas del elote
        for (i in -1..1) {
            drawLine(
                color = Brand.gold,
                start = Offset(cx, h * 0.08f),
                end = Offset(cx + i * w * 0.07f, h * 0.0f),
                strokeWidth = w * 0.025f,
                cap = StrokeCap.Round
            )
        }

        // Ojos y mejillas
        val eyeY = h * 0.34f
        for (side in listOf(-1f, 1f)) {
            val ex = cx + side * w * 0.115f
            drawCircle(Color.White, w * 0.075f, Offset(ex, eyeY))
            drawCircle(Brand.ink, w * 0.038f, Offset(ex, eyeY + w * 0.01f))
            drawCircle(Color.White, w * 0.012f, Offset(ex + w * 0.012f, eyeY))
            drawCircle(Brand.pink.copy(alpha = 0.65f), w * 0.05f, Offset(cx + side * w * 0.19f, h * 0.49f))
        }

        // Boca según el ánimo
        val mouthW = w * 0.2f
        when (mood) {
            Mood.HAPPY -> drawArc(
                color = Brand.ink, startAngle = 15f, sweepAngle = 150f, useCenter = false,
                topLeft = Offset(cx - mouthW / 2f, h * 0.43f), size = Size(mouthW, mouthW * 0.7f),
                style = Stroke(width = w * 0.03f, cap = StrokeCap.Round)
            )
            Mood.CHEER -> {
                drawArc(
                    color = Brand.ink, startAngle = 0f, sweepAngle = 180f, useCenter = true,
                    topLeft = Offset(cx - mouthW / 2f, h * 0.44f), size = Size(mouthW, mouthW * 0.85f)
                )
                drawArc(
                    color = Color(0xFFFF6B8B), startAngle = 20f, sweepAngle = 140f, useCenter = true,
                    topLeft = Offset(cx - mouthW * 0.32f, h * 0.44f + mouthW * 0.45f),
                    size = Size(mouthW * 0.64f, mouthW * 0.4f)
                )
            }
            Mood.SAD -> drawArc(
                color = Brand.ink, startAngle = 200f, sweepAngle = 140f, useCenter = false,
                topLeft = Offset(cx - mouthW / 2f, h * 0.52f), size = Size(mouthW, mouthW * 0.6f),
                style = Stroke(width = w * 0.03f, cap = StrokeCap.Round)
            )
        }
    }
}
