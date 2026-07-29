package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeBlue
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeCoral
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeYellow

/**
 * "추억을 소환 중" 커스텀 로더 — 작은 사진 카드 3장이 물결치듯 톡톡 올라온다.
 * 일반 CircularProgressIndicator 대신 로딩 상태에 쓴다.
 */
@Composable
fun AlbumLoader(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "album")
    val colors = listOf(AbodeBlue, AbodeYellow, AbodeCoral)

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        colors.forEachIndexed { i, color ->
            val scale by transition.animateFloat(
                initialValue = 0.65f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 620,
                        delayMillis = i * 150,
                        easing = FastOutSlowInEasing,
                    ),
                    repeatMode = RepeatMode.Reverse,
                ),
                label = "photoScale$i",
            )
            androidx.compose.foundation.layout.Box(
                modifier = Modifier
                    .size(18.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        rotationZ = (scale - 0.8f) * 24f
                    }
                    .clip(RoundedCornerShape(6.dp))
                    .background(color),
            )
        }
    }
}
