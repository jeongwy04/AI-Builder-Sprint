package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.ai_builder_hackathon.gttgtt.domain.model.Photo
import com.ai_builder_hackathon.gttgtt.ui.theme.PhotoPlaceholderBackground

/**
 * [Photo] 를 그린다. URL 이 있으면 Coil 로 로드하고,
 * 없거나 로딩 중이면 뒤에 깔린 중립 회색이 보인다 (placeholder 겸 fallback).
 * 예전엔 테마별 그라디언트([Photo.fallback])를 썼는데, 색이 튀어서 단색 회색으로 통일했다.
 */
@Composable
fun PhotoImage(
    photo: Photo,
    corner: Dp,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(corner))
            .background(PhotoPlaceholderBackground)
    ) {
        val url = photo.url
        if (url != null) {
            val platformContext = LocalPlatformContext.current
            // signed URL 은 발급될 때마다 토큰이 바뀐다 — URL 문자열 자체를 캐시 키로 쓰면
            // MediaUploader 의 2시간 캐시가 만료되거나 앱을 재시작할 때마다 URL이 달라져서
            // Coil 디스크 캐시가 매번 미스 나고 사진을 처음부터 다시 받는다. storagePath 는
            // 사진이 존재하는 한 절대 안 바뀌니 그걸 캐시 키로 고정해서, URL이 새로 발급돼도
            // 이미 받아둔 파일을 그대로 재사용하게 한다.
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
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
