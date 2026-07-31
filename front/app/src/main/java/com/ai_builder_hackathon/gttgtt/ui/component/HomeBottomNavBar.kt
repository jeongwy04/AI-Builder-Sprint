package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
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
import com.ai_builder_hackathon.gttgtt.ui.theme.GlassTopHighlight
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import dev.chrisbanes.haze.HazeState

/** 홈 하단 탭. 카메라는 촬영 액션, 그룹은 현재 목록 화면. */
enum class HomeTab { CAMERA, GROUP }

private val BarHeight = 58.dp
// 흰색 프로스트 글래스 + 파란 탭(선택 시 파랑 채움/흰 글자, 미선택은 파란 글자).
private val NavShadow = Color(0x1A4A6FA5)
private val NavGlassTint = Color.White.copy(alpha = 0.60f)
private val NavGlassBorder = Color.White.copy(alpha = 0.70f)
private val UnselectedContent = BrandGreen

/**
 * 홈 하단 [카메라 | 그룹] 세그먼트 캡슐 — SETLOG 처럼 각 탭이 글자를 감싸는 콤팩트한 형태.
 * 바는 두 탭 폭에 딱 맞게 줄어들어(가운데 정렬) 빈 공간이 남지 않는다.
 * 다크 프로스트 글래스(뒤 목록이 블러되어 비침) + 선택 탭에 상단 하이라이트로 3D 광원 느낌.
 */
@Composable
fun HomeBottomNavBar(
    selected: HomeTab,
    hazeState: HazeState,
    onCameraClick: () -> Unit,
    onGroupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val barShape = RoundedCornerShape(BarHeight / 2)
    Row(
        modifier = modifier
            .height(BarHeight)
            .shadow(elevation = 18.dp, shape = barShape, clip = false, spotColor = NavShadow, ambientColor = NavShadow)
            .frostedGlass(hazeState, barShape, tint = NavGlassTint, blurRadius = 30.dp, borderColor = NavGlassBorder)
            .padding(6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SegmentTab(
            iconRes = R.drawable.ic_camera,
            label = "카메라",
            selected = selected == HomeTab.CAMERA,
            onClick = onCameraClick,
        )
        SegmentTab(
            iconRes = R.drawable.ic_message_2,
            label = "그룹",
            selected = selected == HomeTab.GROUP,
            onClick = onGroupClick,
        )
    }
}

/** 글자를 감싸는(content-sized) 탭 칩. 선택되면 그린으로 차오르고 상단 하이라이트가 붙는다. */
@Composable
private fun SegmentTab(
    iconRes: Int,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val chipShape = RoundedCornerShape((BarHeight - 12.dp) / 2)
    val background by animateColorAsState(
        targetValue = if (selected) BrandGreen else Color.Transparent,
        animationSpec = tween(durationMillis = 250),
        label = "segmentBackground",
    )
    val content by animateColorAsState(
        targetValue = if (selected) SurfaceWhite else UnselectedContent,
        animationSpec = tween(durationMillis = 250),
        label = "segmentContent",
    )
    Row(
        modifier = Modifier
            .fillMaxHeight()
            .clip(chipShape)
            .background(background)
            .then(
                if (selected) Modifier.border(1.dp, GlassTopHighlight, chipShape) else Modifier,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
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
