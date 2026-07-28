package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ai_builder_hackathon.gttgtt.ui.theme.AvatarGradients
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreenDark
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreenSoft
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import kotlin.math.absoluteValue

// 시안 .stack .av : 24x24, 흰 테두리 2px, 좌측 -8 겹침
private val AvatarSize = 24.dp
private val AvatarOverlap = (-8).dp
private val AvatarRing = 2.dp

/**
 * 겹쳐진 멤버 아바타 + "+N" 배지.
 *
 * 프로필 이미지가 아직 없어서 id 해시로 그라디언트만 채운다.
 * 이미지가 생기면 [MemberDot] 안을 AsyncImage 로 바꾸면 나머지는 그대로 쓸 수 있다.
 */
@Composable
fun GroupAvatarStack(
    memberIds: List<String>,
    hiddenCount: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AvatarOverlap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        memberIds.forEach { id ->
            MemberDot(memberId = id)
        }
        if (hiddenCount > 0) {
            OverflowBadge(count = hiddenCount)
        }
    }
}

@Composable
private fun MemberDot(memberId: String) {
    Box(
        modifier = Modifier
            .size(AvatarSize)
            .clip(CircleShape)
            .background(gradientFor(memberId))
            .border(width = AvatarRing, color = SurfaceWhite, shape = CircleShape)
    )
}

/** 시안의 .plusn — 진한 그린이 아니라 연한 그린 배경 + 진한 그린 글자. */
@Composable
private fun OverflowBadge(count: Int) {
    Box(
        modifier = Modifier
            .size(AvatarSize)
            .clip(CircleShape)
            .background(BrandGreenSoft)
            .border(width = AvatarRing, color = SurfaceWhite, shape = CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "+$count",
            color = BrandGreenDark,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.ExtraBold,
        )
    }
}

/** 같은 사용자는 항상 같은 색을 갖도록 id 해시로 팔레트를 고른다. */
private fun gradientFor(memberId: String) =
    AvatarGradients[memberId.hashCode().absoluteValue % AvatarGradients.size]

@Preview(showBackground = true)
@Composable
private fun GroupAvatarStackPreview() {
    GttgttTheme {
        GroupAvatarStack(
            memberIds = listOf("u-minji", "u-seoyeon", "u-jaehun"),
            hiddenCount = 3,
        )
    }
}
