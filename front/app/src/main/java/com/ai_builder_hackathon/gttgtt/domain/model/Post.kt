package com.ai_builder_hackathon.gttgtt.domain.model

/**
 * 그룹 피드의 게시물 하나.
 *
 * 사진은 아직 Storage 가 없어 [GradientTheme] 플레이스홀더로 둔다.
 * 실제 미디어가 붙으면 [photos] 타입만 URL 목록으로 바꾸면 된다.
 */
data class Post(
    val id: String,
    val archiveId: String,
    val authorId: String,
    val authorName: String,
    /** 추억이 일어난 날. 업로드 시각(created_at)과 혼용하지 않는다 (CLAUDE.md §6.2). */
    val memoryDateMillis: Long,
    val photos: List<GradientTheme>,
    val caption: String,
    val likeCount: Int,
    val commentCount: Int,
    /** 내가 좋아요를 눌렀는지 */
    val likedByMe: Boolean = false,
) {
    /** 사진이 1장이면 큰 히어로, 2장 이상이면 3열 그리드로 그린다. */
    val hasSinglePhoto: Boolean get() = photos.size == 1
}
