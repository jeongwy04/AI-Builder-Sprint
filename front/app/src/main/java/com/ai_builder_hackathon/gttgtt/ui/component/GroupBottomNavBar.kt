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

private val BarHeight = 60.dp
private val ItemWidth = 58.dp
private val ItemHeight = 42.dp

/**
 * 그룹 피드 하단의 흰색 알약 플로팅 바.
 * 채팅 이동과 AI 추억 찾기 두 가지만 담는다.
 *
 * 선택된 항목은 그린 타원으로 차오르고 아이콘이 흰색으로 반전된다.
 */
@Composable
fun GroupBottomNavBar(
    isAiSelected: Boolean,
    onAiClick: () -> Unit,
    onChatClick: () -> Unit,
    modifier: Modifier = Modifier,
    isChatSelected: Boolean = false,
) {
    Row(
        modifier = modifier
            .height(BarHeight)
            // 높이의 절반이 radius 가 되는 완전한 알약(타원) 형태
            .shadow(elevation = 16.dp, shape = CircleShape, clip = false)
            .clip(CircleShape)
            .background(SurfaceWhite)
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        NavItem(
            iconRes = R.drawable.ic_sparkles,
            contentDescription = "AI 추억 찾기",
            isSelected = isAiSelected,
            onClick = onAiClick,
        )
        NavItem(
            iconRes = R.drawable.ic_message_2,
            contentDescription = "그룹 채팅",
            isSelected = isChatSelected,
            onClick = onChatClick,
        )
    }
}

/** 타원형 항목. 선택되면 그린으로 차오르고 아이콘이 흰색이 된다. */
@Composable
private fun NavItem(
    iconRes: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    // 탭 순간 색이 뚝 바뀌지 않고 부드럽게 차오르게.
    val background by animateColorAsState(
        targetValue = if (isSelected) BrandGreen else Color.Transparent,
        label = "navItemBackground",
    )
    val iconTint by animateColorAsState(
        targetValue = if (isSelected) SurfaceWhite else BrandGreen,
        label = "navItemIcon",
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
private fun GroupBottomNavBarPreview() {
    GttgttTheme {
        GroupBottomNavBar(
            isAiSelected = true,
            onAiClick = {},
            onChatClick = {},
        )
    }
}
