package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeCoral
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary

/**
 * 좋아요 하트 + 개수. 좋아요로 켜질 때 하트가 톡 튀는 팝 애니메이션 (이펙트 3).
 *
 * 상태(liked/count)는 화면 ViewModel 이 들고, 여기서는 표시 + 탭만 한다.
 */
@Composable
fun LikeButton(
    liked: Boolean,
    count: Int,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scale = remember { Animatable(1f) }
    LaunchedEffect(liked) {
        if (liked) {
            scale.snapTo(0.6f)
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.34f, stiffness = Spring.StiffnessMedium),
            )
        }
    }

    Row(
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            onClick = onToggle,
        ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Icon(
            painter = painterResource(
                if (liked) R.drawable.ic_heart_filled else R.drawable.ic_heart,
            ),
            contentDescription = if (liked) "좋아요 취소" else "좋아요",
            tint = if (liked) AbodeCoral else TextSecondary,
            modifier = Modifier
                .size(18.dp)
                .graphicsLayer {
                    scaleX = scale.value
                    scaleY = scale.value
                },
        )
        Text(
            text = count.toString(),
            color = if (liked) AbodeCoral else TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
