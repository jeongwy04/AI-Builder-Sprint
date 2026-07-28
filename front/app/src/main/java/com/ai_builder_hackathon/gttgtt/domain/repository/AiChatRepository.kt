package com.ai_builder_hackathon.gttgtt.domain.repository

import com.ai_builder_hackathon.gttgtt.domain.model.AiMessage

interface AiChatRepository {
    /** 그룹 진입 시 AI 가 먼저 건네는 인사 (CLAUDE.md §6.4) */
    fun greeting(): AiMessage

    /**
     * 사용자 메시지를 보내고 AI 응답을 받는다.
     *
     * 실제 구현은 `chat` Edge Function 을 호출한다. 앱에서 Upstage 를 직접 부르지 않는다 (§5.4).
     */
    suspend fun send(archiveId: String, text: String): Result<AiMessage>
}
