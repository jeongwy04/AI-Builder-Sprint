package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.BeachGradient
import com.ai_builder_hackathon.gttgtt.ui.theme.FamilyGradient
import com.ai_builder_hackathon.gttgtt.ui.theme.FoodGradient
import com.ai_builder_hackathon.gttgtt.ui.theme.ForestGradient
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.LaptopGradient
import com.ai_builder_hackathon.gttgtt.ui.theme.NightGradient
import com.ai_builder_hackathon.gttgtt.ui.theme.SeaGradient

// 시안 .gthumb : 60x60, radius 18
private val ThumbnailSize = 60.dp
private val ThumbnailCorner = 18.dp

/**
 * 그룹 카드 왼쪽의 그라디언트 썸네일.
 * 실제 대표 사진이 생기면 이 Box 안에 Coil AsyncImage 를 얹고 그라디언트는 폴백으로 남긴다.
 */
@Composable
fun GroupThumbnail(
    theme: GradientTheme,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(ThumbnailSize)
            .clip(RoundedCornerShape(ThumbnailCorner))
            .background(gradientOf(theme))
    )
}

/** 그라디언트 테마 → Brush. 썸네일 말고 사진 placeholder 에서도 쓴다. */
fun gradientOf(theme: GradientTheme): Brush = theme.toBrush()

private fun GradientTheme.toBrush(): Brush = when (this) {
    GradientTheme.BEACH -> BeachGradient
    GradientTheme.FOREST -> ForestGradient
    GradientTheme.FOOD -> FoodGradient
    GradientTheme.LAPTOP -> LaptopGradient
    GradientTheme.FAMILY -> FamilyGradient
    GradientTheme.SEA -> SeaGradient
    GradientTheme.NIGHT -> NightGradient
}

@Preview(showBackground = true)
@Composable
private fun GroupThumbnailPreview() {
    GttgttTheme {
        GroupThumbnail(theme = GradientTheme.BEACH)
    }
}
