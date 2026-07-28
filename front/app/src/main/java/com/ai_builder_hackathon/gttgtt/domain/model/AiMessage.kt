package com.ai_builder_hackathon.gttgtt.domain.model

/**
 * AI 추억 찾기 대화의 메시지 하나.
 *
 * 그룹 채팅([ChatMessage])과 별개다. 이쪽은 사람이 아니라 AI 와의 대화이고,
 * 응답에 검색 결과가 딸려 올 수 있다.
 */
data class AiMessage(
    val id: String,
    val role: Role,
    val text: String,
    /**
     * AI 가 search_memories 도구로 찾아낸 기억들.
     * 비어 있으면 되묻는 중이거나 일반 응답이다.
     *
     * LLM 은 이 목록 안에서만 답해야 한다 (CLAUDE.md §6.3).
     */
    val results: List<MemoryHit> = emptyList(),
) {
    enum class Role { USER, ASSISTANT }

    val isUser: Boolean get() = role == Role.USER
}

/** 검색 결과 카드 하나에 필요한 최소 정보. */
data class MemoryHit(
    val memoryId: String,
    val title: String,
    /** "2025.12.22" 로 보여줄 추억 날짜 */
    val memoryDateMillis: Long,
    val theme: GradientTheme,
)
