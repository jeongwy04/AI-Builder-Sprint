package com.ai_builder_hackathon.gttgtt.ui.screen.grouplist

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ai_builder_hackathon.gttgtt.domain.model.ArchiveSummary
import com.ai_builder_hackathon.gttgtt.domain.model.GroupTheme
import com.ai_builder_hackathon.gttgtt.ui.component.GroupAvatarStack
import com.ai_builder_hackathon.gttgtt.ui.component.GroupThumbnail
import com.ai_builder_hackathon.gttgtt.ui.component.SearchField
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.CardBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextMuted
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

// 시안 .pad / .search / .list 의 좌우 여백은 모두 20
private val ScreenPadding = 20.dp
private const val VISIBLE_AVATAR_COUNT = 3

/**
 * S1 그룹(채팅방) 목록.
 *
 * ViewModel 을 여기서 주입받고, 실제 UI 는 상태만 받는 [GroupListContent] 가 그린다.
 * 이렇게 나눠야 @Preview 가 Hilt 없이 동작한다.
 */
@Composable
fun GroupListScreen(
    onGroupClick: (archiveId: String) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GroupListContent(
        uiState = uiState,
        onQueryChange = viewModel::onQueryChange,
        onGroupClick = onGroupClick,
        onProfileClick = onProfileClick,
        modifier = modifier,
    )
}

@Composable
private fun GroupListContent(
    uiState: GroupListUiState,
    onQueryChange: (String) -> Unit,
    onGroupClick: (String) -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
    ) {
        HomeHeader(
            onProfileClick = onProfileClick,
            modifier = Modifier.padding(
                start = ScreenPadding,
                end = ScreenPadding,
                top = 6.dp,
                bottom = 2.dp,
            ),
        )

        SearchField(
            query = uiState.query,
            onQueryChange = onQueryChange,
            placeholder = "채팅방 검색",
            modifier = Modifier.padding(
                start = ScreenPadding,
                end = ScreenPadding,
                top = 14.dp,
            ),
        )

        Spacer(Modifier.height(18.dp))

        when {
            uiState.isLoading -> LoadingState()
            uiState.errorMessage != null -> MessageState(uiState.errorMessage)
            uiState.isEmpty -> MessageState(
                if (uiState.query.isBlank()) "아직 참여 중인 채팅방이 없어요." else "검색 결과가 없어요."
            )

            else -> GroupList(
                groups = uiState.groups,
                onGroupClick = onGroupClick,
            )
        }
    }
}

/**
 * 로고(왼쪽) + MY 버튼(오른쪽 끝).
 * 시안에 있던 알림 벨은 제거했다.
 */
@Composable
private fun HomeHeader(
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        // 시안 .logo b : 25px / 900 / italic / letter-spacing -.04em
        // Pretendard 를 res/font/ 에 넣으면 fontFamily 만 지정하면 된다.
        Text(
            text = "그때그때",
            style = TextStyle(
                color = BrandGreen,
                fontSize = 25.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = (-0.04).em,
            ),
        )
        MyButton(onClick = onProfileClick)
    }
}

/** 시안 기준 46x40 의 둥근 사각형. 완전한 알약 형태가 아니다. */
@Composable
private fun MyButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(width = 46.dp, height = 40.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(BrandGreen)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "MY",
            style = TextStyle(
                color = SurfaceWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Black,
                fontStyle = FontStyle.Italic,
                letterSpacing = 0.02.em,
            ),
        )
    }
}

@Composable
private fun GroupList(
    groups: List<ArchiveSummary>,
    onGroupClick: (String) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(
            start = ScreenPadding,
            end = ScreenPadding,
            bottom = 24.dp,
        ),
        // 시안 .list gap: 11
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        items(groups, key = { it.id }) { group ->
            GroupCard(group = group, onClick = { onGroupClick(group.id) })
        }
    }
}

@Composable
private fun GroupCard(
    group: ArchiveSummary,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            // 시안 .gcard : radius 22, padding 13, gap 13
            .clip(RoundedCornerShape(22.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        GroupThumbnail(theme = group.theme)

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = group.name,
                style = TextStyle(
                    color = TextPrimary,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.02).em,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = group.lastMessagePreview,
                color = TextSecondary,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))

            val shownMembers = group.memberIds.take(VISIBLE_AVATAR_COUNT)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                GroupAvatarStack(
                    memberIds = shownMembers,
                    // "+N" 의 N 은 전체 인원에서 실제로 그린 아바타 수를 뺀 값이다.
                    hiddenCount = group.hiddenMemberCount(shownMembers.size),
                )
                Text(
                    text = formatTime(group.lastActivityAtMillis),
                    color = TextMuted,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        CircularProgressIndicator(
            color = BrandGreen,
            modifier = Modifier.padding(top = 40.dp),
        )
    }
}

@Composable
private fun MessageState(message: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 40.dp),
        )
    }
}

// "오후 8:45" 형식. 한국어 로케일을 고정해 기기 설정과 무관하게 시안대로 보이게 한다.
private val TimeFormatter = DateTimeFormatter.ofPattern("a h:mm", Locale.KOREAN)

private fun formatTime(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(TimeFormatter)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun GroupListContentPreview() {
    GttgttTheme {
        GroupListContent(
            uiState = GroupListUiState(
                isLoading = false,
                groups = listOf(
                    ArchiveSummary(
                        id = "1",
                        name = "강릉 여행",
                        lastMessagePreview = "민지: 바다 너무 예뻤어",
                        lastActivityAtMillis = 0L,
                        theme = GroupTheme.BEACH,
                        memberIds = listOf("u-minji", "u-seoyeon", "u-jaehun"),
                        totalMemberCount = 6,
                    ),
                    ArchiveSummary(
                        id = "2",
                        name = "우리 대학 동기들",
                        lastMessagePreview = "현우: 다음에 또 보자 ㅋㅋ",
                        lastActivityAtMillis = 0L,
                        theme = GroupTheme.FOREST,
                        memberIds = listOf("u-hyunwoo", "u-doyun"),
                        totalMemberCount = 6,
                    ),
                    ArchiveSummary(
                        id = "3",
                        name = "가족",
                        lastMessagePreview = "엄마: 주말에 만나자~",
                        lastActivityAtMillis = 0L,
                        theme = GroupTheme.FAMILY,
                        memberIds = listOf("u-mom", "u-dad"),
                        totalMemberCount = 4,
                    ),
                    ArchiveSummary(
                        id = "4",
                        name = "개발팀",
                        lastMessagePreview = "지훈: PR 리뷰 부탁드려요!",
                        lastActivityAtMillis = 0L,
                        theme = GroupTheme.LAPTOP,
                        memberIds = listOf("u-jihun", "u-sora", "u-taeyang"),
                        totalMemberCount = 6,
                    ),
                ),
            ),
            onQueryChange = {},
            onGroupClick = {},
            onProfileClick = {},
        )
    }
}
