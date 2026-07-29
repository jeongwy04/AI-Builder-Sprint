package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// 유리(글래스) 표면 톤. 배경 그라데이션이 살짝 비쳐 보이도록 반투명 흰색을 쓴다.
// ⚠️ 진짜 배경 블러(backdrop blur)는 minSdk 26 에서 안정적으로 지원되지 않아,
//    반투명 표면 + 상단 광택 + 얇은 하이라이트 보더로 유리 느낌을 근사한다.
private val GlassTint = Color.White.copy(alpha = 0.60f)
private val GlassSheenTop = Color.White.copy(alpha = 0.55f)
private val GlassSheenBottom = Color.White.copy(alpha = 0.05f)
private val GlassBorder = Color.White.copy(alpha = 0.70f)

/**
 * 반투명 유리 표면. 배경이 은은하게 비쳐 깊이감을 준다.
 * 그림자는 이 modifier 바깥에서 같은 [shape] 로 먼저 얹는다(그래야 그림자가 잘려나가지 않는다).
 *
 * 사용 예:
 * ```
 * Modifier
 *     .shadow(12.dp, shape, spotColor = ..., ambientColor = ...)
 *     .glassSurface(shape)
 * ```
 */
fun Modifier.glassSurface(
    shape: Shape,
    tint: Color = GlassTint,
): Modifier = this
    .clip(shape)
    .background(tint)
    // 위쪽이 더 밝게 빛나는 광택 — 유리에 빛이 닿는 느낌.
    .background(Brush.verticalGradient(listOf(GlassSheenTop, GlassSheenBottom)))
    .border(width = 1.dp, color = GlassBorder, shape = shape)
