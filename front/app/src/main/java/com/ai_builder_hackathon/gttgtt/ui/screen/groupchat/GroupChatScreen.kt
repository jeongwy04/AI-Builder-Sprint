package com.ai_builder_hackathon.gttgtt.ui.screen.groupchat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.domain.model.ChatListItem
import com.ai_builder_hackathon.gttgtt.domain.model.ChatMessage
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.Photo
import com.ai_builder_hackathon.gttgtt.domain.model.SharedMemoryPreview
import com.ai_builder_hackathon.gttgtt.domain.model.asPhoto
import com.ai_builder_hackathon.gttgtt.ui.component.AppTopBar
import com.ai_builder_hackathon.gttgtt.ui.component.MemberAvatar
import com.ai_builder_hackathon.gttgtt.ui.component.PhotoImage
import com.ai_builder_hackathon.gttgtt.ui.component.SearchField
import com.ai_builder_hackathon.gttgtt.ui.component.TopBarButton
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreenDark
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreenSoft
import com.ai_builder_hackathon.gttgtt.ui.theme.DisplayFontFamily
import com.ai_builder_hackathon.gttgtt.ui.theme.ChatBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.DateChipBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.DateChipText
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.InputBarIcon
import com.ai_builder_hackathon.gttgtt.ui.theme.MessageTimeText
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.SenderNameText
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun GroupChatScreen(
    onBackClick: () -> Unit,
    onMemoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: GroupChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GroupChatContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onInputChange = viewModel::onInputChange,
        onSendClick = viewModel::onSendClick,
        onMemoryClick = onMemoryClick,
        onSearchClick = viewModel::onSearchClick,
        onSearchQueryChange = viewModel::onSearchQueryChange,
        onDismissSearch = viewModel::onDismissSearch,
        modifier = modifier,
    )
}

@Composable
private fun GroupChatContent(
    uiState: GroupChatUiState,
    onBackClick: () -> Unit,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onMemoryClick: (String) -> Unit = {},
    onSearchClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onDismissSearch: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // 채팅창을 처음 열었을 때는 스크롤 애니메이션 없이 바로 맨 아래로 가 있어야 자연스럽다.
    // (위에서부터 스크롤해 내려오는 게 보이면 어색하다.) 그 이후 새 메시지가 오면 그때는 애니메이션.
    var hasScrolledToInitialBottom by rememberSaveable { mutableStateOf(false) }

    // scrollToItem/animateScrollToItem 은 마지막 아이템의 "시작 지점"만 뷰포트로 끌어올릴
    // 뿐이라, 그 아이템이 뷰포트보다 작으면 진짜 맨 아래까지 못 내려간 것처럼 보인다
    // (MemoryDetailScreen 에서 겪은 것과 같은 문제). scrollBy/animateScrollBy 로 큰 값을
    // 더 밀어 넣어 남은 스크롤 가능 거리만큼 마저 내려간다 — 값이 커도 실제 남은 거리만
    // 소비하고 멈추므로 범위를 벗어나지 않는다.
    suspend fun jumpToBottom() {
        if (uiState.items.isEmpty()) return
        listState.scrollToItem(uiState.items.lastIndex)
        listState.scrollBy(2_000f)
    }

    suspend fun animateToBottom() {
        if (uiState.items.isEmpty()) return
        listState.animateScrollToItem(uiState.items.lastIndex)
        listState.animateScrollBy(2_000f)
    }

    LaunchedEffect(uiState.items.size) {
        // items 가 아직 비어 있으면(로딩 중) "처음 스크롤을 끝냈다"고 표시하지 않는다 —
        // 안 그러면 나중에 실제 메시지가 채워질 때 처음 진입인데도 애니메이션이 붙어버린다.
        if (uiState.items.isEmpty()) return@LaunchedEffect
        if (!hasScrolledToInitialBottom) {
            jumpToBottom()
            hasScrolledToInitialBottom = true
        } else {
            animateToBottom()
        }
    }

    // 키보드가 뜨거나 접힐 때(높이가 바뀔 때)마다 다시 맨 아래로 붙인다. imePadding() 이
    // 리스트 뷰포트를 줄이기만 할 뿐, 이미 맨 아래로 가 있던 스크롤 위치를 다시 계산해주지는
    // 않아서 — 키보드가 올라오면 방금까지 보이던 마지막 메시지가 화면 밖으로 밀려난다.
    val density = LocalDensity.current
    val imeBottomPx = WindowInsets.ime.getBottom(density)
    val isKeyboardOpen = imeBottomPx > 0

    LaunchedEffect(imeBottomPx) {
        jumpToBottom()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
            // enableEdgeToEdge() 를 쓰는 앱이라 시스템이 알아서 레이아웃을 밀어올려주지 않는다.
            // 이게 없으면 키보드가 InputBar(전송 버튼 포함)를 그대로 덮어버린다.
            .imePadding()
    ) {
        // 상단바만 목록 화면과 같은 배경을 써서 대화 영역과 층이 나뉜다.
        Box(modifier = Modifier.background(ScreenBackground)) {
            Column {
                AppTopBar(
                    title = uiState.groupName,
                    onBackClick = onBackClick,
                    centerTitle = true,
                    action = {
                        TopBarButton(
                            iconRes = R.drawable.ic_search,
                            contentDescription = "대화 검색",
                            background = BrandGreenSoft,
                            tint = BrandGreenDark,
                            onClick = onSearchClick,
                        )
                    },
                )

                if (uiState.isSearchActive) {
                    ChatSearchBar(
                        query = uiState.searchQuery,
                        onQueryChange = onSearchQueryChange,
                        onClose = onDismissSearch,
                        resultCount = if (uiState.searchQuery.isBlank()) null else uiState.messageCount,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
            }
        }

        Box(modifier = Modifier.weight(1f)) {
            when {
                uiState.isLoading -> LoadingState()
                uiState.errorMessage != null && uiState.items.isEmpty() ->
                    MessageState(uiState.errorMessage)

                else -> LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 18.dp, end = 18.dp, top = 2.dp, bottom = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    items(uiState.items, key = { it.key }) { item ->
                        when (item) {
                            is ChatListItem.DateHeader -> DateDivider(item.dateMillis)
                            is ChatListItem.Message -> MessageRow(item.message, onMemoryClick = onMemoryClick)
                        }
                    }
                }
            }
        }

        InputBar(
            value = uiState.input,
            onValueChange = onInputChange,
            onSendClick = onSendClick,
            canSend = uiState.canSend,
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 12.dp,
                // 🚀 키보드가 켜졌을 땐 하단 여백을 4dp로 확 줄여서 키보드에 딱 붙임
                bottom = if (isKeyboardOpen) 4.dp else 16.dp
            ),
        )
    }
}

/**
 * 상단바 검색 버튼을 누르면 그 아래 나타나는 검색창.
 * 입력하는 대로 [GroupChatViewModel] 이 메시지 목록을 필터링한다 — 여기선 그리기만 한다.
 */
@Composable
private fun ChatSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onClose: () -> Unit,
    resultCount: Int?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SearchField(
                query = query,
                onQueryChange = onQueryChange,
                placeholder = "대화 내용 검색",
                modifier = Modifier.weight(1f),
            )
            Icon(
                painter = painterResource(R.drawable.ic_x),
                contentDescription = "검색 닫기",
                tint = TextSecondary,
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClose)
                    .padding(4.dp),
            )
        }
        if (resultCount != null) {
            Text(
                text = if (resultCount > 0) "${resultCount}건 찾았어요" else "검색 결과가 없어요",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
            )
        }
    }
}

/** 시안 .datediv — 가운데 정렬된 회보라 칩 */
@Composable
private fun DateDivider(dateMillis: Long) {
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Text(
            text = formatDate(dateMillis),
            color = DateChipText,
            fontSize = 11.5.sp,
            fontWeight = FontWeight.ExtraBold,
            modifier = Modifier
                .clip(CircleShape)
                .background(DateChipBackground)
                .padding(horizontal = 14.dp, vertical = 6.dp),
        )
    }
}

/**
 * 메시지 한 줄.
 * 내 메시지는 오른쪽 정렬 + 아바타·이름 없이, 상대 메시지는 왼쪽 정렬 + 아바타·이름과 함께 그린다.
 */
@Composable
private fun MessageRow(message: ChatMessage, onMemoryClick: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
    ) {
        Row(
            // 시안 .msg max-width 80%
            modifier = Modifier.fillMaxWidth(0.8f),
            horizontalArrangement = if (message.isMine) Arrangement.End else Arrangement.Start,
            verticalAlignment = Alignment.Bottom,
        ) {
            if (message.isMine) {
                MessageTime(message.sentAtMillis)
                Spacer(Modifier.width(4.dp))
                Bubble(message, onMemoryClick = onMemoryClick)
            } else {
                MemberAvatar(
                    memberId = message.senderId,
                    size = 32.dp,
                    showRing = false,
                    modifier = Modifier.padding(top = 2.dp),
                )
                Spacer(Modifier.width(9.dp))
                Column {
                    Text(
                        text = message.senderName,
                        color = SenderNameText,
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(start = 3.dp, bottom = 5.dp),
                    )
                    Bubble(message, onMemoryClick = onMemoryClick)
                }
                Spacer(Modifier.width(4.dp))
                MessageTime(message.sentAtMillis)
            }
        }
    }
}

/**
 * 말풍선. 보낸 쪽에 따라 한쪽 모서리만 각지게 만든다.
 * 상대: 좌상단 6 / 나: 우상단 6
 */
@Composable
private fun Bubble(message: ChatMessage, onMemoryClick: (String) -> Unit) {
    val shape = if (message.isMine) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 6.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    } else {
        RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    }

    if (message.isSharedMemory) {
        val preview = requireNotNull(message.sharedMemory)
        SharedMemoryBubble(preview = preview, onClick = { onMemoryClick(preview.memoryId) })
        return
    }

    if (message.isPhoto) {
        // 사진 버블은 흰 액자에 사진을 끼운 형태라 패딩이 다르다.
        Box(
            modifier = Modifier
                .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp), clip = false)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceWhite)
                .padding(6.dp)
        ) {
            PhotoImage(
                photo = requireNotNull(message.photo),
                corner = 11.dp,
                modifier = Modifier.size(width = 150.dp, height = 112.dp),
            )
        }
        return
    }

    Text(
        text = message.text.orEmpty(),
        color = if (message.isMine) SurfaceWhite else TextPrimary,
        fontSize = 13.5.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 20.sp,
        modifier = Modifier
            .shadow(elevation = 3.dp, shape = shape, clip = false)
            .clip(shape)
            .background(if (message.isMine) BrandGreen else SurfaceWhite)
            .padding(horizontal = 14.dp, vertical = 11.dp),
    )
}

/** 피드에서 "보내기"로 공유된 기억. 탭하면 기억 상세로 이동한다. */
@Composable
private fun SharedMemoryBubble(preview: SharedMemoryPreview, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .width(220.dp)
            .shadow(elevation = 3.dp, shape = RoundedCornerShape(16.dp), clip = false)
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PhotoImage(
            photo = preview.photo ?: Photo(fallback = GradientTheme.BEACH),
            corner = 11.dp,
            modifier = Modifier.size(52.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "공유된 기억",
                color = BrandGreenDark,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = preview.title,
                color = TextPrimary,
                fontFamily = DisplayFontFamily,
                fontSize = 12.5.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatMemoryDate(preview.memoryDateMillis),
                color = TextSecondary,
                fontSize = 10.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun MessageTime(millis: Long) {
    Text(
        text = formatTime(millis),
        color = MessageTimeText,
        fontSize = 10.5.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(bottom = 2.dp),
    )
}

/** 시안 .inbar — 흰 알약 바 + 그린 전송 버튼 */
@Composable
private fun InputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    canSend: Boolean,
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
        Icon(
            painter = painterResource(R.drawable.ic_photo),
            contentDescription = "사진 첨부",
            tint = InputBarIcon,
            modifier = Modifier.size(20.dp),
        )

        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = "메시지 입력…",
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
                // Modifier.alpha() 로 흐리게 처리하면 graphicsLayer 합성이 하나 더 생기는데,
                // 일부 기기(특히 One UI)에서 IME 인셋이 바뀌는 재배치 시점과 겹치면
                // 이 레이어가 통째로 안 그려지는 경우가 있었다. 그래서 alpha 대신
                // 색 자체에 투명도를 넣어 배경색 한 번만 그리도록 바꿨다.
                .background(if (canSend) BrandGreen else BrandGreen.copy(alpha = 0.4f))
                .clickable(enabled = canSend, onClick = onSendClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_up),
                contentDescription = "보내기",
                tint = SurfaceWhite,
                modifier = Modifier.size(19.dp),
            )
        }
    }
}

@Composable
private fun LoadingState() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        CircularProgressIndicator(color = BrandGreen, modifier = Modifier.padding(top = 40.dp))
    }
}

@Composable
private fun MessageState(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.TopCenter) {
        Text(
            text = message,
            color = TextSecondary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 40.dp),
        )
    }
}

// 시안 표기: "2025년 12월 22일 (일)" / "6:31"
private val DateFormatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일 (E)", Locale.KOREAN)
private val TimeFormatter = DateTimeFormatter.ofPattern("h:mm", Locale.KOREAN)

// 공유된 기억 카드는 날짜 구분선보다 좁은 자리라 "2025.12.22" 처럼 짧게 쓴다.
private val MemoryDateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

private fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateFormatter)

private fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(TimeFormatter)

private fun formatMemoryDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(MemoryDateFormatter)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun GroupChatContentPreview() {
    val base = 1_766_413_860_000L // 2025-12-22 18:31 근처
    GttgttTheme {
        GroupChatContent(
            uiState = GroupChatUiState(
                isLoading = false,
                groupName = "강릉 여행",
                memberCount = 5,
                items = listOf(
                    ChatListItem.DateHeader(base),
                    ChatListItem.Message(
                        ChatMessage(
                            id = "1", archiveId = "a", senderId = "u-minji", senderName = "민지",
                            sentAtMillis = base,
                            text = "여러분! 저 밤에 치킨 먹다 울었던 사진 찾아봤는데 너무 웃겨 ㅋㅋ",
                        )
                    ),
                    ChatListItem.Message(
                        ChatMessage(
                            id = "2", archiveId = "a", senderId = "u-me", senderName = "나",
                            sentAtMillis = base + 120_000,
                            text = "ㅋㅋㅋㅋㅋ 진짜 추억이다 그때", isMine = true,
                        )
                    ),
                    ChatListItem.Message(
                        ChatMessage(
                            id = "3", archiveId = "a", senderId = "u-jihun", senderName = "지훈",
                            sentAtMillis = base + 180_000,
                            photo = GradientTheme.FOOD.asPhoto(),
                        )
                    ),
                ),
            ),
            onBackClick = {},
            onInputChange = {},
            onSendClick = {},
        )
    }
}
