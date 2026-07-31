package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ai_builder_hackathon.gttgtt.R
import com.ai_builder_hackathon.gttgtt.domain.model.Photo
import com.ai_builder_hackathon.gttgtt.ui.theme.SurfaceWhite

/**
 * 사진을 원본 비율 그대로(잘리지 않게 Fit) 전체화면에서 보는 뷰어.
 *
 * 목록/상세의 썸네일은 [PhotoImage] 로 Crop 되어 일부만 보이는데, 여기서는 검열 없이
 * 사진 전체가 보이도록 [ContentScale.Fit] 을 쓴다. 좌우로 스와이프하면 [photos] 의
 * 나머지 사진도 이어서 볼 수 있다 — 호출부의 페이저와 [initialIndex] 를 공유해서
 * 탭한 사진부터 바로 열린다.
 */
@Composable
fun PhotoViewerDialog(
    photos: List<Photo>,
    initialIndex: Int,
    onDismiss: () -> Unit,
) {
    if (photos.isEmpty()) return

    Dialog(
        onDismissRequest = onDismiss,
        // 기본 다이얼로그 폭 제한을 풀어야 화면 전체를 채울 수 있다.
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val pagerState = rememberPagerState(
            initialPage = initialIndex.coerceIn(0, photos.lastIndex),
            pageCount = { photos.size },
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                val photo = photos[page]
                Box(
                    // 사진이든 여백이든 탭하면 닫힌다 — 별도 닫기 버튼과 함께 두 가지
                    // 방법을 다 제공한다.
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center,
                ) {
                    val url = photo.url
                    if (url != null) {
                        val platformContext = LocalPlatformContext.current
                        // PhotoImage 와 같은 이유 — storagePath 를 캐시 키로 고정해서 썸네일 볼 때
                        // 이미 받아둔 파일이 있으면(같은 사진이니까) 여기서 또 새로 받지 않는다.
                        val request = remember(url, photo.storagePath) {
                            ImageRequest.Builder(platformContext)
                                .data(url)
                                .crossfade(true)
                                .apply {
                                    photo.storagePath?.let { path ->
                                        memoryCacheKey(path)
                                        diskCacheKey(path)
                                    }
                                }
                                .build()
                        }
                        AsyncImage(
                            model = request,
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }

            Icon(
                painter = painterResource(R.drawable.ic_x),
                contentDescription = "닫기",
                tint = SurfaceWhite,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
                    .clickable(onClick = onDismiss)
                    .padding(6.dp),
            )

            if (photos.size > 1) {
                Text(
                    text = "${pagerState.currentPage + 1} / ${photos.size}",
                    color = SurfaceWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
    }
}
