package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.DarkGlassBorder
import com.ai_builder_hackathon.gttgtt.ui.theme.DarkGlassTint
import com.ai_builder_hackathon.gttgtt.ui.theme.GlassTopHighlight
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import dev.chrisbanes.haze.HazeState

/** 홈 하단 탭. 카메라는 촬영 액션, 그룹은 현재 목록 화면. */
enum class HomeTab { CAMERA, GROUP }

private val BarHeight = 60.dp
private val NavShadow = Color(0x40000000)
private val UnselectedContent = Color.White.copy(alpha = 0.55f)

/**
 * 홈 하단 [카메라 | 그룹] 세그먼트 캡슐.
 * - 다크 프로스트 글래스(뒤 목록이 블러되어 비침, [HazeState] 공유).
 * - 선택 칩이 250ms 로 슬라이딩, 상단 가장자리에 흰색 하이라이트로 3D 광원 느낌.
 */
@Composable
fun HomeBottomNavBar(
    selected: HomeTab,
    hazeState: HazeState,
    onCameraClick: () -> Unit,
    onGroupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // 캡슐 유지(완전 라운드) + 게시물 박스와 같은 RoundedCornerShape 계열(스퀴클 아님).
    val barShape = RoundedCornerShape(BarHeight / 2)
    val chipShape = RoundedCornerShape((BarHeight - 12.dp) / 2)

    // -1 = 왼쪽(카메라), +1 = 오른쪽(그룹). 탭이 바뀌면 250ms 로 미끄러진다.
    val bias by animateFloatAsState(
        targetValue = if (selected == HomeTab.CAMERA) -1f else 1f,
        animationSpec = tween(durationMillis = 250),
        label = "segmentSlide",
    )

    Box(
        modifier = modifier
            .height(BarHeight)
            .shadow(elevation = 18.dp, shape = barShape, clip = false, spotColor = NavShadow, ambientColor = NavShadow)
            .frostedGlass(hazeState, barShape, tint = DarkGlassTint, blurRadius = 30.dp, borderColor = DarkGlassBorder)
            .padding(6.dp),
    ) {
        // 슬라이딩 하이라이트 — 선택된 칩. 상단 하이라이트 테두리로 광원 느낌.
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.5f)
                .align(BiasAlignment(horizontalBias = bias, verticalBias = 0f))
                .clip(chipShape)
                .background(BrandGreen)
                .border(width = 1.dp, color = GlassTopHighlight, shape = chipShape),
        )
        Row(modifier = Modifier.fillMaxSize()) {
            SegmentLabel(
                iconRes = R.drawable.ic_camera,
                label = "카메라",
                selected = selected == HomeTab.CAMERA,
                onClick = onCameraClick,
                modifier = Modifier.weight(1f),
            )
            SegmentLabel(
                iconRes = R.drawable.ic_message_2,
                label = "그룹",
                selected = selected == HomeTab.GROUP,
                onClick = onGroupClick,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SegmentLabel(
    iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val content by animateColorAsState(
        targetValue = if (selected) SurfaceWhite else UnselectedContent,
        animationSpec = tween(durationMillis = 250),
        label = "segmentContent",
    )
    Row(
        modifier = modifier
            .fillMaxHeight()
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(7.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = label,
            tint = content,
            modifier = Modifier.size(19.dp),
        )
        Text(
            text = label,
            color = content,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
