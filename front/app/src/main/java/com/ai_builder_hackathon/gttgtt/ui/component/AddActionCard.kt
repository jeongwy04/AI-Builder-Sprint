package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_builder_hackathon.gttgtt.ui.theme.DarkGlassBorder
import com.ai_builder_hackathon.gttgtt.ui.theme.DarkGlassTint
import com.ai_builder_hackathon.gttgtt.ui.theme.GlassBlueGlow
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import dev.chrisbanes.haze.HazeState

private val CardShape = SmoothCornerShape(radius = 28.dp, smoothness = 0.6f)
private val CardShadow = Color(0x55000000)
private val Divider = Color.White.copy(alpha = 0.08f)

/**
 * '+' 를 누르면 뜨는 다크 글래스 액션 카드 (이미지 2).
 * - 프로스트 글래스(뒤 콘텐츠 블러) 위에 좌상단에서 번지는 파란 방사형 글로우.
 * - "그룹방 만들기"(Bold 20sp) / "코드로 참여하기"(Medium 18sp).
 */
@Composable
fun AddActionCard(
    hazeState: HazeState,
    onCreateGroup: () -> Unit,
    onJoinByCode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 좌상단에서 번지는 파란 빛. 반투명 → 투명이라 유리 블러가 그대로 비친다.
    val blueGlow = Brush.radialGradient(
        colorStops = arrayOf(
            0f to GlassBlueGlow.copy(alpha = 0.45f),
            0.7f to Color.Transparent,
        ),
        center = Offset(0f, 0f),
        radius = 620f,
    )

    Column(
        modifier = modifier
            .shadow(elevation = 22.dp, shape = CardShape, clip = false, spotColor = CardShadow, ambientColor = CardShadow)
            .frostedGlass(hazeState, CardShape, tint = DarkGlassTint, blurRadius = 30.dp, borderColor = DarkGlassBorder)
            .background(brush = blueGlow, shape = CardShape)
            .padding(vertical = 6.dp),
    ) {
        ActionRow(text = "그룹방 만들기", fontSize = 20.sp, fontWeight = FontWeight.Bold, onClick = onCreateGroup)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp)
                .height(1.dp)
                .background(Divider),
        )
        ActionRow(text = "코드로 참여하기", fontSize = 18.sp, fontWeight = FontWeight.Medium, onClick = onJoinByCode)
    }
}

@Composable
private fun ActionRow(
    text: String,
    fontSize: androidx.compose.ui.unit.TextUnit,
    fontWeight: FontWeight,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        color = SurfaceWhite,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 22.dp, vertical = 16.dp),
    )
}
