package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeBlue
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeCoral
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeYellow
import kotlin.random.Random

private data class Confetto(
    val x: Float,
    val startY: Float,
    val fall: Float,
    val drift: Float,
    val color: Color,
    val size: Float,
    val rot: Float,
    val rotSpeed: Float,
)

/**
 * 컨페티 오버레이 (이펙트 4). 화면 최상단 Box 의 마지막 자식으로 둔다.
 *
 * [trigger] 를 0 → 1 → 2 … 로 증가시킬 때마다 한 번 터진다. 0 이면 아무것도 안 그린다.
 * 사용 예: 기억 저장 성공 시 `confetti++`.
 */
@Composable
fun ConfettiOverlay(
    trigger: Int,
    modifier: Modifier = Modifier,
) {
    if (trigger == 0) return

    val palette = listOf(AbodeBlue, AbodeYellow, AbodeCoral, AbodeGreen)
    val pieces = remember(trigger) {
        List(30) { i ->
            Confetto(
                x = Random.nextFloat(),
                startY = -0.05f - Random.nextFloat() * 0.2f,
                fall = 0.9f + Random.nextFloat() * 0.45f,
                drift = (Random.nextFloat() - 0.5f) * 0.28f,
                color = palette[i % palette.size],
                size = 7f + Random.nextFloat() * 9f,
                rot = Random.nextFloat() * 360f,
                rotSpeed = (Random.nextFloat() - 0.5f) * 720f,
            )
        }
    }
    val progress = remember(trigger) { Animatable(0f) }
    LaunchedEffect(trigger) {
        progress.snapTo(0f)
        progress.animateTo(1f, tween(durationMillis = 1300, easing = LinearEasing))
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val p = progress.value
        val alpha = (1f - p * p).coerceIn(0f, 1f)
        pieces.forEach { c ->
            val cx = (c.x + c.drift * p) * size.width
            val cy = (c.startY + c.fall * p) * size.height
            rotate(degrees = c.rot + c.rotSpeed * p, pivot = Offset(cx, cy)) {
                drawRect(
                    color = c.color.copy(alpha = alpha),
                    topLeft = Offset(cx - c.size / 2f, cy - c.size / 2f),
                    size = Size(c.size, c.size * 1.6f),
                )
            }
        }
    }
}
