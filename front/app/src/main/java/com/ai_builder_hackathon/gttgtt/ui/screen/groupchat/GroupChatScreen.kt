package com.ai_builder_hackathon.gttgtt.ui.screen.groupchat

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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.domain.model.ChatListItem
import com.ai_builder_hackathon.gttgtt.domain.model.ChatMessage
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.ui.component.AppTopBar
import com.ai_builder_hackathon.gttgtt.ui.component.MemberAvatar
import com.ai_builder_hackathon.gttgtt.ui.component.TopBarButton
import com.ai_builder_hackathon.gttgtt.ui.component.gradientOf
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreenDark
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreenSoft
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
    modifier: Modifier = Modifier,
    viewModel: GroupChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    GroupChatContent(
        uiState = uiState,
        onBackClick = onBackClick,
        onInputChange = viewModel::onInputChange,
        onSendClick = viewModel::onSendClick,
        modifier = modifier,
    )
}

@Composable
private fun GroupChatContent(
    uiState: GroupChatUiState,
    onBackClick: () -> Unit,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // 메시지가 늘어나면 항상 최신 메시지가 보이게 내려준다.
    LaunchedEffect(uiState.items.size) {
        if (uiState.items.isNotEmpty()) {
            listState.animateScrollToItem(uiState.items.lastIndex)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ChatBackground)
    ) {
        // 상단바만 목록 화면과 같은 배경을 써서 대화 영역과 층이 나뉜다.
        Box(modifier = Modifier.background(ScreenBackground)) {
            AppTopBar(
                title = uiState.groupName,
                subtitle = "멤버 ${uiState.memberCount}명",
                onBackClick = onBackClick,
                action = {
                    TopBarButton(
                        iconRes = R.drawable.ic_search,
                        contentDescription = "대화 검색",
                        background = BrandGreenSoft,
                        tint = BrandGreenDark,
                        onClick = { /* TODO: 대화 검색 시안 나오면 연결 */ },
                    )
                },
            )
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
                            is ChatListItem.Message -> MessageRow(item.message)
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
            modifier = Modifier.padding(16.dp),
        )
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
private fun MessageRow(message: ChatMessage) {
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
                Bubble(message)
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
                    Bubble(message)
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
private fun Bubble(message: ChatMessage) {
    val shape = if (message.isMine) {
        RoundedCornerShape(topStart = 18.dp, topEnd = 6.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
    } else {
        RoundedCornerShape(topStart = 6.dp, topEnd = 18.dp, bottomEnd = 18.dp, bottomStart = 18.dp)
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
            Box(
                modifier = Modifier
                    .size(width = 150.dp, height = 112.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(gradientOf(requireNotNull(message.photo)))
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
                .background(BrandGreen)
                // 보낼 내용이 없으면 눌러도 아무 일이 없다는 걸 흐리게 표시한다.
                .alpha(if (canSend) 1f else 0.4f)
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

private fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateFormatter)

private fun formatTime(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(TimeFormatter)

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
                            photo = GradientTheme.FOOD,
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
