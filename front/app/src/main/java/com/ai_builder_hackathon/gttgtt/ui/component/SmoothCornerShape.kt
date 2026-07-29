package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.min

/**
 * iOS 식 "연속 곡률(corner smoothing / squircle)" 모서리.
 *
 * Compose 기본 [androidx.compose.foundation.shape.RoundedCornerShape] 은 모서리를 정확한 원호로
 * 깎아서 곡률이 급하게 꺾인다. 여기서는 직선 구간에서 모서리로 넘어갈 때 큐빅 베지어로 완만하게
 * 이어 붙여, 카드·검색바·네비바가 더 부드럽게 보이도록 한다.
 *
 * @param radius     모서리 반경
 * @param smoothness 곡선이 직선 쪽으로 얼마나 더 뻗을지(0=원호에 가까움, 0.6 정도가 iOS 느낌).
 *
 * 외부 의존성 없이 Path 로 직접 그린다 (CLAUDE.md §14 — 새 의존성 금지).
 */
class SmoothCornerShape(
    private val radius: Dp,
    private val smoothness: Float = 0.6f,
) : Shape {

    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density,
    ): Outline {
        val maxR = min(size.width, size.height) / 2f
        val r = min(with(density) { radius.toPx() }, maxR)
        // 곡선이 시작되는 지점 — 모서리에서 r*(1+smoothness) 만큼 직선을 따라 물러난 곳.
        val ext = min(r * (1f + smoothness), maxR)
        val w = size.width
        val h = size.height

        val path = Path().apply {
            moveTo(ext, 0f)
            lineTo(w - ext, 0f)
            cubicTo(w - r, 0f, w, r, w, ext)             // 우상
            lineTo(w, h - ext)
            cubicTo(w, h - r, w - r, h, w - ext, h)      // 우하
            lineTo(ext, h)
            cubicTo(r, h, 0f, h - r, 0f, h - ext)        // 좌하
            lineTo(0f, ext)
            cubicTo(0f, r, r, 0f, ext, 0f)               // 좌상
            close()
        }
        return Outline.Generic(path)
    }
}
