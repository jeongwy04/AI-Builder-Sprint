package com.ai_builder_hackathon.gttgtt.domain.model

/**
 * 화면에 표시할 사진 한 장.
 *
 * Supabase 가 붙으면 [url] 에 signed URL 이 들어오고,
 * 아직 없거나(Fake) 로딩 전이면 [fallback] 그라디언트를 그린다.
 * 이 구조 덕에 Fake ↔ 실데이터 전환 시 화면 코드를 다시 고칠 필요가 없다.
 *
 * [id]/[storagePath] 는 이미 서버에 저장된 사진(=media_assets 행)에만 채워진다.
 * 기억 수정 화면에서 "이 사진을 지운다"고 할 때 어떤 media_assets 행인지 식별하는 데 쓴다 —
 * 아직 업로드 전인 새 사진(로컬 content:// URI)에는 해당하지 않는다.
 */
data class Photo(
    val id: String? = null,
    val storagePath: String? = null,
    val url: String? = null,
    val fallback: GradientTheme = GradientTheme.BEACH,
)

/** Fake 데이터 작성용 축약 */
fun GradientTheme.asPhoto(id: String? = null): Photo = Photo(id = id, fallback = this)
