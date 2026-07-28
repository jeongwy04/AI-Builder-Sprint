package com.ai_builder_hackathon.gttgtt.domain.model

/**
 * 화면에 표시할 사진 한 장.
 *
 * Supabase 가 붙으면 [url] 에 signed URL 이 들어오고,
 * 아직 없거나(Fake) 로딩 전이면 [fallback] 그라디언트를 그린다.
 * 이 구조 덕에 Fake ↔ 실데이터 전환 시 화면 코드를 다시 고칠 필요가 없다.
 */
data class Photo(
    val url: String? = null,
    val fallback: GradientTheme = GradientTheme.BEACH,
)

/** Fake 데이터 작성용 축약 */
fun GradientTheme.asPhoto(): Photo = Photo(fallback = this)
