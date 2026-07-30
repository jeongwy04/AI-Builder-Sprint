package com.ai_builder_hackathon.gttgtt.domain.model

/**
 * 마이페이지에 필요한 내 정보.
 *
 * [avatarUrl] 이 null 이면 [id] 해시로 아바타 색을 정한다 (다른 화면과 같은 규칙,
 * [com.ai_builder_hackathon.gttgtt.ui.component.MemberAvatar] 참고).
 */
data class UserProfile(
    val id: String,
    val name: String,
    /** "그때그때 120일째" 의 120 */
    val streakDays: Int,
    /** "추억을 모으는 중 ✨" 같은 한 줄 상태 */
    val statusMessage: String,
    val memoryCount: Int,
    val mediaCount: Int,
    /** 프로필 사진 signed URL. 아직 안 올렸으면 null — 그라디언트 기본 아바타로 대체된다. */
    val avatarUrl: String? = null,
)
