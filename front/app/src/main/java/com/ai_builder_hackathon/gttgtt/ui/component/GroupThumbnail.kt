package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.BeachGradient
import com.ai_builder_hackathon.gttgtt.ui.theme.FamilyGradient
import com.ai_builder_hackathon.gttgtt.ui.theme.FoodGradient
import com.ai_builder_hackathon.gttgtt.ui.theme.ForestGradient
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.LaptopGradient
import com.ai_builder_hackathon.gttgtt.ui.theme.NightGradient
import com.ai_builder_hackathon.gttgtt.ui.theme.SeaGradient

private val ThumbnailSize = 60.dp
private val ThumbnailCorner = 20.dp

/**
 * 그룹 썸네일 — 파스텔 배경 위에 큰 이모지 스티커 (만화 느낌).
 * 실제 대표 사진이 생기면 이 Box 안에 Coil AsyncImage 를 얹고 이 스티커는 폴백으로 남긴다.
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
            .background(tintOf(theme)),
        contentAlignment = Alignment.Center,
    ) {
        // 프로필 이미지 등록 전 placeholder. 이미지가 붙으면 여기 Coil AsyncImage 를 얹는다.
        Icon(
            painter = painterResource(R.drawable.ic_photo),
            contentDescription = null,
            tint = TextSecondary,
            modifier = Modifier.size(22.dp),
        )
    }
}

/** 테마별 파스텔 배경. */
private fun tintOf(theme: GradientTheme): Color = when (theme) {
    GradientTheme.BEACH -> Color(0xFFFDE7D3)
    GradientTheme.FOREST -> Color(0xFFE3F2E1)
    GradientTheme.FOOD -> Color(0xFFFFF3BF)
    GradientTheme.LAPTOP -> Color(0xFFE4EEFE)
    GradientTheme.FAMILY -> Color(0xFFFFE6E6)
    GradientTheme.SEA -> Color(0xFFDFF1FA)
    GradientTheme.NIGHT -> Color(0xFFEBE6FA)
}

/** 그라디언트 테마 → Brush. 사진 placeholder 에서 쓴다. */
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
        GroupThumbnail(theme = GradientTheme.FOREST)
    }
}
