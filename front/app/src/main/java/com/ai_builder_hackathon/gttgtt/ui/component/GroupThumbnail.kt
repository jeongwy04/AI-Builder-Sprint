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
import com.ai_builder_hackathon.gttgtt.domain.model.GroupTheme
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
    theme: GroupTheme,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .size(ThumbnailSize)
            .clip(RoundedCornerShape(ThumbnailCorner))
            .background(theme.toBrush())
    )
}

private fun GroupTheme.toBrush(): Brush = when (this) {
    GroupTheme.BEACH -> BeachGradient
    GroupTheme.FOREST -> ForestGradient
    GroupTheme.FOOD -> FoodGradient
    GroupTheme.LAPTOP -> LaptopGradient
    GroupTheme.FAMILY -> FamilyGradient
    GroupTheme.SEA -> SeaGradient
    GroupTheme.NIGHT -> NightGradient
}

@Preview(showBackground = true)
@Composable
private fun GroupThumbnailPreview() {
    GttgttTheme {
        GroupThumbnail(theme = GroupTheme.BEACH)
    }
}
