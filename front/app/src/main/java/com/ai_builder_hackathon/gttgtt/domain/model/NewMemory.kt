package com.ai_builder_hackathon.gttgtt.domain.model

/**
 * 작성 화면(S4)에서 올리는 새 기억.
 *
 * 아직 서버에 없는 상태라 id 가 없고, 사진도 URL 이 아니라
 * 기기 안의 content:// URI 목록이다. 업로드는 Repository 가 한다.
 */
data class NewMemory(
    val archiveId: String,
    val title: String,
    val body: String,
    /** 추억이 일어난 날. EXIF 촬영일이 있으면 그 값이 기본으로 들어온다 (CLAUDE.md §6.2). */
    val memoryDateMillis: Long,
    /** 기기 갤러리의 content:// URI 목록 */
    val photoUris: List<String>,
    /** 함께한 사람으로 태깅한 멤버 id */
    val participantIds: List<String>,
)
