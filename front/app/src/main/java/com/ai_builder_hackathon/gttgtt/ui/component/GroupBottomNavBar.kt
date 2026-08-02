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
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.ui.theme.AbodeBlue
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite

private val BarHeight = 60.dp
private val ItemWidth = 58.dp
private val ItemHeight = 42.dp

/**
 * 그룹 피드 하단의 흰색 알약 플로팅 바.
 * AI 추억 찾기 · 그룹 채팅 · 기억(게시물) 남기기 세 가지를 담는다.
 *
 * AI/채팅은 선택된 항목이 그린 타원으로 차오르고 아이콘이 흰색으로 반전되는 토글이다.
 * "기억 남기기"는 토글이 아니라 항상 눌러서 바로 작성 화면으로 넘어가는 액션이라
 * 선택 상태 없이 항상 흰 배경 + 초록 아이콘으로 고정한다.
 */
@Composable
fun GroupBottomNavBar(
    isAiSelected: Boolean,
    onAiClick: () -> Unit,
    onChatClick: () -> Unit,
    onCreateMemoryClick: () -> Unit,
    modifier: Modifier = Modifier,
    isChatSelected: Boolean = false,
    chatUnreadCount: Int = 0,
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
            iconRes = R.drawable.ic_solar_spark,
            contentDescription = "AI 추억 찾기",
            isSelected = isAiSelected,
            onClick = onAiClick,
        )
        // 가운데 자리 — 토글이 아니라 항상 흰 배경 + 초록 아이콘으로 고정된 액션 버튼.
        CreateMemoryNavItem(onClick = onCreateMemoryClick)
        NavItem(
            iconRes = R.drawable.ic_message_2,
            contentDescription = "그룹 채팅",
            isSelected = isChatSelected,
            onClick = onChatClick,
            unreadCount = chatUnreadCount,
        )
    }
}

/** 타원형 토글 항목. 선택되면 그린으로 차오르고 아이콘이 흰색이 된다. */
@Composable
private fun NavItem(
    iconRes: Int,
    contentDescription: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    unreadCount: Int = 0,
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

    NavItemBox(
        iconRes = iconRes,
        contentDescription = contentDescription,
        background = background,
        iconTint = iconTint,
        onClick = onClick,
        unreadCount = unreadCount,
    )
}

/**
 * "기억 남기기" 전용 항목. AI/채팅과 달리 토글이 아니라 항상 같은 모습이라
 * 색을 애니메이션할 필요가 없다 — 흰 배경 + 초록 아이콘 고정.
 */
@Composable
private fun CreateMemoryNavItem(onClick: () -> Unit) {
    NavItemBox(
        iconRes = R.drawable.ic_plus,
        contentDescription = "기억 남기기",
        background = SurfaceWhite,
        iconTint = BrandGreen,
        onClick = onClick,
    )
}

@Composable
private fun NavItemBox(
    iconRes: Int,
    contentDescription: String,
    background: Color,
    iconTint: Color,
    onClick: () -> Unit,
    unreadCount: Int = 0,
) {
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

        if (unreadCount > 0) {
            NavBadge(
                count = unreadCount,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 2.dp, end = 6.dp),
            )
        }
    }
}

/** 네비게이션 아이콘 우측 상단에 얹는 안 읽음 배지. 타원이 아닌 정원(正圓) 고정. 99개 넘으면 "99+". */
@Composable
private fun NavBadge(count: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(AbodeBlue),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            color = SurfaceWhite,
            fontSize = 8.sp,
            fontWeight = FontWeight.Black,
            lineHeight = 8.sp,
            style = LocalTextStyle.current.copy(
                platformStyle = PlatformTextStyle(includeFontPadding = false),
            ),
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
            onCreateMemoryClick = {},
            chatUnreadCount = 5,
        )
    }
}
