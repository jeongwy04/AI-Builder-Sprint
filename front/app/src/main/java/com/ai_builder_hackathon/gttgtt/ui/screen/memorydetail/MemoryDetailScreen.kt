package com.ai_builder_hackathon.gttgtt.ui.screen.memorydetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
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
import com.ai_builder_hackathon.gttgtt.ui.theme.LikeChipBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.LikeChipText
import com.ai_builder_hackathon.gttgtt.ui.theme.MoreIcon
import com.ai_builder_hackathon.gttgtt.ui.theme.ParticipantNameText
import com.ai_builder_hackathon.gttgtt.ui.theme.PhotoCountBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackgroundBrush
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary
import com.ai_builder_hackathon.gttgtt.ui.theme.TopBarIcon
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val SidePadding = 20.dp
private val DangerColor = Color(0xFFB64B39)

@Composable
fun MemoryDetailScreen(
    onBackClick: () -> Unit,
    onEditClick: (archiveId: String, memoryId: String) -> Unit,
    onDeleted: () -> Unit,
    onChatClick: (archiveId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoryDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.isDeleted) {
        if (uiState.isDeleted) onDeleted()
    }

    // 공유가 성공할 때마다 카운트가 늘어난다 — 채팅방으로 넘어가서 방금 보낸 메시지를 바로 보여준다.
    // 이동 직후 onShareHandled() 로 카운트를 되돌려야 한다 — 안 그러면 채팅방에서 뒤로가기로
    // 돌아왔을 때 이 화면이 재조립되며 "값이 여전히 그대로"인 걸 보고 또 튕겨버린다
    // (GroupFeedScreen 에서 겪었던 것과 같은 원인).
    LaunchedEffect(uiState.shareSuccessCount) {
        if (uiState.shareSuccessCount > 0) {
            uiState.memory?.let { onChatClick(it.archiveId) }
            viewModel.onShareHandled()
        }
    }

    MemoryDetailContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onPhotoIndexChange = viewModel::onPhotoIndexChange,
        onCommentInputChange = viewModel::onCommentInputChange,
        onSubmitComment = viewModel::onSubmitComment,
        onMoreClick = viewModel::onMoreClick,
        onDismissOptionsSheet = viewModel::onDismissOptionsSheet,
        onEditClick = {
            uiState.memory?.let { memory ->
                viewModel.onDismissOptionsSheet()
                onEditClick(memory.archiveId, memory.id)
            }
        },
        onDeleteClick = viewModel::onDeleteClick,
        onDismissDeleteConfirm = viewModel::onDismissDeleteConfirm,
        onConfirmDelete = viewModel::onConfirmDelete,
        onLikeClick = viewModel::onLikeClick,
        onShareClick = viewModel::onShareClick,
        onCommentFocusHandled = viewModel::onCommentFocusHandled,
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
    onMoreClick: () -> Unit,
    onDismissOptionsSheet: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismissDeleteConfirm: () -> Unit,
    onConfirmDelete: () -> Unit,
    onLikeClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onCommentFocusHandled: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val commentFocusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    // DetailBody 의 LazyColumn 과 같은 스크롤 상태를 들고 있어야, 댓글 입력창에
    // 포커스가 갈 때 이 화면(MemoryDetailContent)에서 맨 아래(댓글 영역)로 스크롤시킬 수 있다.
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 댓글 입력창에 포커스가 갈 때마다(자동 포커스든, 사용자가 직접 탭했든) 목록을 맨 아래로
    // 스크롤해서 댓글 영역이 키보드 위로 보이게 한다.
    //
    // animateScrollToItem(lastIndex) 만으로는 마지막 아이템의 "시작 지점"만 뷰포트 위로
    // 끌어올릴 뿐이라, 그 아이템이 뷰포트보다 작으면 조금만 내려간 것처럼 보인다.
    // 그래서 먼저 마지막 아이템 근처로 이동한 뒤, animateScrollBy 로 큰 값을 더 밀어 넣어
    // 실제 스크롤 끝(진짜 맨 아래)까지 마저 내려간다 — animateScrollBy 는 남은 스크롤
    // 가능 거리만큼만 소비하고 멈추기 때문에 값이 커도 범위를 벗어나지 않는다.
    fun scrollToComments() {
        coroutineScope.launch {
            val lastIndex = listState.layoutInfo.totalItemsCount - 1
            if (lastIndex >= 0) {
                listState.animateScrollToItem(lastIndex)
                listState.animateScrollBy(2_000f)
            }
        }
    }

    // 피드 카드의 댓글 버튼으로 들어왔을 때만 켜져 있다 (Route.MemoryDetail.focusComment).
    // memory 가 로드돼 CommentInputBar 가 실제로 그려진 다음에야 포커스를 줄 수 있어
    // memory 유무도 같이 key 로 건다. 처리 후에는 onCommentFocusHandled() 로 꺼야
    // 화면이 재조립될 때(예: 수정 후 복귀) 키보드가 또 뜨지 않는다.
    LaunchedEffect(uiState.memory != null, uiState.shouldFocusCommentInput) {
        if (uiState.memory != null && uiState.shouldFocusCommentInput) {
            commentFocusRequester.requestFocus()
            keyboardController?.show()
            scrollToComments()
            onCommentFocusHandled()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackgroundBrush)
            // GroupChatScreen/AiChatSheet 와 같은 이유 — 없으면 키보드가 댓글 입력바를 그대로 덮는다.
            .imePadding()
    ) {
        DetailTopBar(onBackClick = onBackClick, onMoreClick = onMoreClick)

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
                    onLikeClick = onLikeClick,
                    onShareClick = onShareClick,
                    isSharing = uiState.isSharing,
                    shareError = uiState.shareError,
                    listState = listState,
                    modifier = Modifier.weight(1f),
                )
                CommentInputBar(
                    value = uiState.commentInput,
                    onValueChange = onCommentInputChange,
                    onSubmit = onSubmitComment,
                    canSubmit = uiState.canSubmitComment,
                    focusRequester = commentFocusRequester,
                    // 사용자가 입력창을 직접 탭했을 때도(자동 포커스 경로와 별개로) 댓글
                    // 영역이 보이도록 같은 스크롤 로직을 탄다.
                    onFocused = ::scrollToComments,
                    modifier = Modifier.padding(16.dp),
                )
            }
        }
    }

    if (uiState.isOptionsSheetOpen) {
        MemoryOptionsSheet(
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick,
            onDismiss = onDismissOptionsSheet,
        )
    }

    if (uiState.isDeleteConfirmOpen) {
        DeleteMemoryConfirmDialog(
            isDeleting = uiState.isDeleting,
            errorMessage = uiState.deleteError,
            onConfirm = onConfirmDelete,
            onDismiss = onDismissDeleteConfirm,
        )
    }
}

/**
 * 제목 없는 상단바. 뒤로가기와 더보기만 둔다.
 * 사진과 제목이 바로 아래에 크게 나오므로 상단에 제목을 또 적으면 중복이다.
 */
@Composable
private fun DetailTopBar(onBackClick: () -> Unit, onMoreClick: () -> Unit) {
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
            onClick = onMoreClick,
        )
    }
}

/** 더보기 버튼을 누르면 뜨는 수정/삭제 메뉴. GroupFeedScreen 의 GroupSettingsSheet 와 같은 모양. */
@Composable
private fun MemoryOptionsSheet(
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceWhite)
                .padding(vertical = 8.dp),
        ) {
            Text(
                text = "기억 관리",
                color = TextPrimary,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            OptionsMenuRow(label = "수정", onClick = onEditClick)
            OptionsMenuRow(label = "삭제", labelColor = DangerColor, onClick = onDeleteClick)
        }
    }
}

@Composable
private fun OptionsMenuRow(
    label: String,
    onClick: () -> Unit,
    labelColor: Color = TextPrimary,
) {
    Text(
        text = label,
        color = labelColor,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
    )
}

/** 삭제 확인 다이얼로그. GroupFeedScreen 의 DeleteGroupConfirmDialog 와 같은 모양. */
@Composable
private fun DeleteMemoryConfirmDialog(
    isDeleting: Boolean,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(SurfaceWhite)
                .padding(24.dp),
        ) {
            Text(
                text = "기억을 삭제할까요?",
                color = TextPrimary,
                fontSize = 19.sp,
                fontWeight = FontWeight.Black,
            )
            Spacer(Modifier.height(10.dp))
            Text(
                text = "사진과 댓글이 함께 삭제되고, 되돌릴 수 없어요.",
                color = TextSecondary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                lineHeight = 19.sp,
            )

            if (errorMessage != null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = errorMessage,
                    color = DangerColor,
                    fontSize = 12.5.sp,
                    fontWeight = FontWeight.SemiBold,
                )
            }

            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(ScreenBackground)
                        .clickable(enabled = !isDeleting, onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(text = "취소", color = TextSecondary, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(DangerColor)
                        .clickable(enabled = !isDeleting, onClick = onConfirm),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            color = SurfaceWhite,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(20.dp),
                        )
                    } else {
                        Text(text = "삭제", color = SurfaceWhite, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }
            }
        }
    }
}

@Composable
private fun DetailBody(
    memory: MemoryDetail,
    currentPhotoIndex: Int,
    onPhotoIndexChange: (Int) -> Unit,
    onLikeClick: () -> Unit,
    onShareClick: () -> Unit,
    isSharing: Boolean,
    shareError: String?,
    listState: LazyListState = rememberLazyListState(),
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        state = listState,
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
                // 날짜와 같은 줄, 같은 높이에 오른쪽 정렬로 좋아요 · 채팅방 보내기.
                // 시안엔 없어 피드 카드(PostFooter/ShareButton)와 같은 모양으로 맞춘다.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = formatDate(memory.memoryDateMillis),
                        color = BrandGreenDark,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                    )
                    LikeAndShareActions(
                        likedByMe = memory.likedByMe,
                        likeCount = memory.likeCount,
                        onLikeClick = onLikeClick,
                        onShareClick = onShareClick,
                        isSharing = isSharing,
                    )
                }
                if (shareError != null) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = shareError,
                        color = DangerColor,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
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

/**
 * 날짜와 같은 줄, 같은 높이에 오른쪽으로 놓는 좋아요 칩 + 채팅방 보내기 버튼.
 * GroupFeedScreen 의 PostFooter 와 같은 모양이다.
 */
@Composable
private fun LikeAndShareActions(
    likedByMe: Boolean,
    likeCount: Int,
    onLikeClick: () -> Unit,
    onShareClick: () -> Unit,
    isSharing: Boolean,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(LikeChipBackground)
                .clickable(onClick = onLikeClick)
                .padding(horizontal = 13.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                painter = painterResource(if (likedByMe) R.drawable.ic_heart_filled else R.drawable.ic_heart),
                contentDescription = "좋아요",
                tint = LikeChipText,
                modifier = Modifier.size(15.dp),
            )
            Text(
                text = likeCount.toString(),
                color = LikeChipText,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
            )
        }

        Spacer(Modifier.width(10.dp))

        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(if (isSharing) BrandGreen.copy(alpha = 0.4f) else BrandGreen)
                .clickable(enabled = !isSharing, onClick = onShareClick),
            contentAlignment = Alignment.Center,
        ) {
            if (isSharing) {
                CircularProgressIndicator(
                    color = SurfaceWhite,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(16.dp),
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_send),
                    contentDescription = "채팅방에 보내기",
                    tint = SurfaceWhite,
                    modifier = Modifier.size(15.dp),
                )
            }
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
    focusRequester: FocusRequester? = null,
    /** 입력창이 포커스를 얻을 때(자동이든 사용자가 직접 탭했든) 호출된다 — 댓글 영역 스크롤용. */
    onFocused: () -> Unit = {},
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
                modifier = Modifier
                    .fillMaxWidth()
                    .let { base -> focusRequester?.let { base.focusRequester(it) } ?: base }
                    .onFocusChanged { state -> if (state.isFocused) onFocused() },
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
            onMoreClick = {},
            onDismissOptionsSheet = {},
            onEditClick = {},
            onDeleteClick = {},
            onDismissDeleteConfirm = {},
            onConfirmDelete = {},
        )
    }
}
