package com.ai_builder_hackathon.gttgtt.domain.model

/**
 * 그룹 목록(S1)의 한 줄에 필요한 최소 정보.
 *
 * 도메인 모델이라 색상 값을 들고 있지 않다. 대신 [GroupTheme] 같은 의미 단위만 두고,
 * 실제 Color 매핑은 ui/theme 에서 한다.
 */
data class ArchiveSummary(
    val id: String,
    val name: String,
    /** "민지: 바다 너무 예뻤어" 처럼 이미 작성자명이 붙은 미리보기 문구 */
    val lastMessagePreview: String,
    /** 마지막 활동 시각(epoch millis). 화면 표기 형식은 UI가 결정한다. */
    val lastActivityAtMillis: Long,
    val theme: GroupTheme,
    /** 아바타로 표시할 멤버 id. 색은 id 해시로 정해진다. */
    val memberIds: List<String>,
    val totalMemberCount: Int,
) {
    /** 아바타를 [visibleCount]개만 보여줄 때 뒤에 붙는 "+N" 의 N */
    fun hiddenMemberCount(visibleCount: Int): Int =
        (totalMemberCount - visibleCount).coerceAtLeast(0)
}

/**
 * 썸네일 그라디언트 종류. 서버에는 이 이름 문자열로 저장한다.
 * 이름은 시안(design/redesign-mockup.html)의 클래스명과 일치시킨다.
 */
enum class GroupTheme {
    BEACH,
    FOREST,
    FOOD,
    LAPTOP,
    FAMILY,
    SEA,
    NIGHT,
}
