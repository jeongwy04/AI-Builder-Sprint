package com.ai_builder_hackathon.gttgtt.ui.screen.chat

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.domain.model.AiMessage
import com.ai_builder_hackathon.gttgtt.domain.model.GradientTheme
import com.ai_builder_hackathon.gttgtt.domain.model.asPhoto
import com.ai_builder_hackathon.gttgtt.domain.model.MemoryHit
import com.ai_builder_hackathon.gttgtt.ui.component.PhotoImage
import com.ai_builder_hackathon.gttgtt.ui.theme.AiFabGradient
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreenDark
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

/**
 * AI 추억 찾기 패널.
 *
 * ModalBottomSheet 를 쓰지 않는 이유: 모달 시트는 별도 윈도우에 그려져서
 * 하단 네비게이션 바가 그 위로 올라올 수 없다. 피드 레이아웃 안의 일반 패널로 두면
 * 바가 패널 위에 얹혀 함께 올라간다. 높이는 호출부가 정한다.
 *
 * ViewModel 이 GroupFeed 목적지에 스코프되므로 패널을 닫아도 대화가 유지된다.
 */
@Composable
fun AiChatPanel(
    onMemoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AiChatViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .background(ScreenBackground)
    ) {
        AiChatSheetContent(
            uiState = uiState,
            onInputChange = viewModel::onInputChange,
            onSendClick = viewModel::onSendClick,
            onMemoryClick = onMemoryClick,
        )
    }
}

@Composable
private fun AiChatSheetContent(
    uiState: AiChatUiState,
    onInputChange: (String) -> Unit,
    onSendClick: () -> Unit,
    onMemoryClick: (String) -> Unit,
) {
    val listState = rememberLazyListState()

    LaunchedEffect(uiState.messages.size, uiState.isThinking) {
        if (uiState.messages.isNotEmpty()) {
            listState.animateScrollToItem(uiState.messages.lastIndex)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            // GroupChatScreen 과 같은 이유 — edge-to-edge 라 키보드가 알아서 안 밀어준다.
            .imePadding(),
    ) {
        Spacer(Modifier.height(14.dp))
        SheetHeader()

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            items(uiState.messages, key = { it.id }) { message ->
                AiMessageRow(message = message, onMemoryClick = onMemoryClick)
            }
            if (uiState.isThinking) {
                item(key = "thinking") { ThinkingBubble() }
            }
            if (uiState.errorMessage != null) {
                item(key = "error") {
                    Text(
                        text = uiState.errorMessage,
                        color = TextSecondary,
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }

        AiInputBar(
            value = uiState.input,
            onValueChange = onInputChange,
            onSendClick = onSendClick,
            canSend = uiState.canSend,
            modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 16.dp),
        )
    }
}

@Composable
private fun SheetHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        SparkleAvatar(size = 32.dp)
        Column {
            Text(
                text = "AI 추억 찾기",
                color = TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
            )
            Text(
                text = "이 그룹의 기록에서만 찾아요",
                color = TextSecondary,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun SparkleAvatar(size: Dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(size / 3))
            .background(AiFabGradient),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_sparkles),
            contentDescription = null,
            tint = SurfaceWhite,
            modifier = Modifier.size(size * 0.6f),
        )
    }
}

@Composable
private fun AiMessageRow(
    message: AiMessage,
    onMemoryClick: (String) -> Unit,
) {
    if (message.isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = message.text,
                color = SurfaceWhite,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
                modifier = Modifier
                    .widthIn(max = 260.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp, topEnd = 6.dp,
                            bottomEnd = 18.dp, bottomStart = 18.dp,
                        )
                    )
                    .background(BrandGreen)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            )
        }
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(9.dp),
    ) {
        SparkleAvatar(size = 28.dp)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = message.text,
                color = TextPrimary,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Medium,
                lineHeight = 20.sp,
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 6.dp, topEnd = 18.dp,
                            bottomEnd = 18.dp, bottomStart = 18.dp,
                        )
                    )
                    .background(SurfaceWhite)
                    .padding(horizontal = 14.dp, vertical = 11.dp),
            )

            // 도구가 돌려준 결과 안에서만 보여준다 (CLAUDE.md §6.3).
            message.results.forEach { hit ->
                MemoryHitCard(hit = hit, onClick = { onMemoryClick(hit.memoryId) })
            }
        }
    }
}

@Composable
private fun MemoryHitCard(hit: MemoryHit, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(CardBackground)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        PhotoImage(
            photo = hit.thumbnail,
            corner = 13.dp,
            modifier = Modifier.size(44.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = hit.title,
                color = TextPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = formatDate(hit.memoryDateMillis),
                color = TextMuted,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }
        // 오른쪽 화살표가 따로 없어 chevron-left 를 뒤집어 쓴다.
        Icon(
            painter = painterResource(R.drawable.ic_chevron_left),
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier
                .size(16.dp)
                .rotate(180f),
        )
    }
}

@Composable
private fun ThinkingBubble() {
    Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
        SparkleAvatar(size = 28.dp)
        Text(
            text = "기억을 찾고 있어요…",
            color = TextSecondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier
                .clip(
                    RoundedCornerShape(
                        topStart = 6.dp, topEnd = 18.dp,
                        bottomEnd = 18.dp, bottomStart = 18.dp,
                    )
                )
                .background(SurfaceWhite)
                .padding(horizontal = 14.dp, vertical = 11.dp),
        )
    }
}

@Composable
private fun AiInputBar(
    value: String,
    onValueChange: (String) -> Unit,
    onSendClick: () -> Unit,
    canSend: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(SurfaceWhite)
            .padding(start = 16.dp, top = 8.dp, end = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (value.isEmpty()) {
                Text(
                    text = "찾고 싶은 추억을 말해보세요",
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
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(BrandGreen)
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

private val DateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")

private fun formatDate(millis: Long): String =
    Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).format(DateFormatter)

@Preview(showBackground = true, widthDp = 393, heightDp = 500)
@Composable
private fun AiChatSheetContentPreview() {
    GttgttTheme {
        Box(modifier = Modifier.background(ScreenBackground)) {
            AiChatSheetContent(
                uiState = AiChatUiState(
                    messages = listOf(
                        AiMessage("1", AiMessage.Role.ASSISTANT, "어떤 추억을 찾고 싶으신가요?"),
                        AiMessage("2", AiMessage.Role.USER, "작년 겨울에 바다 갔던 거"),
                        AiMessage(
                            "3", AiMessage.Role.ASSISTANT, "이런 기억을 찾았어요.",
                            results = listOf(
                                MemoryHit("m1", "바다 진짜 예뻤던 강릉 첫날", 1_766_275_200_000L, GradientTheme.SEA.asPhoto()),
                                MemoryHit("m2", "시험 끝나고 치킨 먹다 울었던 날", 1_766_361_600_000L, GradientTheme.FOOD.asPhoto()),
                            ),
                        ),
                    ),
                ),
                onInputChange = {},
                onSendClick = {},
                onMemoryClick = {},
            )
        }
    }
}
