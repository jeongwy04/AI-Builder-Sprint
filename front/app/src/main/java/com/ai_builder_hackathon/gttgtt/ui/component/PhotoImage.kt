package com.ai_builder_hackathon.gttgtt.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import coil3.compose.AsyncImage
import com.ai_builder_hackathon.gttgtt.domain.model.Photo

/**
 * [Photo] 를 그린다. URL 이 있으면 Coil 로 로드하고,
 * 없거나 로딩 중이면 뒤에 깔린 그라디언트가 보인다 (placeholder 겸 fallback).
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
            .background(gradientOf(photo.fallback))
    ) {
        if (photo.url != null) {
            AsyncImage(
                model = photo.url,
                contentDescription = contentDescription,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
