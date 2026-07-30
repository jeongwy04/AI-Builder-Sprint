package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite

/** 홈 하단 탭. 카메라는 촬영 액션, 그룹은 현재 목록 화면. */
enum class HomeTab { CAMERA, GROUP }

// GroupBottomNavBar(그룹 피드 하단 바)와 정확히 같은 치수 — 두 화면을 오가도 바 크기가 안 흔들리게.
private val BarHeight = 60.dp
private val ItemWidth = 58.dp
private val ItemHeight = 42.dp

/**
 * 홈 하단 [카메라 | 그룹] 플로팅 바.
 *
 * 그룹 피드 하단 바([GroupBottomNavBar])와 같은 디자인 언어로 통일했다 — 다크 프로스트
 * 글래스 + 슬라이딩 세그먼트 대신, 흰 알약(pill) 배경 위에 원형 아이템을 나란히 두고
 * 선택된 항목만 그린으로 차오르며 아이콘이 흰색으로 반전된다.
 */
@Composable
fun HomeBottomNavBar(
    selected: HomeTab,
    onCameraClick: () -> Unit,
    onGroupClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .height(BarHeight)
            // 높이의 절반이 radius 인 완전한 알약(타원) 형태 — GroupBottomNavBar와 동일.
            .shadow(elevation = 16.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(SurfaceWhite)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        HomeNavItem(
            iconRes = R.drawable.ic_camera,
            contentDescription = "카메라",
            isSelected = selected == HomeTab.CAMERA,
            onClick = onCameraClick,
        )
        HomeNavItem(
            iconRes = R.drawable.ic_message_2,
            contentDescription = "그룹",
            isSelected = selected == HomeTab.GROUP,
            onClick = onGroupClick,
        )
    }
}

/**
 * 타원형 토글 항목. GroupBottomNavBar.NavItem 과 동일한 애니메이션·색 규칙(선택 시
 * 그린 배경 + 흰 아이콘, 아닐 때는 투명 배경 + 그린 아이콘)을 그대로 따른다.
 */
@Composable
private fun HomeNavItem(
    iconRes: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val background by animateColorAsState(
        targetValue = if (isSelected) BrandGreen else Color.Transparent,
        label = "homeNavItemBackground",
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) SurfaceWhite else BrandGreen,
        label = "homeNavItemIcon",
    )

    Box(
        modifier = Modifier
            .width(ItemWidth)
            .height(ItemHeight)
            .clip(CircleShape)
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(23.dp),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF5F6F8)
@Composable
private fun HomeBottomNavBarPreview() {
    GttgttTheme {
        HomeBottomNavBar(
            selected = HomeTab.GROUP,
            onCameraClick = {},
            onGroupClick = {},
        )
    }
}
