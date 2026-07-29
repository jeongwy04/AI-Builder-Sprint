package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.Icon
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import coil3.compose.AsyncImage
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.ui.theme.DisplayFontFamily
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
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
 * 그룹 썸네일 — 우선순위: 실제 대표 사진 → 이름 이니셜 → 사진 placeholder.
 * - [imageUrl] 이 있으면 Coil 로 대표 사진을 그린다.
 * - 없고 [initial] 이 있으면 테마 그라디언트 위에 이름 첫 글자를 얹은 세련된 이니셜 아바타.
 * - 둘 다 없으면 파스텔 배경 + 사진 아이콘.
 */
@Composable
fun GroupThumbnail(
    theme: GradientTheme,
    modifier: Modifier = Modifier,
    imageUrl: String? = null,
    initial: String? = null,
) {
    val showInitial = imageUrl == null && !initial.isNullOrBlank()
    Box(
        modifier = modifier
            .size(ThumbnailSize)
            .clip(RoundedCornerShape(ThumbnailCorner))
            // 이니셜 아바타는 또렷하게 보이도록 진한 그라디언트, 그 외엔 파스텔 톤.
            // Brush/Color 는 공통 타입이 Any 라 삼항으로 넘기면 background() 오버로드가 안 맞는다.
            // 그래서 Modifier 단위로 분기한다.
            .then(
                if (showInitial) {
                    Modifier.background(gradientOf(theme))
                } else {
                    Modifier.background(tintOf(theme))
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        when {
            imageUrl != null -> AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )

            showInitial -> Text(
                text = initial!!.trim().take(1).uppercase(),
                color = SurfaceWhite,
                fontFamily = DisplayFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
            )

            else -> Icon(
                painter = painterResource(R.drawable.ic_photo),
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(22.dp),
            )
        }
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
