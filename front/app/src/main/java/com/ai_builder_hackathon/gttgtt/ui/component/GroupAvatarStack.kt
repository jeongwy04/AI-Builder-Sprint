package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ai_builder_hackathon.gttgtt.ui.theme.AvatarGradients
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreenDark
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreenSoft
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import kotlin.math.absoluteValue

// 시안 .stack .av : 24x24, 흰 테두리 2px, 좌측 -8 겹침
private val StackAvatarSize = 24.dp
private val AvatarOverlap = (-8).dp
private val AvatarRing = 2.dp

/**
 * 겹쳐진 멤버 아바타 + "+N" 배지.
 *
 * @param avatarUrlById memberId → 프로필 사진 URL. 키가 없거나 값이 null 인 멤버는
 * id 해시로 고정 배정한 그라디언트를 대신 보여준다.
 * @param avatarPathById memberId → 프로필 사진 storage path. Coil 캐시 키 고정용 — [MemberAvatar] 참고.
 */
@Composable
fun GroupAvatarStack(
    memberIds: List<String>,
    hiddenCount: Int,
    modifier: Modifier = Modifier,
    avatarUrlById: Map<String, String> = emptyMap(),
    avatarPathById: Map<String, String> = emptyMap(),
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(AvatarOverlap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        memberIds.forEach { id ->
            MemberAvatar(
                memberId = id,
                size = StackAvatarSize,
                imageUrl = avatarUrlById[id],
                imagePath = avatarPathById[id],
            )
        }
        if (hiddenCount > 0) {
            OverflowBadge(count = hiddenCount)
        }
    }
}

/**
 * 멤버 한 명의 원형 아바타.
 * @param showRing 겹쳐 쌓을 때만 흰 테두리를 두른다. 단독으로 쓸 때는 불필요.
 * @param imageUrl 실제 프로필 사진 URL. null 이면(대부분의 멤버가 아직 이렇다) [memberId] 해시로
 * 고정 배정한 그라디언트를 대신 보여준다 — 이 파라미터 하나만 채우면 되고 호출부는 그대로 둘 수 있다.
 * @param imagePath 프로필 사진 storage path. signed URL 은 발급될 때마다 토큰이 바뀌어 URL 문자열
 * 자체를 캐시 키로 쓰면 화면을 오갈 때마다 Coil 이 다시 받는다(PhotoImage 와 같은 이유) — 이 값이
 * 있으면 그걸 캐시 키로 고정해서 이미 받아둔 파일을 재사용한다.
 */
@Composable
fun MemberAvatar(
    memberId: String,
    size: Dp,
    modifier: Modifier = Modifier,
    showRing: Boolean = true,
    imageUrl: String? = null,
    imagePath: String? = null,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(gradientFor(memberId))
            .then(
                if (showRing) {
                    Modifier.border(width = AvatarRing, color = SurfaceWhite, shape = CircleShape)
                } else {
                    Modifier
                }
            )
    ) {
        if (imageUrl != null) {
            val platformContext = LocalPlatformContext.current
            val request = remember(imageUrl, imagePath) {
                ImageRequest.Builder(platformContext)
                    .data(imageUrl)
                    .crossfade(true)
                    .apply {
                        imagePath?.let { path ->
                            memoryCacheKey(path)
                            diskCacheKey(path)
                        }
                    }
                    .build()
            }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

/** 시안의 .plusn — 진한 그린이 아니라 연한 그린 배경 + 진한 그린 글자. */
@Composable
private fun OverflowBadge(count: Int) {
    Box(
        modifier = Modifier
            .size(StackAvatarSize)
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
