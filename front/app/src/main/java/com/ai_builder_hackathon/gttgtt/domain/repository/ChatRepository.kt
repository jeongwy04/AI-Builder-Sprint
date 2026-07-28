package com.ai_builder_hackathon.gttgtt.domain.repository

import com.ai_builder_hackathon.gttgtt.domain.model.ChatMessage

interface ChatRepository {
    /** 오래된 것부터 시간순 */
    suspend fun getMessages(archiveId: String): Result<List<ChatMessage>>

    /** 보낸 메시지를 돌려준다(서버가 부여한 id·시각 포함). */
    suspend fun sendMessage(archiveId: String, text: String): Result<ChatMessage>
}
