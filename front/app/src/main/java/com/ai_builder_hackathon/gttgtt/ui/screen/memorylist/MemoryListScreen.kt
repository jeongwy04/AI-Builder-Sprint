package com.ai_builder_hackathon.gttgtt.ui.screen.memorylist

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
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.domain.model.MemorySummary
import com.ai_builder_hackathon.gttgtt.ui.theme.BrandGreen
import com.ai_builder_hackathon.gttgtt.ui.theme.GttgttTheme
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackground
import com.ai_builder_hackathon.gttgtt.ui.theme.ScreenBackgroundBrush
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite
import com.ai_builder_hackathon.gttgtt.ui.theme.TextPrimary
import com.ai_builder_hackathon.gttgtt.ui.theme.TextSecondary
import com.ai_builder_hackathon.gttgtt.ui.theme.TopBarIcon
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SidePadding = 20.dp

@Composable
fun MemoryListScreen(
    kind: MemoryListKind,
    onBackClick: () -> Unit,
    onMemoryClick: (memoryId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MemoryListViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    LaunchedEffect(kind) { viewModel.load(kind) }

    MemoryListContent(
        title = when (kind) {
            MemoryListKind.MINE -> "내가 남긴 추억"
            MemoryListKind.LIKED -> "좋아요한 추억"
        },
        emptyText = when (kind) {
            MemoryListKind.MINE -> "아직 남긴 추억이 없어요."
            MemoryListKind.LIKED -> "아직 좋아요한 추억이 없어요."
        },
        uiState = uiState,
        onBackClick = onBackClick,
        onMemoryClick = onMemoryClick,
        modifier = modifier,
    )
}

@Composable
private fun MemoryListContent(
    title: String,
    emptyText: String,
    uiState: MemoryListUiState,
    onBackClick: () -> Unit,
    onMemoryClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackgroundBrush),
    ) {
        TopBar(title = title, onBackClick = onBackClick)

        when {
            uiState.isLoading -> CenterBox { CircularProgressIndicator(color = BrandGreen) }
            uiState.errorMessage != null -> CenterBox { MessageText(uiState.errorMessage) }
            uiState.isEmpty -> CenterBox { MessageText(emptyText) }
            else -> LazyColumn(
                contentPadding = PaddingValues(
                    start = SidePadding,
                    end = SidePadding,
                    top = 8.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(11.dp),
            ) {
                items(uiState.memories, key = { it.id }) { memory ->
                    MemoryRow(memory = memory, onClick = { onMemoryClick(memory.id) })
                }
            }
        }
    }
}

@Composable
private fun TopBar(title: String, onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = SidePadding, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_chevron_left),
            contentDescription = "뒤로",
            tint = TopBarIcon,
            modifier = Modifier
                .size(24.dp)
                .clickable(onClick = onBackClick),
        )
        Text(
            text = title,
            color = TextPrimary,
            fontSize = 19.sp,
            fontWeight = FontWeight.Black,
        )
    }
}

@Composable
private fun MemoryRow(memory: MemorySummary, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceWhite)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = formatDate(memory.memoryDateMillis),
                color = BrandGreen,
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
            )
            if (!memory.placeName.isNullOrBlank()) {
                Text(
                    text = "· ${memory.placeName}",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Spacer(Modifier.height(5.dp))
        Text(
            text = memory.preview,
            color = TextPrimary,
            fontSize = 14.5.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun CenterBox(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = 60.dp),
        contentAlignment = Alignment.TopCenter,
    ) { content() }
}

@Composable
private fun MessageText(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
    )
}

private val DateFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN)

private fun formatDate(epochMillis: Long): String =
    Instant.ofEpochMilli(epochMillis)
        .atZone(ZoneId.systemDefault())
        .format(DateFormatter)

@Preview(showBackground = true, widthDp = 393, heightDp = 852)
@Composable
private fun MemoryListContentPreview() {
    GttgttTheme {
        MemoryListContent(
            title = "내가 남긴 추억",
            emptyText = "아직 남긴 추억이 없어요.",
            uiState = MemoryListUiState(
                isLoading = false,
                memories = listOf(
                    MemorySummary("1", "a", 0L, "강릉 경포해변", "경포 바다 진짜 미쳤다. 회 먹고 밤에 폭죽 터뜨렸음 ㅋㅋ"),
                    MemorySummary("2", "a", 0L, "남산서울타워", "올해 첫눈 오던 날! 호빵 사 먹고 자물쇠도 걸었다."),
                ),
            ),
            onBackClick = {},
            onMemoryClick = {},
        )
    }
}
