package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer

/**
 * Abode 스타일 스프링 프레스 — 누르면 살짝 눌렸다가 통통 튀어 돌아온다 (이펙트 2).
 *
 * 카드·버튼·타일 어디에나 `Modifier.bounceClick { ... }` 로 붙인다.
 * 기본 리플 대신 스케일 스프링만 준다 (indication = null).
 */
fun Modifier.bounceClick(
    pressedScale: Float = 0.94f,
    onClick: () -> Unit,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMedium),
        label = "bounceScale",
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.clickable(
        interactionSource = interaction,
        indication = null,
        onClick = onClick,
    )
}
