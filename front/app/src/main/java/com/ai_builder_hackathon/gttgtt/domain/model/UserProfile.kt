package com.ai_builder_hackathon.gttgtt.domain.model

/**
 * 마이페이지에 필요한 내 정보.
 *
 * [avatarUrl] 이 null 이면(사진 미등록) [id] 해시로 기본 아바타 색을 정한다.
 */
data class UserProfile(
    val id: String,
    val name: String,
    /** "추억을 모으는 중 ✨" 같은 한 줄 상태 */
    val statusMessage: String,
    val memoryCount: Int,
    val mediaCount: Int,
    /** 내가 좋아요한 기억 수 (마이페이지 카드에 표시) */
    val likedCount: Int,
    /** 프로필 사진 signed URL. 미등록이면 null → 기본(그라디언트) 아바타. */
    val avatarUrl: String? = null,
    /** 프로필 사진 Storage 경로 (profiles.avatar_url). */
    val avatarPath: String? = null,
)
