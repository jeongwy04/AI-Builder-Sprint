package com.ai_builder_hackathon.gttgtt.domain.model

/**
 * 추억 상세 화면에 필요한 전체 정보.
 *
 * 사진은 [Photo] — signed URL 이 없으면 그라디언트 fallback 으로 그려진다.
 */
data class MemoryDetail(
    val id: String,
    val archiveId: String,
    /** 추억이 실제 일어난 날. 업로드 시각과 혼용하지 않는다 (CLAUDE.md §6.2). */
    val memoryDateMillis: Long,
    val title: String,
    val body: String,
    /** 히어로 영역에 넘겨볼 사진들. 첫 장이 대표. */
    val photos: List<Photo>,
    val participants: List<Participant>,
    /** 같은 그룹의 다른 기억에서 가져온 연관 사진 */
    val relatedPhotos: List<Photo>,
    val comments: List<Comment>,
) {
    val photoCount: Int get() = photos.size
}

data class Participant(
    val id: String,
    /** 나 자신이면 "나" 로 들어온다. */
    val name: String,
)

data class Comment(
    val id: String,
    val authorId: String,
    val authorName: String,
    val text: String,
    val createdAtMillis: Long,
    val likeCount: Int = 0,
)
