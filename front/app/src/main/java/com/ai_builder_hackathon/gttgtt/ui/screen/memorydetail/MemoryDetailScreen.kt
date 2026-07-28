package com.ai_builder_hackathon.gttgtt.ui.screen.memorydetail

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.domain.model.Comment
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.Photo
import com.ai_builder_hackathon.gttgtt.domain.model.asPhoto
import com.ai_builder_hackathon.gttgtt.domain.model.MemoryDetail
import com.ai_builder_hackathon.gttgtt.domain.model.Participant
import com.ai_builder_hackathon.gttgtt.ui.component.MemberAvatar
import com.ai_builder_hackathon.gttgtt.ui.component.PhotoImage
import com.ai_builder_hackathon.gttgtt.ui.component.TopBarButton
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreenDark
import com.ai_builder_hackathon.gttgtt.ui.theme.CommentMetaText
import com.ai_builder_hackathon.gttgtt.ui.theme.CommentText
import com.ai_builder_hackathon.gttgtt.ui.theme.DetailBodyText
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.InputBarIcon
import com.ai_builder_hackathon.gttgtt.ui.theme.MoreIcon
import com.ai_builder_hackathon.gttgtt.ui.theme.ParticipantNameText
import com.ai_builder_hackathon.gttgtt.ui.theme.PhotoCountBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary
import com.ai_builder_hackathon.gttgtt.ui.theme.TopBarIcon
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SidePadding = 20.dp

@Composable
fun MemoryDetailScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    MemoryDetailContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onPhotoIndexChange = viewModel::onPhotoIndexChange,
        onCommentInputChange = viewModel::onCommentInputChange,
        onSubmitComment = viewModel::onSubmitComment,
        modifier = modifier,
    )
}

@Composable
private fun MemoryDetailContent(
    uiState: MemoryDetailUiState,
    onBackClick: () -> Unit,
    onPhotoIndexChange: (Int) -> Unit,
    onCommentInputChange: (String) -> Unit,
    onSubmitComment: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            // GroupChatScreen/AiChatSheet 와 같은 이유 — 없으면 키보드가 댓글 입력바를 그대로 덮는다.
            .imePadding()
    ) {
        DetailTopBar(onBackClick = onBackClick)

        when {
            uiState.isLoading -> CenterBox { CircularProgressIndicator(color = BrandGreen) }
            uiState.memory == null -> CenterBox {
                Text(
                    text = uiState.errorMessage ?: "기억을 불러오지 못했습니다.",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            else -> {
                DetailBody(
                    memory = uiState.memory,
                    currentPhotoIndex = uiState.currentPhotoIndex,
                    onPhotoIndexChange = onPhotoIndexChange,
                    modifier = Modifier.weight(1f),
                )
                CommentInputBar(
                    value = uiState.commentInput,
                    onValueChange = onCommentInputChange,
                    onSubmit = onSubmitComment,
                    canSubmit = uiState.canSubmitComment,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }
}

/**
 * 제목 없는 상단바. 뒤로가기와 더보기만 둔다.
 * 사진과 제목이 바로 아래에 크게 나오므로 상단에 제목을 또 적으면 중복이다.
 */
@Composable
private fun DetailTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SidePadding, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        TopBarButton(
            iconRes = R.drawable.ic_chevron_left,
            contentDescription = "뒤로",
            background = SurfaceWhite,
            tint = TopBarIcon,
            onClick = onBackClick,
        )
        TopBarButton(
            iconRes = R.drawable.ic_dots,
            contentDescription = "더보기",
            background = SurfaceWhite,
            tint = MoreIcon,
            onClick = { /* TODO: 수정·삭제 메뉴 */ },
        )
    }
}

@Composable
private fun DetailBody(
    memory: MemoryDetail,
    currentPhotoIndex: Int,
    onPhotoIndexChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(bottom = 24.dp),
    ) {
        item(key = "hero") {
            HeroPhotos(
                photos = memory.photos,
                currentIndex = currentPhotoIndex,
                onIndexChange = onPhotoIndexChange,
            )
        }

        item(key = "head") {
            Column(modifier = Modifier.padding(horizontal = SidePadding)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = formatDate(memory.memoryDateMillis),
                    color = BrandGreenDark,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                )
                Spacer(Modifier.height(7.dp))
                Text(
                    text = memory.title,
                    style = TextStyle(
                        color = TextPrimary,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.03).em,
                        lineHeight = 28.sp,
                    ),
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    text = memory.body,
                    color = DetailBodyText,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 21.6.sp,
                )
            }
        }

        item(key = "people-label") { SectionLabel("함께한 사람") }
        item(key = "people") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = SidePadding),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                items(memory.participants, key = { it.id }) { ParticipantItem(it) }
            }
        }

        if (memory.relatedPhotos.isNotEmpty()) {
            item(key = "related-label") {
                SectionLabel(
                    text = "관련 사진",
                    trailing = "전체 보기",
                    onTrailingClick = { /* TODO: 그룹 앨범 화면 */ },
                )
            }
            item(key = "related") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = SidePadding),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(memory.relatedPhotos.size) { index ->
                        PhotoImage(
                            photo = memory.relatedPhotos[index],
                            corner = 16.dp,
                            modifier = Modifier.size(74.dp),
                        )
                    }
                }
            }
        }

        item(key = "comment-label") { SectionLabel("댓글 ${memory.comments.size}") }

        if (memory.comments.isEmpty()) {
            item(key = "comment-empty") {
                Text(
                    text = "첫 댓글을 남겨보세요.",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = SidePadding),
                )
            }
        } else {
            items(memory.comments, key = { it.id }) { comment ->
                CommentCard(comment)
                Spacer(Modifier.height(10.dp))
            }
        }
    }
}

/** 시안 .hero-photo — 좌우로 넘기고 우상단에 "1 / 8" 이 뜬다. */
@Composable
private fun HeroPhotos(
    photos: List<Photo>,
    currentIndex: Int,
    onIndexChange: (Int) -> Unit,
) {
    val pagerState = rememberPagerState(pageCount = { photos.size })

    // 스와이프로 바뀐 페이지를 ViewModel 로 올려 상태를 한 곳에 모은다.
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.currentPage }.collect(onIndexChange)
    }

    Box(
        modifier = Modifier
            .padding(start = SidePadding, end = SidePadding, top = 6.dp)
            .fillMaxWidth()
            .height(210.dp)
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(24.dp), clip = false)
            .clip(RoundedCornerShape(24.dp))
    ) {
        HorizontalPager(state = pagerState, modifier = Modifier.fillMaxSize()) { page ->
            PhotoImage(
                photo = photos[page],
                corner = 0.dp,
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (photos.size > 1) {
            Text(
                text = "${currentIndex + 1} / ${photos.size}",
                color = SurfaceWhite,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp)
                    .clip(CircleShape)
                    .background(PhotoCountBackground)
                    .padding(horizontal = 11.dp, vertical = 5.dp),
            )
        }
    }
}

@Composable
private fun SectionLabel(
    text: String,
    trailing: String? = null,
    onTrailingClick: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = SidePadding, end = SidePadding, top = 20.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = text,
            color = TextPrimary,
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
        )
        if (trailing != null) {
            Text(
                text = trailing,
                color = BrandGreenDark,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onTrailingClick?.invoke() },
            )
        }
    }
}

@Composable
private fun ParticipantItem(participant: Participant) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        MemberAvatar(memberId = participant.id, size = 44.dp, showRing = false)
        Spacer(Modifier.height(5.dp))
        Text(
            text = participant.name,
            color = ParticipantNameText,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** 시안 .cmt */
@Composable
private fun CommentCard(comment: Comment) {
    Column(
        modifier = Modifier
            .padding(horizontal = SidePadding)
            .fillMaxWidth()
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(18.dp), clip = false)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceWhite)
            .padding(horizontal = 15.dp, vertical = 13.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp),
        ) {
            MemberAvatar(memberId = comment.authorId, size = 30.dp, showRing = false)
            Text(
                text = comment.authorName,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = formatShortDate(comment.createdAtMillis),
                color = CommentMetaText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = comment.text,
            color = CommentText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            lineHeight = 19.5.sp,
        )
        Spacer(Modifier.height(9.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "답글 달기",
                color = CommentMetaText,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.clickable { /* TODO: 대댓글 */ },
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_heart),
                    contentDescription = null,
                    tint = CommentMetaText,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = comment.likeCount.toString(),
                    color = CommentMetaText,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun CommentInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    canSubmit: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 6.dp, shape = RoundedCornerShape(22.dp), clip = false)
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceWhite)
            .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = "댓글을 입력하세요…",
                    color = TextSecondary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                textStyle = LocalTextStyle.current.copy(
                    color = TextPrimary,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Medium,
                ),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_mood_smile),
            contentDescription = "이모지",
            tint = InputBarIcon,
            modifier = Modifier.size(20.dp),
        )
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(15.dp))
                // GroupChatScreen 과 같은 이유로 alpha() 대신 색 자체에 투명도를 넣는다.
                .background(if (canSubmit) BrandGreen else BrandGreen.copy(alpha = 0.4f))
                .clickable(enabled = canSubmit, onClick = onSubmit),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_up),
                contentDescription = "등록",
                tint = SurfaceWhite,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().padding(top = 60.dp),
        contentAlignment = Alignment.TopCenter,
        content = { content() },
    )
}

private val DateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
private val ShortDateFormatter = DateTimeFormatter.ofPattern("MM.dd")

private fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateFormatter)

private fun formatShortDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(ShortDateFormatter)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MemoryDetailContentPreview() {
    GttgttTheme {
        MemoryDetailContent(
            uiState = MemoryDetailUiState(
                isLoading = false,
                memory = MemoryDetail(
                    id = "mem-chicken",
                    archiveId = "a",
                    memoryDateMillis = 1_766_361_600_000L,
                    title = "시험 끝나고 치킨 먹다 울었던 날",
                    body = "치킨 먹다 졸업 얘기 나와서 다 같이 울었던 날. 정말 잊지 못할 추억 ❤️",
                    photos = listOf(
                        GradientTheme.FOOD.asPhoto(),
                        GradientTheme.NIGHT.asPhoto(),
                        GradientTheme.BEACH.asPhoto(),
                    ),
                    participants = listOf(
                        Participant("u-me", "나"),
                        Participant("u-minji", "민지"),
                        Participant("u-hyunwoo", "현우"),
                        Participant("u-jihun", "지훈"),
                        Participant("u-seoyeon", "소연"),
                    ),
                    relatedPhotos = listOf(
                        GradientTheme.SEA.asPhoto(),
                        GradientTheme.FOOD.asPhoto(),
                        GradientTheme.FOREST.asPhoto(),
                        GradientTheme.NIGHT.asPhoto(),
                    ),
                    comments = listOf(
                        Comment(
                            "c1", "u-minji", "민지",
                            "진짜 그때 생각하면 아직도 울컥 😢",
                            1_766_361_600_000L, 2,
                        ),
                    ),
                ),
            ),
            onBackClick = {},
            onPhotoIndexChange = {},
            onCommentInputChange = {},
            onSubmitComment = {},
        )
    }
}
